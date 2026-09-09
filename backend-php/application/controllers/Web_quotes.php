<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /quotes, /quotes/{id} — totals shown are whatever Sync::push_post() last
 * recomputed from the line items; nothing is re-derived here. */
class Web_quotes extends Web_resource_controller {

	protected $table = 'quotes';
	protected $view_dir = 'quotes';
	protected $base_path = 'quotes';
	protected $nav_key = 'quotes';
	protected $title = 'Quotes';
	protected $subtitle = 'What you\'ve priced, and who hasn\'t answered yet.';
	protected $search_fields = array('number', 'notes');
	protected $search_related = array('column' => 'customer_id', 'table' => 'customers', 'field' => 'name');
	protected $search_placeholder = 'Search number or customer…';
	protected $status_options = array(
		'draft' => 'Draft',
		'sent' => 'Sent',
		'accepted' => 'Accepted',
		'declined' => 'Declined',
		'expired' => 'Expired',
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

	protected function extra_index_data()
	{
		$open = $this->db->select('COALESCE(SUM(total), 0) AS total, COUNT(*) AS n')
			->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->where_in('status', array('draft', 'sent'))
			->get('quotes')->row_array();

		return array('open_value' => $open['total'], 'open_count' => $open['n']);
	}

	public function show($id)
	{
		$quote = $this->find_or_404($id);
		$names = $this->name_map('customers', array($quote['customer_id']));
		$quote['customer_name'] = $names[$quote['customer_id']] ?? '—';

		$customer = $this->db->where('id', $quote['customer_id'])->where('business_id', $this->business_id)
			->get('customers')->row_array();

		$line_items = $this->db->where('business_id', $this->business_id)->where('quote_id', $id)
			->where('deleted_at', NULL)->order_by('sort_order', 'ASC')->get('quote_line_items')->result_array();

		$jobs = $this->db->where('business_id', $this->business_id)->where('quote_id', $id)
			->where('deleted_at', NULL)->get('jobs')->result_array();
		$invoices = $this->db->where('business_id', $this->business_id)->where('quote_id', $id)
			->where('deleted_at', NULL)->get('invoices')->result_array();

		$this->render('quotes/show', array(
			'page_title' => $quote['number'] ?: 'Draft quote',
			'active_nav' => 'quotes',
			'crumbs' => array(array('label' => 'Quotes', 'url' => 'quotes'), array('label' => $quote['number'] ?: 'Draft quote')),
			'quote' => $quote,
			'customer' => $customer,
			'line_items' => $line_items,
			'jobs' => $jobs,
			'invoices' => $invoices,
		));
	}
}
