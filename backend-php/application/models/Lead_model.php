<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** Mirrors backend/crm/models.py's Lead — see docs/API_CONTRACT.md's "lead"
 * field payload. */
class Lead_model extends Business_owned_model {

	protected $table = 'leads';

	public function from_wire(array $fields)
	{
		return array(
			'name' => $fields['name'],
			'phone' => $fields['phone'] ?? '',
			'email' => $fields['email'] ?? '',
			'source' => $fields['source'] ?? 'other',
			'enquiry' => $fields['enquiry'] ?? '',
			'notes' => $fields['notes'] ?? '',
			'status' => $fields['status'] ?? 'new',
			'follow_up_date' => $fields['follow_up_date'] ?? NULL,
			'converted_customer_id' => $fields['converted_customer_id'] ?? NULL,
		);
	}

	public function to_wire(array $row)
	{
		return array(
			'name' => $row['name'],
			'phone' => $row['phone'],
			'email' => $row['email'],
			'source' => $row['source'],
			'enquiry' => $row['enquiry'],
			'notes' => $row['notes'],
			'status' => $row['status'],
			'follow_up_date' => $row['follow_up_date'],
			'converted_customer_id' => $row['converted_customer_id'],
		);
	}

	public function to_sync_change(array $row)
	{
		return array(
			'model' => 'lead',
			'id' => $row['id'],
			'updated_at' => iso8601($row['updated_at']),
			'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			'fields' => $this->to_wire($row),
		);
	}
}
