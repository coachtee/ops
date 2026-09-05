<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /quotes, /quotes/{id} — read-only web views over Quote_model + its
 * line items. Totals are always whatever Sync::push_post() last
 * recomputed — never re-derived here. */
class Web_quotes extends Web_Controller {

	public function __construct()
	{
		parent::__construct();
		$this->load->model('Quote_model');
	}

	public function index()
	{
		$quotes = $this->Quote_model->all_for_business($this->business_id);
		foreach ($quotes as &$quote)
		{
			$quote['customer_name'] = $this->_customer_name($quote['customer_id']);
		}
		unset($quote);

		$this->render('quotes/index', array(
			'page_title' => 'Quotes',
			'active_nav' => 'quotes',
			'quotes' => $quotes,
		));
	}

	public function show($id)
	{
		$quote = $this->Quote_model->find($id, $this->business_id);
		if ($quote === NULL)
		{
			show_404();
		}
		$quote['customer_name'] = $this->_customer_name($quote['customer_id']);

		$line_items = $this->db->where('business_id', $this->business_id)->where('quote_id', $id)
			->where('deleted_at', NULL)->order_by('sort_order', 'ASC')->get('quote_line_items')->result_array();

		$this->render('quotes/show', array(
			'page_title' => $quote['number'] ?: 'Draft quote',
			'active_nav' => 'quotes',
			'quote' => $quote,
			'line_items' => $line_items,
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
