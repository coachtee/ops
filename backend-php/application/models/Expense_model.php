<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** Mirrors backend/finance/models.py's Expense — see docs/API_CONTRACT.md's
 * "expense" field payload. `amount` is VAT-INCLUSIVE; vat_amount is always
 * server-derived (see money_helper.php's money_extract_vat_from_inclusive
 * and Sync::push_post(), which computes it inline before insert/update).
 * receipt_image is read-only here — the wire field 'receipt_image' maps to
 * the DB column 'receipt_image_url' and is only ever set via
 * POST /api/expenses/{id}/receipt/ (see Expenses::receipt_post()). */
class Expense_model extends Business_owned_model {

	protected $table = 'expenses';

	public function from_wire(array $fields)
	{
		return array(
			'job_id' => $fields['job_id'] ?? NULL,
			'supplier_id' => $fields['supplier_id'] ?? NULL,
			'category' => $fields['category'] ?? 'other',
			'description' => $fields['description'] ?? '',
			'amount' => $fields['amount'],
			'is_vat_applicable' => !empty($fields['is_vat_applicable']) ? 1 : 0,
			'date' => $fields['date'],
		);
	}

	public function to_wire(array $row)
	{
		return array(
			'job_id' => $row['job_id'],
			'supplier_id' => $row['supplier_id'],
			'category' => $row['category'],
			'description' => $row['description'],
			'amount' => $row['amount'],
			'is_vat_applicable' => (bool) $row['is_vat_applicable'],
			'vat_amount' => $row['vat_amount'],
			'date' => $row['date'],
			'receipt_image' => $row['receipt_image_url'],
		);
	}

	public function to_sync_change(array $row)
	{
		return array(
			'model' => 'expense',
			'id' => $row['id'],
			'updated_at' => iso8601($row['updated_at']),
			'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			'fields' => $this->to_wire($row),
		);
	}

	/** amount must be > 0; date cannot be more than 1 day in the future —
	 * see docs/API_CONTRACT.md's expense validation rules. */
	public function validate(array $fields)
	{
		$errors = array();
		if ((float) $fields['amount'] <= 0)
		{
			$errors['amount'] = array('Amount must be greater than zero.');
		}
		$tomorrow = (new DateTimeImmutable('tomorrow', new DateTimeZone('UTC')))->format('Y-m-d');
		if ($fields['date'] > $tomorrow)
		{
			$errors['date'] = array('Date cannot be more than 1 day in the future.');
		}
		return $errors;
	}

	public function set_receipt_url($id, $business_id, $url)
	{
		$now = mysql_now();
		$this->db->where('id', $id)->where('business_id', $business_id)
			->update('expenses', array('receipt_image_url' => $url, 'updated_at' => $now));
	}
}
