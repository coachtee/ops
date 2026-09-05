<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** Mirrors backend/accounts/models.py's Business + BusinessSerializer
 * field-for-field — the wire shape (docs/API_CONTRACT.md) is unchanged by
 * this rewrite, only the implementation underneath it. */
class Business_model extends CI_Model {

	public function find($id)
	{
		return $this->db->get_where('businesses', array('id' => $id))->row_array();
	}

	public function create(array $fields)
	{
		$now = date('Y-m-d H:i:s');
		$id = uuid4();
		$this->db->insert('businesses', array(
			'id' => $id,
			'name' => $fields['name'],
			'trading_name' => $fields['trading_name'] ?? '',
			'registration_number' => $fields['registration_number'] ?? '',
			'tax_number' => $fields['tax_number'] ?? '',
			'vat_number' => $fields['vat_number'] ?? '',
			'is_vat_registered' => !empty($fields['is_vat_registered']) ? 1 : 0,
			'phone' => $fields['phone'] ?? '',
			'email' => $fields['email'] ?? '',
			'address_line1' => $fields['address_line1'] ?? '',
			'address_line2' => $fields['address_line2'] ?? '',
			'suburb' => $fields['suburb'] ?? '',
			'city' => $fields['city'] ?? '',
			'province' => $fields['province'] ?? '',
			'postal_code' => $fields['postal_code'] ?? '',
			'industry' => $fields['industry'] ?? 'other',
			'created_at' => $now,
			'updated_at' => $now,
		));
		return $id;
	}

	public function update($id, array $fields)
	{
		$fields['updated_at'] = date('Y-m-d H:i:s');
		$this->db->where('id', $id)->update('businesses', $fields);
	}

	/** Matches BusinessSerializer's exact keys — see BusinessDto.kt on the
	 * Android side, unchanged by this rewrite. */
	public function to_wire(array $row)
	{
		return array(
			'id' => $row['id'],
			'name' => $row['name'],
			'trading_name' => $row['trading_name'],
			'registration_number' => $row['registration_number'],
			'tax_number' => $row['tax_number'],
			'vat_number' => $row['vat_number'],
			'is_vat_registered' => (bool) $row['is_vat_registered'],
			'industry' => $row['industry'],
			'phone' => $row['phone'],
			'email' => $row['email'],
			'address_line1' => $row['address_line1'],
			'address_line2' => $row['address_line2'],
			'suburb' => $row['suburb'],
			'city' => $row['city'],
			'province' => $row['province'],
			'postal_code' => $row['postal_code'],
			'logo' => $row['logo_url'],
			'created_at' => iso8601($row['created_at']),
			'updated_at' => iso8601($row['updated_at']),
		);
	}
}
