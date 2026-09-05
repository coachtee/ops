<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** Mirrors backend/finance/models.py's Supplier — see docs/API_CONTRACT.md's
 * "supplier" field payload. */
class Supplier_model extends Business_owned_model {

	protected $table = 'suppliers';

	public function from_wire(array $fields)
	{
		return array(
			'name' => $fields['name'],
			'contact_person' => $fields['contact_person'] ?? '',
			'phone' => $fields['phone'] ?? '',
			'email' => $fields['email'] ?? '',
			'notes' => $fields['notes'] ?? '',
		);
	}

	public function to_wire(array $row)
	{
		return array(
			'name' => $row['name'],
			'contact_person' => $row['contact_person'],
			'phone' => $row['phone'],
			'email' => $row['email'],
			'notes' => $row['notes'],
		);
	}

	public function to_sync_change(array $row)
	{
		return array(
			'model' => 'supplier',
			'id' => $row['id'],
			'updated_at' => iso8601($row['updated_at']),
			'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			'fields' => $this->to_wire($row),
		);
	}
}
