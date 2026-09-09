<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /employees, /employees/{id} — a staff contact plus the agreed pay rate.
 * The rate is a reminder of what was agreed, never used to auto-compute a
 * payslip (that would need hours tracking this product deliberately
 * doesn't do). */
class Web_employees extends Web_resource_controller {

	protected $table = 'employees';
	protected $view_dir = 'employees';
	protected $base_path = 'employees';
	protected $nav_key = 'employees';
	protected $title = 'Employees';
	protected $subtitle = 'Who works for you, and what you agreed to pay them.';
	protected $search_fields = array('name', 'role', 'phone', 'email');
	protected $search_placeholder = 'Search name, role, phone…';
	protected $status_field = 'pay_rate_type';
	protected $status_options = array('hourly' => 'Hourly', 'daily' => 'Daily', 'monthly' => 'Monthly');
	protected $order_by = 'name';
	protected $order_dir = 'ASC';

	public function show($id)
	{
		$employee = $this->find_or_404($id);

		$payslips = $this->db->where('business_id', $this->business_id)->where('employee_id', $id)
			->where('deleted_at', NULL)->order_by('period_end', 'DESC')->limit(24)->get('payslips')->result_array();

		$visits = $this->db->where('business_id', $this->business_id)->where('employee_id', $id)
			->where('deleted_at', NULL)->order_by('scheduled_date', 'DESC')->limit(10)->get('visits')->result_array();
		$job_titles = $this->name_map('jobs', array_column($visits, 'job_id'), 'title');
		foreach ($visits as &$visit)
		{
			$visit['job_title'] = $job_titles[$visit['job_id']] ?? '—';
		}
		unset($visit);

		$paid = $this->db->select('COALESCE(SUM(net_pay), 0) AS total')
			->where('business_id', $this->business_id)->where('employee_id', $id)
			->where('deleted_at', NULL)->get('payslips')->row_array();

		$this->render('employees/show', array(
			'page_title' => $employee['name'],
			'active_nav' => 'employees',
			'crumbs' => array(array('label' => 'Employees', 'url' => 'employees'), array('label' => $employee['name'])),
			'employee' => $employee,
			'payslips' => $payslips,
			'visits' => $visits,
			'paid_total' => $paid['total'],
		));
	}
}
