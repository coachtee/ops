<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * /payslips — gross pay and deductions are both entered by the owner (or
 * copied from their bookkeeper); net_pay is the one derived field. This
 * product does not compute PAYE/UIF tax tables and makes no claim of
 * payroll-tax accuracy or e-filing — see docs/API_CONTRACT.md.
 */
class Web_payslips extends Web_resource_controller {

	protected $table = 'payslips';
	protected $view_dir = 'payslips';
	protected $base_path = 'payslips';
	protected $nav_key = 'payslips';
	protected $title = 'Payslips';
	protected $subtitle = 'A record of what was paid, per person, per period.';
	protected $search_fields = array('deductions_note', 'notes');
	protected $search_related = array('column' => 'employee_id', 'table' => 'employees', 'field' => 'name');
	protected $search_placeholder = 'Search employee or note…';
	protected $order_by = 'period_end';

	protected function decorate(array $rows)
	{
		$names = $this->name_map('employees', array_column($rows, 'employee_id'));
		foreach ($rows as &$row)
		{
			$row['employee_name'] = $names[$row['employee_id']] ?? '—';
		}
		return $rows;
	}

	protected function extra_index_data()
	{
		$month = $this->db->select('COALESCE(SUM(net_pay), 0) AS net, COUNT(*) AS n')
			->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->where('period_end >=', date('Y-m-01'))
			->get('payslips')->row_array();

		return array('month_net' => $month['net'], 'month_count' => $month['n']);
	}
}
