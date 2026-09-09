<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /jobs, /jobs/{id} */
class Web_jobs extends Web_resource_controller {

	protected $table = 'jobs';
	protected $view_dir = 'jobs';
	protected $base_path = 'jobs';
	protected $nav_key = 'jobs';
	protected $title = 'Jobs';
	protected $subtitle = 'The work itself — scheduled, in progress and done.';
	protected $search_fields = array('number', 'title', 'description');
	protected $search_related = array('column' => 'customer_id', 'table' => 'customers', 'field' => 'name');
	protected $search_placeholder = 'Search number, title or customer…';
	protected $status_options = array(
		'not_started' => 'Not started',
		'in_progress' => 'In progress',
		'completed' => 'Completed',
		'cancelled' => 'Cancelled',
	);

	protected function decorate(array $rows)
	{
		$names = $this->name_map('customers', array_column($rows, 'customer_id'));
		foreach ($rows as &$row)
		{
			$row['customer_name'] = $names[$row['customer_id']] ?? '—';
		}
		return $rows;
	}

	public function show($id)
	{
		$job = $this->find_or_404($id);
		$customer = $this->db->where('id', $job['customer_id'])->where('business_id', $this->business_id)
			->get('customers')->row_array();

		$visits = $this->db->where('business_id', $this->business_id)->where('job_id', $id)
			->where('deleted_at', NULL)->order_by('scheduled_date', 'ASC')->get('visits')->result_array();
		$employees = $this->name_map('employees', array_column($visits, 'employee_id'));
		foreach ($visits as &$visit)
		{
			$visit['employee_name'] = $visit['employee_id'] ? ($employees[$visit['employee_id']] ?? '—') : NULL;
		}
		unset($visit);

		$expenses = $this->db->where('business_id', $this->business_id)->where('job_id', $id)
			->where('deleted_at', NULL)->order_by('date', 'DESC')->get('expenses')->result_array();
		$invoices = $this->db->where('business_id', $this->business_id)->where('job_id', $id)
			->where('deleted_at', NULL)->get('invoices')->result_array();

		$spent = 0.0;
		foreach ($expenses as $expense)
		{
			$spent += (float) $expense['amount'];
		}
		$billed = 0.0;
		foreach ($invoices as $invoice)
		{
			if (!in_array($invoice['status'], array('draft', 'cancelled'), TRUE))
			{
				$billed += (float) $invoice['total'];
			}
		}

		$this->render('jobs/show', array(
			'page_title' => $job['number'] ?: $job['title'],
			'active_nav' => 'jobs',
			'crumbs' => array(array('label' => 'Jobs', 'url' => 'jobs'), array('label' => $job['number'] ?: $job['title'])),
			'job' => $job,
			'customer' => $customer,
			'visits' => $visits,
			'expenses' => $expenses,
			'invoices' => $invoices,
			'spent' => $spent,
			'billed' => $billed,
		));
	}
}
