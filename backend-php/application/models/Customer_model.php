<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** Mirrors backend/crm/models.py's Customer + CustomerSerializer — see
 * docs/API_CONTRACT.md's "customer" field payload. */
class Customer_model extends Business_owned_model {

	protected $table = 'customers';

	public function from_wire(array $fields)
	{
		return array(
			'name' => $fields['name'],
			'customer_type' => $fields['customer_type'] ?? 'individual',
			'phone' => $fields['phone'] ?? '',
			'email' => $fields['email'] ?? '',
			'address_line1' => $fields['address_line1'] ?? '',
			'address_line2' => $fields['address_line2'] ?? '',
			'suburb' => $fields['suburb'] ?? '',
			'city' => $fields['city'] ?? '',
			'province' => $fields['province'] ?? '',
			'postal_code' => $fields['postal_code'] ?? '',
			'notes' => $fields['notes'] ?? '',
			'source_lead_id' => $fields['source_lead_id'] ?? NULL,
		);
	}

	public function to_wire(array $row)
	{
		return array(
			'name' => $row['name'],
			'customer_type' => $row['customer_type'],
			'phone' => $row['phone'],
			'email' => $row['email'],
			'address_line1' => $row['address_line1'],
			'address_line2' => $row['address_line2'],
			'suburb' => $row['suburb'],
			'city' => $row['city'],
			'province' => $row['province'],
			'postal_code' => $row['postal_code'],
			'notes' => $row['notes'],
			'source_lead_id' => $row['source_lead_id'],
		);
	}

	/** Full sync-changes-array shape: id/updated_at/deleted_at/fields. */
	public function to_sync_change(array $row)
	{
		return array(
			'model' => 'customer',
			'id' => $row['id'],
			'updated_at' => iso8601($row['updated_at']),
			'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			'fields' => $this->to_wire($row),
		);
	}
}
