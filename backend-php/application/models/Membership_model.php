<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * V1 assumption unchanged from the Django backend this replaces (see
 * docs/DISCOVERY.md): a user belongs to exactly one business.
 */
class Membership_model extends CI_Model {

	/** Returns array('business_id' => ..., 'role' => ...) or NULL. */
	public function for_user($user_id)
	{
		$row = $this->db->select('business_id, role')
			->where('user_id', $user_id)
			->get('memberships')
			->row_array();
		return $row ?: NULL;
	}

	public function create($user_id, $business_id, $role = 'owner')
	{
		$now = date('Y-m-d H:i:s');
		$this->db->insert('memberships', array(
			'id' => uuid4(),
			'user_id' => $user_id,
			'business_id' => $business_id,
			'role' => $role,
			'created_at' => $now,
			'updated_at' => $now,
		));
	}
}
