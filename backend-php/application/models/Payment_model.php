<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** Mirrors backend/finance/models.py's Payment — see docs/API_CONTRACT.md's
 * "payment" field payload. Recording/deleting a payment against an invoice
 * requires Sync's post-batch recompute of that invoice's amount_paid/status
 * (see Sync::push_post() and Invoice_model::recompute_payment_state()). */
class Payment_model extends Business_owned_model {

	protected $table = 'payments';

	public function from_wire(array $fields)
	{
		return array(
			'customer_id' => $fields['customer_id'],
			'invoice_id' => $fields['invoice_id'] ?? NULL,
			'amount' => $fields['amount'],
			'method' => $fields['method'] ?? 'eft',
			'reference' => $fields['reference'] ?? '',
			'paid_date' => $fields['paid_date'],
			'notes' => $fields['notes'] ?? '',
		);
	}

	public function to_wire(array $row)
	{
		return array(
			'customer_id' => $row['customer_id'],
			'invoice_id' => $row['invoice_id'],
			'amount' => $row['amount'],
			'method' => $row['method'],
			'reference' => $row['reference'],
			'paid_date' => $row['paid_date'],
			'notes' => $row['notes'],
		);
	}

	public function to_sync_change(array $row)
	{
		return array(
			'model' => 'payment',
			'id' => $row['id'],
			'updated_at' => iso8601($row['updated_at']),
			'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			'fields' => $this->to_wire($row),
		);
	}
}
