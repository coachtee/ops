<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /customers, /customers/{id} — read-only web views over Customer_model
 * (the Android app remains the write path for this data — see
 * docs/API_CONTRACT.md's "Sync" section). */
class Web_customers extends Web_Controller {

	public function __construct()
	{
		parent::__construct();
		$this->load->model('Customer_model');
	}

	public function index()
	{
		$customers = $this->Customer_model->all_for_business($this->business_id);
		$this->render('customers/index', array(
			'page_title' => 'Customers',
			'active_nav' => 'customers',
			'customers' => $customers,
		));
	}

	public function show($id)
	{
		$customer = $this->Customer_model->find($id, $this->business_id);
		if ($customer === NULL)
		{
			show_404();
		}

		$quotes = $this->db->where('business_id', $this->business_id)->where('customer_id', $id)
			->where('deleted_at', NULL)->order_by('created_at', 'DESC')->get('quotes')->result_array();
		$invoices = $this->db->where('business_id', $this->business_id)->where('customer_id', $id)
			->where('deleted_at', NULL)->order_by('created_at', 'DESC')->get('invoices')->result_array();
		$jobs = $this->db->where('business_id', $this->business_id)->where('customer_id', $id)
			->where('deleted_at', NULL)->order_by('created_at', 'DESC')->get('jobs')->result_array();

		$this->render('customers/show', array(
			'page_title' => $customer['name'],
			'active_nav' => 'customers',
			'customer' => $customer,
			'quotes' => $quotes,
			'invoices' => $invoices,
			'jobs' => $jobs,
		));
	}
}
