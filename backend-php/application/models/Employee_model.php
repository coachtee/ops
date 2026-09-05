<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** Mirrors backend/people/models.py's Employee — see docs/API_CONTRACT.md's
 * "employee" field payload. pay_rate/pay_rate_type are informational only,
 * never used to auto-compute a payslip's gross_pay. */
class Employee_model extends Business_owned_model {

	protected $table = 'employees';

	public function from_wire(array $fields)
	{
		return array(
			'name' => $fields['name'],
			'role' => $fields['role'] ?? '',
			'phone' => $fields['phone'] ?? '',
			'email' => $fields['email'] ?? '',
			'pay_rate_type' => $fields['pay_rate_type'] ?? 'monthly',
			'pay_rate' => $fields['pay_rate'] ?? '0.00',
			'start_date' => $fields['start_date'] ?? NULL,
			'notes' => $fields['notes'] ?? '',
		);
	}

	public function to_wire(array $row)
	{
		return array(
			'name' => $row['name'],
			'role' => $row['role'],
			'phone' => $row['phone'],
			'email' => $row['email'],
			'pay_rate_type' => $row['pay_rate_type'],
			'pay_rate' => $row['pay_rate'],
			'start_date' => $row['start_date'],
			'notes' => $row['notes'],
		);
	}

	public function to_sync_change(array $row)
	{
		return array(
			'model' => 'employee',
			'id' => $row['id'],
			'updated_at' => iso8601($row['updated_at']),
			'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			'fields' => $this->to_wire($row),
		);
	}
}
