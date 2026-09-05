<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** Mirrors backend/finance/models.py's Invoice — see docs/API_CONTRACT.md's
 * "invoice" field payload. number/subtotal/vat_amount/total/amount_paid are
 * all server-derived and deliberately absent from from_wire()'s output —
 * see Sync::push_post(), recompute_totals() and recompute_payment_state()
 * below (ports backend/finance/services.py's recompute_invoice_totals()/
 * recompute_invoice_payment_state() exactly). */
class Invoice_model extends Business_owned_model {

	protected $table = 'invoices';

	const STATUS_PAID = 'paid';
	const STATUS_PARTIALLY_PAID = 'partially_paid';
	const STATUS_SENT = 'sent';
	const STATUS_CANCELLED = 'cancelled';

	public function from_wire(array $fields)
	{
		return array(
			'customer_id' => $fields['customer_id'],
			'job_id' => $fields['job_id'] ?? NULL,
			'quote_id' => $fields['quote_id'] ?? NULL,
			'status' => $fields['status'] ?? 'draft',
			'issue_date' => $fields['issue_date'],
			'due_date' => $fields['due_date'] ?? NULL,
			'notes' => $fields['notes'] ?? '',
			'terms' => $fields['terms'] ?? '',
			'is_vat_applicable' => !empty($fields['is_vat_applicable']) ? 1 : 0,
			'discount_amount' => $fields['discount_amount'] ?? '0.00',
			'sent_at' => $fields['sent_at'] ?? NULL,
		);
	}

	public function to_wire(array $row)
	{
		return array(
			'customer_id' => $row['customer_id'],
			'job_id' => $row['job_id'],
			'quote_id' => $row['quote_id'],
			'number' => $row['number'],
			'status' => $row['status'],
			'issue_date' => $row['issue_date'],
			'due_date' => $row['due_date'],
			'notes' => $row['notes'],
			'terms' => $row['terms'],
			'is_vat_applicable' => (bool) $row['is_vat_applicable'],
			'discount_amount' => $row['discount_amount'],
			'subtotal' => $row['subtotal'],
			'vat_amount' => $row['vat_amount'],
			'total' => $row['total'],
			'amount_paid' => $row['amount_paid'],
			'sent_at' => $row['sent_at'] ? iso8601($row['sent_at']) : NULL,
		);
	}

	public function to_sync_change(array $row)
	{
		return array(
			'model' => 'invoice',
			'id' => $row['id'],
			'updated_at' => iso8601($row['updated_at']),
			'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			'fields' => $this->to_wire($row),
		);
	}

	/** Ports recompute_invoice_totals(): recompute subtotal/vat_amount/total
	 * from this invoice's own current non-deleted line items, then always
	 * cascades into recompute_payment_state() without bumping updated_at a
	 * second time (matching bump_updated_at=False on that inner call). */
	public function recompute_totals($id, $business_id)
	{
		$invoice = $this->find_any($id, $business_id);
		if ($invoice === NULL)
		{
			return;
		}
		$line_totals = $this->db->select('line_total')
			->where('invoice_id', $id)
			->where('deleted_at', NULL)
			->get('invoice_line_items')
			->result_array();
		$totals = money_compute_document_totals(
			array_column($line_totals, 'line_total'),
			$invoice['discount_amount'],
			(bool) $invoice['is_vat_applicable']
		);
		$this->db->where('id', $id)->where('business_id', $business_id)->update('invoices', array(
			'subtotal' => $totals[0],
			'vat_amount' => $totals[1],
			'total' => $totals[2],
			'updated_at' => mysql_now(),
		));
		$this->recompute_payment_state($id, $business_id, FALSE);
	}

	/**
	 * Ports recompute_invoice_payment_state() exactly: amount_paid is
	 * always derived from actual non-deleted payment records, never entered
	 * by hand. PAID/PARTIALLY_PAID are fully derived from amount_paid vs
	 * total in both directions — including a payment being deleted/
	 * corrected, which must pull the invoice back out of "Paid". Other
	 * workflow statuses (draft/cancelled) are never touched by this.
	 */
	public function recompute_payment_state($id, $business_id, $bump_updated_at = TRUE)
	{
		$invoice = $this->find_any($id, $business_id);
		if ($invoice === NULL)
		{
			return;
		}
		$sum_row = $this->db->select('COALESCE(SUM(amount), 0) AS total')
			->where('invoice_id', $id)
			->where('deleted_at', NULL)
			->get('payments')
			->row_array();
		$amount_paid = money_quantize($sum_row['total']);

		$update = array('amount_paid' => $amount_paid);

		if ($invoice['status'] !== self::STATUS_CANCELLED)
		{
			$total = (float) $invoice['total'];
			$paid = (float) $amount_paid;
			if ($paid > 0 && $total > 0 && $paid >= $total)
			{
				$new_status = self::STATUS_PAID;
			}
			elseif ($paid > 0)
			{
				$new_status = self::STATUS_PARTIALLY_PAID;
			}
			elseif (in_array($invoice['status'], array(self::STATUS_PAID, self::STATUS_PARTIALLY_PAID), TRUE))
			{
				$new_status = self::STATUS_SENT;
			}
			else
			{
				$new_status = $invoice['status'];
			}
			if ($new_status !== $invoice['status'])
			{
				$update['status'] = $new_status;
			}
		}

		if ($bump_updated_at)
		{
			$update['updated_at'] = mysql_now();
		}

		$this->db->where('id', $id)->where('business_id', $business_id)->update('invoices', $update);
	}
}
