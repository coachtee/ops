<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** Mirrors backend/work/models.py's Visit — see docs/API_CONTRACT.md's
 * "visit" field payload. Wire field 'photo' maps to DB column 'photo_url',
 * read-only here — only ever set via POST /api/visits/{id}/photo/ (see
 * Visits::photo_post()). Customer/address are reached via the parent job,
 * never duplicated here. */
class Visit_model extends Business_owned_model {

	protected $table = 'visits';

	public function from_wire(array $fields)
	{
		return array(
			'job_id' => $fields['job_id'],
			'employee_id' => $fields['employee_id'] ?? NULL,
			'scheduled_date' => $fields['scheduled_date'],
			'start_time' => $fields['start_time'] ?? NULL,
			'end_time' => $fields['end_time'] ?? NULL,
			'status' => $fields['status'] ?? 'scheduled',
			'notes' => $fields['notes'] ?? '',
			'started_at' => $fields['started_at'] ?? NULL,
			'completed_at' => $fields['completed_at'] ?? NULL,
		);
	}

	public function to_wire(array $row)
	{
		return array(
			'job_id' => $row['job_id'],
			'employee_id' => $row['employee_id'],
			'scheduled_date' => $row['scheduled_date'],
			'start_time' => $row['start_time'],
			'end_time' => $row['end_time'],
			'status' => $row['status'],
			'notes' => $row['notes'],
			'started_at' => $row['started_at'] ? iso8601($row['started_at']) : NULL,
			'completed_at' => $row['completed_at'] ? iso8601($row['completed_at']) : NULL,
			'photo' => $row['photo_url'],
		);
	}

	public function to_sync_change(array $row)
	{
		return array(
			'model' => 'visit',
			'id' => $row['id'],
			'updated_at' => iso8601($row['updated_at']),
			'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			'fields' => $this->to_wire($row),
		);
	}

	public function set_photo_url($id, $business_id, $url)
	{
		$now = mysql_now();
		$this->db->where('id', $id)->where('business_id', $business_id)
			->update('visits', array('photo_url' => $url, 'updated_at' => $now));
	}
}
