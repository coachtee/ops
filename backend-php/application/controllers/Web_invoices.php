<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /invoices, /invoices/{id} — read-only web views over Invoice_model + its
 * line items + payments. Totals/amount_paid/status are always whatever
 * Sync::push_post() last recomputed — never re-derived here. */
class Web_invoices extends Web_Controller {

	public function __construct()
	{
		parent::__construct();
		$this->load->model('Invoice_model');
	}

	public function index()
	{
		$invoices = $this->Invoice_model->all_for_business($this->business_id);
		foreach ($invoices as &$invoice)
		{
			$invoice['customer_name'] = $this->_customer_name($invoice['customer_id']);
		}
		unset($invoice);

		$this->render('invoices/index', array(
			'page_title' => 'Invoices',
			'active_nav' => 'invoices',
			'invoices' => $invoices,
		));
	}

	public function show($id)
	{
		$invoice = $this->Invoice_model->find($id, $this->business_id);
		if ($invoice === NULL)
		{
			show_404();
		}
		$invoice['customer_name'] = $this->_customer_name($invoice['customer_id']);

		$line_items = $this->db->where('business_id', $this->business_id)->where('invoice_id', $id)
			->where('deleted_at', NULL)->order_by('sort_order', 'ASC')->get('invoice_line_items')->result_array();
		$payments = $this->db->where('business_id', $this->business_id)->where('invoice_id', $id)
			->where('deleted_at', NULL)->order_by('paid_date', 'DESC')->get('payments')->result_array();

		$this->render('invoices/show', array(
			'page_title' => $invoice['number'] ?: 'Draft invoice',
			'active_nav' => 'invoices',
			'invoice' => $invoice,
			'line_items' => $line_items,
			'payments' => $payments,
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
