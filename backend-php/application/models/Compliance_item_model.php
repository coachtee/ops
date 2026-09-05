<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** Mirrors backend/compliance/models.py's ComplianceItem — see
 * docs/API_CONTRACT.md's "compliance_item" field payload. A plain
 * owner-managed reminder list; no recurrence engine server-side. */
class Compliance_item_model extends Business_owned_model {

	protected $table = 'compliance_items';

	public function from_wire(array $fields)
	{
		return array(
			'category' => $fields['category'] ?? 'other',
			'title' => $fields['title'],
			'due_date' => $fields['due_date'],
			'completed_date' => $fields['completed_date'] ?? NULL,
			'is_recurring' => !empty($fields['is_recurring']) ? 1 : 0,
			'notes' => $fields['notes'] ?? '',
		);
	}

	public function to_wire(array $row)
	{
		return array(
			'category' => $row['category'],
			'title' => $row['title'],
			'due_date' => $row['due_date'],
			'completed_date' => $row['completed_date'],
			'is_recurring' => (bool) $row['is_recurring'],
			'notes' => $row['notes'],
		);
	}

	public function to_sync_change(array $row)
	{
		return array(
			'model' => 'compliance_item',
			'id' => $row['id'],
			'updated_at' => iso8601($row['updated_at']),
			'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			'fields' => $this->to_wire($row),
		);
	}
}
