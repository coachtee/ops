<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** Mirrors backend/people/models.py's Payslip — see docs/API_CONTRACT.md's
 * "payslip" field payload. net_pay is always server-derived
 * (gross_pay - deductions), never taken from client input — see
 * money_helper.php's money_compute_net_pay() and Sync::push_post(), which
 * computes it inline before insert/update, same pattern as
 * quote/invoice line_total. */
class Payslip_model extends Business_owned_model {

	protected $table = 'payslips';

	public function from_wire(array $fields)
	{
		return array(
			'employee_id' => $fields['employee_id'],
			'period_start' => $fields['period_start'],
			'period_end' => $fields['period_end'],
			'gross_pay' => $fields['gross_pay'],
			'deductions' => $fields['deductions'] ?? '0.00',
			'deductions_note' => $fields['deductions_note'] ?? '',
			'paid_date' => $fields['paid_date'] ?? NULL,
			'notes' => $fields['notes'] ?? '',
		);
	}

	public function to_wire(array $row)
	{
		return array(
			'employee_id' => $row['employee_id'],
			'period_start' => $row['period_start'],
			'period_end' => $row['period_end'],
			'gross_pay' => $row['gross_pay'],
			'deductions' => $row['deductions'],
			'deductions_note' => $row['deductions_note'],
			'net_pay' => $row['net_pay'],
			'paid_date' => $row['paid_date'],
			'notes' => $row['notes'],
		);
	}

	public function to_sync_change(array $row)
	{
		return array(
			'model' => 'payslip',
			'id' => $row['id'],
			'updated_at' => iso8601($row['updated_at']),
			'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			'fields' => $this->to_wire($row),
		);
	}

	/** deductions cannot exceed gross_pay; period_end cannot be before
	 * period_start — see docs/API_CONTRACT.md's payslip validation rules. */
	public function validate(array $fields)
	{
		$errors = array();
		if ((float) $fields['deductions'] > (float) $fields['gross_pay'])
		{
			$errors['deductions'] = array('Deductions cannot exceed gross pay.');
		}
		if ($fields['period_end'] < $fields['period_start'])
		{
			$errors['period_end'] = array('Period end cannot be before period start.');
		}
		return $errors;
	}
}
