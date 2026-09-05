<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /jobs, /jobs/{id} — read-only web views over Job_model + its visits. */
class Web_jobs extends Web_Controller {

	public function __construct()
	{
		parent::__construct();
		$this->load->model('Job_model');
	}

	public function index()
	{
		$jobs = $this->Job_model->all_for_business($this->business_id);
		foreach ($jobs as &$job)
		{
			$job['customer_name'] = $this->_customer_name($job['customer_id']);
		}
		unset($job);

		$this->render('jobs/index', array(
			'page_title' => 'Jobs',
			'active_nav' => 'jobs',
			'jobs' => $jobs,
		));
	}

	public function show($id)
	{
		$job = $this->Job_model->find($id, $this->business_id);
		if ($job === NULL)
		{
			show_404();
		}
		$job['customer_name'] = $this->_customer_name($job['customer_id']);

		$visits = $this->db->where('business_id', $this->business_id)->where('job_id', $id)
			->where('deleted_at', NULL)->order_by('scheduled_date', 'ASC')->get('visits')->result_array();

		$this->render('jobs/show', array(
			'page_title' => $job['number'] ?: $job['title'],
			'active_nav' => 'jobs',
			'job' => $job,
			'visits' => $visits,
		));
	}

	private function _customer_name($customer_id)
	{
		if (!$customer_id)
		{
			return '—';
		}
		$row = $this->db->select('name')->where('id', $customer_id)->get('customers')->row_array();
		return $row['name'] ?? '—';
	}
}
