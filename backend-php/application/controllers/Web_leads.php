<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /leads, /leads/{id} — read-only web views over Lead_model. */
class Web_leads extends Web_Controller {

	public function __construct()
	{
		parent::__construct();
		$this->load->model('Lead_model');
	}

	public function index()
	{
		$leads = $this->Lead_model->all_for_business($this->business_id);
		$this->render('leads/index', array(
			'page_title' => 'Leads',
			'active_nav' => 'leads',
			'leads' => $leads,
		));
	}

	public function show($id)
	{
		$lead = $this->Lead_model->find($id, $this->business_id);
		if ($lead === NULL)
		{
			show_404();
		}

		$converted_customer = NULL;
		if (!empty($lead['converted_customer_id']))
		{
			$converted_customer = $this->db->where('id', $lead['converted_customer_id'])
				->get('customers')->row_array();
		}

		$this->render('leads/show', array(
			'page_title' => $lead['name'],
			'active_nav' => 'leads',
			'lead' => $lead,
			'converted_customer' => $converted_customer,
		));
	}
}
