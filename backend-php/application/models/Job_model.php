<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** Mirrors backend/work/models.py's Job — see docs/API_CONTRACT.md's "job"
 * field payload. number is server-assigned (see
 * Document_sequence_model + Sync::push_post()) and deliberately absent from
 * from_wire()'s output. */
class Job_model extends Business_owned_model {

	protected $table = 'jobs';

	public function from_wire(array $fields)
	{
		return array(
			'customer_id' => $fields['customer_id'],
			'quote_id' => $fields['quote_id'] ?? NULL,
			'title' => $fields['title'],
			'description' => $fields['description'] ?? '',
			'status' => $fields['status'] ?? 'not_started',
			'start_date' => $fields['start_date'] ?? NULL,
			'due_date' => $fields['due_date'] ?? NULL,
			'completed_date' => $fields['completed_date'] ?? NULL,
		);
	}

	public function to_wire(array $row)
	{
		return array(
			'customer_id' => $row['customer_id'],
			'quote_id' => $row['quote_id'],
			'number' => $row['number'],
			'title' => $row['title'],
			'description' => $row['description'],
			'status' => $row['status'],
			'start_date' => $row['start_date'],
			'due_date' => $row['due_date'],
			'completed_date' => $row['completed_date'],
		);
	}

	public function to_sync_change(array $row)
	{
		return array(
			'model' => 'job',
			'id' => $row['id'],
			'updated_at' => iso8601($row['updated_at']),
			'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			'fields' => $this->to_wire($row),
		);
	}
}
