<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /settings — business profile as it stands, the connection details the
 * phone app needs, and what has actually synced so far. Read-only: the app
 * remains the one place this data is edited. */
class Web_settings extends Web_Controller {

	public function index()
	{
		$user = $this->db->where('id', $this->user_id)->get('users')->row_array();

		$counts = array();
		$tables = array(
			'leads' => 'Leads', 'customers' => 'Customers', 'quotes' => 'Quotes', 'jobs' => 'Jobs',
			'visits' => 'Visits', 'invoices' => 'Invoices', 'payments' => 'Payments',
			'expenses' => 'Expenses', 'suppliers' => 'Suppliers', 'employees' => 'Employees',
			'payslips' => 'Payslips', 'compliance_items' => 'Compliance items',
		);
		foreach ($tables as $table => $label)
		{
			$counts[$label] = $this->db->where('business_id', $this->business_id)
				->where('deleted_at', NULL)->count_all_results($table);
		}

		$last_sync = $this->db->select('MAX(updated_at) AS last')
			->where('business_id', $this->business_id)
			->get('customers')->row_array();

		$this->render('settings', array(
			'page_title' => 'Settings',
			'active_nav' => 'settings',
			'crumbs' => array(array('label' => 'Settings')),
			'user' => $user,
			'counts' => $counts,
			'api_base' => rtrim(base_url(), '/'),
			'last_sync' => $last_sync['last'] ?? NULL,
		));
	}
}
