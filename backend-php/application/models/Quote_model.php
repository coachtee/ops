<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** Mirrors backend/sales/models.py's Quote — see docs/API_CONTRACT.md's
 * "quote" field payload. number/subtotal/vat_amount/total are all
 * server-derived and deliberately absent from from_wire()'s output, so a
 * client-sent value for any of them is silently ignored on write (matching
 * the DRF serializer's read_only=True fields) — see
 * Sync::push_post()/recompute_totals() for where they're actually set. */
class Quote_model extends Business_owned_model {

	protected $table = 'quotes';

	public function from_wire(array $fields)
	{
		return array(
			'customer_id' => $fields['customer_id'],
			'lead_id' => $fields['lead_id'] ?? NULL,
			'status' => $fields['status'] ?? 'draft',
			'issue_date' => $fields['issue_date'],
			'valid_until' => $fields['valid_until'] ?? NULL,
			'notes' => $fields['notes'] ?? '',
			'terms' => $fields['terms'] ?? '',
			'is_vat_applicable' => !empty($fields['is_vat_applicable']) ? 1 : 0,
			'discount_amount' => $fields['discount_amount'] ?? '0.00',
			'sent_at' => $fields['sent_at'] ?? NULL,
			'accepted_at' => $fields['accepted_at'] ?? NULL,
			'declined_at' => $fields['declined_at'] ?? NULL,
		);
	}

	public function to_wire(array $row)
	{
		return array(
			'customer_id' => $row['customer_id'],
			'lead_id' => $row['lead_id'],
			'number' => $row['number'],
			'status' => $row['status'],
			'issue_date' => $row['issue_date'],
			'valid_until' => $row['valid_until'],
			'notes' => $row['notes'],
			'terms' => $row['terms'],
			'is_vat_applicable' => (bool) $row['is_vat_applicable'],
			'discount_amount' => $row['discount_amount'],
			'subtotal' => $row['subtotal'],
			'vat_amount' => $row['vat_amount'],
			'total' => $row['total'],
			'sent_at' => $row['sent_at'] ? iso8601($row['sent_at']) : NULL,
			'accepted_at' => $row['accepted_at'] ? iso8601($row['accepted_at']) : NULL,
			'declined_at' => $row['declined_at'] ? iso8601($row['declined_at']) : NULL,
		);
	}

	public function to_sync_change(array $row)
	{
		return array(
			'model' => 'quote',
			'id' => $row['id'],
			'updated_at' => iso8601($row['updated_at']),
			'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			'fields' => $this->to_wire($row),
		);
	}

	/**
	 * Recomputes subtotal/vat_amount/total from this quote's own current
	 * non-deleted line items — mirrors backend/sales/services.py's
	 * recompute_quote_totals() exactly, always bumping updated_at (called
	 * only from Sync::push_post()'s post-batch recompute, never inline
	 * during a single record's own apply).
	 */
	public function recompute_totals($id, $business_id)
	{
		$quote = $this->find_any($id, $business_id);
		if ($quote === NULL)
		{
			return;
		}
		$line_totals = $this->db->select('line_total')
			->where('quote_id', $id)
			->where('deleted_at', NULL)
			->get('quote_line_items')
			->result_array();
		$totals = money_compute_document_totals(
			array_column($line_totals, 'line_total'),
			$quote['discount_amount'],
			(bool) $quote['is_vat_applicable']
		);
		$this->db->where('id', $id)->where('business_id', $business_id)->update('quotes', array(
			'subtotal' => $totals[0],
			'vat_amount' => $totals[1],
			'total' => $totals[2],
			'updated_at' => mysql_now(),
		));
	}
}
