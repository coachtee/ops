<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /leads, /leads/{id} */
class Web_leads extends Web_resource_controller {

	protected $table = 'leads';
	protected $view_dir = 'leads';
	protected $base_path = 'leads';
	protected $nav_key = 'leads';
	protected $title = 'Leads';
	protected $subtitle = 'Enquiries that haven\'t become customers yet.';
	protected $search_fields = array('name', 'phone', 'email', 'enquiry');
	protected $search_placeholder = 'Search name, phone, email…';
	protected $status_options = array(
		'new' => 'New',
		'contacted' => 'Contacted',
		'quoted' => 'Quoted',
		'converted' => 'Converted',
		'lost' => 'Lost',
	);

	protected function extra_index_data()
	{
		return array('pipeline' => $this->pipeline_counts());
	}

	/** Counts per status for the small pipeline strip above the table —
	 * "where is everything sitting" is the first question about leads. */
	private function pipeline_counts()
	{
		$rows = $this->db->select('status, COUNT(*) AS n')
			->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->group_by('status')->get('leads')->result_array();

		$counts = array_fill_keys(array_keys($this->status_options), 0);
		foreach ($rows as $row)
		{
			$counts[$row['status']] = (int) $row['n'];
		}
		return $counts;
	}

	public function show($id)
	{
		$lead = $this->find_or_404($id);

		$converted_customer = NULL;
		if (!empty($lead['converted_customer_id']))
		{
			$converted_customer = $this->db->where('id', $lead['converted_customer_id'])
				->where('business_id', $this->business_id)->get('customers')->row_array();
		}

		$quotes = $this->db->where('business_id', $this->business_id)->where('lead_id', $id)
			->where('deleted_at', NULL)->order_by('created_at', 'DESC')->get('quotes')->result_array();

		$this->render('leads/show', array(
			'page_title' => $lead['name'],
			'active_nav' => 'leads',
			'crumbs' => array(array('label' => 'Leads', 'url' => 'leads'), array('label' => $lead['name'])),
			'lead' => $lead,
			'converted_customer' => $converted_customer,
			'quotes' => $quotes,
		));
	}
}
