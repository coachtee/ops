<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * POST /api/sync/push/, GET /api/sync/pull/ — see docs/API_CONTRACT.md's
 * "Sync" section (unchanged by this rewrite): this is the Android app's
 * real read/write path, not the per-resource CRUD controllers. Every model
 * in config('sync_registry') is a Business_owned_model subclass exposing
 * from_wire()/to_wire()/to_sync_change() — see Customer_model for the
 * pattern every resource follows.
 *
 * push_post() ports backend/sync/services.py's apply_change()/apply_push()/
 * _recompute_touched_parents() exactly: a batch is applied in a fixed
 * dependency order, per-record computed fields (line_total, expense
 * vat_amount, payslip net_pay) are set inline, quote/job/invoice numbers
 * are assigned on first successful sync, and — once every change in the
 * batch has been applied — any quote/invoice whose line items (or, for
 * invoices, payments) were touched gets its totals/payment-state
 * recomputed from the database's current state, exactly once per push,
 * regardless of ordering within the batch.
 */
class Sync extends Api_Controller {

	private $registry;

	/** Mirrors MODEL_APPLY_ORDER in backend/sync/services.py — a batch can
	 * contain a whole offline session's worth of records in any order, so
	 * the server applies parents before the children that reference them.
	 * compliance_item has no relations to any other model, so its position
	 * is arbitrary; it's simply applied last. */
	const MODEL_APPLY_ORDER = array(
		'lead' => 0,
		'customer' => 1,
		'quote' => 2,
		'quote_line_item' => 3,
		'job' => 4,
		'invoice' => 5,
		'invoice_line_item' => 6,
		'payment' => 7,
		'supplier' => 8,
		'expense' => 9,
		'employee' => 10,
		'payslip' => 11,
		'compliance_item' => 12,
		'visit' => 13,
	);

	const LINE_ITEM_PARENT_KEY = array(
		'quote_line_item' => 'quote_id',
		'invoice_line_item' => 'invoice_id',
	);

	const NUMBERED_MODELS = array(
		'quote' => 'Q',
		'job' => 'J',
		'invoice' => 'INV',
	);

	public function __construct()
	{
		parent::__construct();
		$this->config->load('sync_registry');
		$this->registry = $this->config->item('sync_registry');
		foreach ($this->registry as $model_class)
		{
			$this->load->model($model_class);
		}
		$this->load->model('Document_sequence_model');
	}

	private function model_for($key)
	{
		$class = $this->registry[$key] ?? NULL;
		if ($class === NULL)
		{
			return NULL;
		}
		return $this->{$class};
	}

	/** Runs a resource-specific business-rule validator if the model
	 * defines one (Payslip_model, Expense_model) — mirrors the Django
	 * serializer's is_valid() failure branch, returning status "error"
	 * without writing anything for that one record. */
	private function validate_fields($model, array $fields)
	{
		if (method_exists($model, 'validate'))
		{
			return $model->validate($fields);
		}
		return array();
	}

	/** Computes the same-record derived fields Django's apply_change()
	 * computes right after save (line_total, expense vat_amount, payslip
	 * net_pay) — done here, before insert/update, since (unlike Django) we
	 * build the row once rather than save-then-patch. */
	private function apply_inline_computed_fields($model_key, array $fields)
	{
		if (array_key_exists($model_key, self::LINE_ITEM_PARENT_KEY))
		{
			$fields['line_total'] = money_compute_line_total($fields['quantity'], $fields['unit_price']);
		}
		elseif ($model_key === 'expense')
		{
			$fields['vat_amount'] = money_extract_vat_from_inclusive($fields['amount'], $fields['is_vat_applicable']);
		}
		elseif ($model_key === 'payslip')
		{
			$fields['net_pay'] = money_compute_net_pay($fields['gross_pay'], $fields['deductions']);
		}
		return $fields;
	}

	public function push_post()
	{
		$body = json_decode($this->input->raw_input_stream, TRUE) ?: array();
		$changes = $body['changes'] ?? array();

		// Apply within the batch in a fixed dependency order — see
		// MODEL_APPLY_ORDER's doc comment above — while keeping each
		// result at its original index so the response mirrors the
		// client's own request order.
		$order_keyed = array();
		foreach ($changes as $i => $change)
		{
			$order_keyed[] = array(self::MODEL_APPLY_ORDER[$change['model'] ?? ''] ?? 99, $i, $change);
		}
		usort($order_keyed, function ($a, $b) {
			return $a[0] <=> $b[0] ?: $a[1] <=> $b[1];
		});

		$results = array_fill(0, count($changes), NULL);
		// Collected while applying, used by the post-batch recompute pass.
		$touched_quote_ids = array();
		$touched_invoice_ids_for_totals = array();
		$touched_invoice_ids_for_payment_state = array();

		$this->db->trans_begin();

		foreach ($order_keyed as list($order, $original_index, $change))
		{
			$model_key = $change['model'] ?? NULL;
			$id = $change['id'] ?? NULL;
			$model = $this->model_for($model_key);

			if ($model === NULL)
			{
				$results[$original_index] = array(
					'model' => $model_key,
					'id' => $id,
					'status' => 'error',
					'errors' => array('model' => array('Unknown model.')),
				);
				continue;
			}

			if (empty($change['updated_at']))
			{
				$results[$original_index] = array(
					'model' => $model_key,
					'id' => $id,
					'status' => 'error',
					'errors' => array('updated_at' => array('This field is required.')),
				);
				continue;
			}

			if (empty($id))
			{
				$results[$original_index] = array(
					'model' => $model_key,
					'id' => $id,
					'status' => 'error',
					'errors' => array('id' => array('This field is required.')),
				);
				continue;
			}

			if ($model->used_by_other_business($id, $this->business_id))
			{
				$results[$original_index] = array(
					'model' => $model_key,
					'id' => $id,
					'status' => 'error',
					'errors' => array('id' => array('This id is already in use by another business.')),
				);
				continue;
			}

			$incoming_updated_at = mysql_datetime_from_iso($change['updated_at']);
			$existing = $model->find_any($id, $this->business_id);

			// Last-write-wins, string comparison is safe here since both
			// sides are the same fixed-width, zero-padded 'Y-m-d H:i:s.u'
			// format — see docs/API_CONTRACT.md's "Sync" acceptance rule.
			if ($existing !== NULL && $existing['updated_at'] >= $incoming_updated_at)
			{
				$results[$original_index] = array(
					'model' => $model_key,
					'id' => $id,
					'status' => 'conflict',
					'server_record' => $model->to_sync_change($existing),
				);
				continue;
			}

			$fields = $model->from_wire($change['fields'] ?? array());

			$errors = $this->validate_fields($model, $fields);
			if (!empty($errors))
			{
				$results[$original_index] = array(
					'model' => $model_key,
					'id' => $id,
					'status' => 'error',
					'errors' => $errors,
				);
				continue;
			}

			$fields = $this->apply_inline_computed_fields($model_key, $fields);

			$incoming_deleted_at = NULL;
			if (!empty($change['deleted_at']))
			{
				$incoming_deleted_at = mysql_datetime_from_iso($change['deleted_at']);
				$fields['deleted_at'] = $incoming_deleted_at;
			}
			$fields['updated_at'] = $incoming_updated_at;

			if ($existing === NULL)
			{
				$model->insert_row($id, $this->business_id, $fields);
			}
			else
			{
				$this->db->where('id', $id)->where('business_id', $this->business_id)
					->update($model->table_name(), $fields);
			}

			if (array_key_exists($model_key, self::NUMBERED_MODELS))
			{
				$current = $model->find_any($id, $this->business_id);
				if ($current['deleted_at'] === NULL && empty($current['number']))
				{
					$number = $this->Document_sequence_model->next_number(
						$this->business_id, $model_key, self::NUMBERED_MODELS[$model_key]
					);
					$this->db->where('id', $id)->where('business_id', $this->business_id)
						->update($model->table_name(), array('number' => $number));
				}
			}

			if ($model_key === 'quote')
			{
				$touched_quote_ids[$id] = TRUE;
			}
			elseif ($model_key === 'quote_line_item')
			{
				$touched_quote_ids[$fields['quote_id']] = TRUE;
			}
			elseif ($model_key === 'invoice')
			{
				$touched_invoice_ids_for_totals[$id] = TRUE;
			}
			elseif ($model_key === 'invoice_line_item')
			{
				$touched_invoice_ids_for_totals[$fields['invoice_id']] = TRUE;
			}
			elseif ($model_key === 'payment' && !empty($fields['invoice_id']))
			{
				$touched_invoice_ids_for_payment_state[$fields['invoice_id']] = TRUE;
			}

			$results[$original_index] = array(
				'model' => $model_key,
				'id' => $id,
				'status' => 'accepted',
				'server_record' => $model->to_sync_change($model->find_any($id, $this->business_id)),
			);
		}

		// Post-batch recompute — see backend/sync/services.py's
		// _recompute_touched_parents(): quotes/invoices' totals are never
		// trusted from client input, always recomputed from current
		// non-deleted line items; invoice payment state is derived from
		// current non-deleted payments. Order matters: recompute_totals()
		// on an invoice already cascades into its own payment-state
		// recompute, so "remaining" below only covers invoices whose
		// totals weren't already touched this batch.
		$this->load->model('Quote_model');
		$this->load->model('Invoice_model');

		foreach (array_keys($touched_quote_ids) as $quote_id)
		{
			$this->Quote_model->recompute_totals($quote_id, $this->business_id);
		}
		foreach (array_keys($touched_invoice_ids_for_totals) as $invoice_id)
		{
			$this->Invoice_model->recompute_totals($invoice_id, $this->business_id);
		}
		$remaining_payment_state = array_diff_key($touched_invoice_ids_for_payment_state, $touched_invoice_ids_for_totals);
		foreach (array_keys($remaining_payment_state) as $invoice_id)
		{
			$this->Invoice_model->recompute_payment_state($invoice_id, $this->business_id, TRUE);
		}

		if ($this->db->trans_status() === FALSE)
		{
			$this->db->trans_rollback();
			$this->response(array('detail' => 'Push failed, no changes were applied.'), 500);
		}
		$this->db->trans_commit();

		// Re-serialize accepted records once more: totals recomputed above
		// may have changed the very record (e.g. a quote whose own
		// discount_amount was in this push) after its result was first built.
		foreach ($results as &$result)
		{
			if ($result['status'] !== 'accepted')
			{
				continue;
			}
			$model = $this->model_for($result['model']);
			$fresh = $model->find_any($result['id'], $this->business_id);
			if ($fresh !== NULL)
			{
				$result['server_record'] = $model->to_sync_change($fresh);
			}
		}
		unset($result);

		$this->response(array('results' => $results), 200);
	}

	public function pull_get()
	{
		// Captured before the query runs, per API_CONTRACT.md's "server_time
		// is captured before the query runs and must be used as the next
		// since — this avoids missing a row written during the request."
		$server_time = mysql_now();
		$since = $this->input->get('since');

		$changes = array();
		foreach ($this->registry as $key => $model_class)
		{
			$model = $this->{$model_class};
			$this->db->where('business_id', $this->business_id);
			if ($since)
			{
				$this->db->where('updated_at >', mysql_datetime_from_iso($since));
			}
			$rows = $this->db->get($model->table_name())->result_array();
			foreach ($rows as $row)
			{
				$changes[] = $model->to_sync_change($row);
			}
		}

		$this->response(array(
			'server_time' => iso8601($server_time),
			'changes' => $changes,
		), 200);
	}
}
