<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /dashboard — the web admin panel's landing page: this month's cash-basis
 * revenue/expenses/profit (same definition as GET /api/reports/
 * profit-summary/), outstanding invoices, open quotes, and recent activity. */
class Web_dashboard extends Web_Controller {

	public function index()
	{
		$month_start = date('Y-m-01');

		$revenue = $this->db->select('COALESCE(SUM(amount), 0) AS total')
			->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->where('paid_date >=', $month_start)->get('payments')->row_array()['total'];

		$expenses = $this->db->select('COALESCE(SUM(amount), 0) AS total')
			->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->where('date >=', $month_start)->get('expenses')->row_array()['total'];

		$outstanding = $this->db->select('COALESCE(SUM(total - amount_paid), 0) AS total')
			->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->where_not_in('status', array('draft', 'paid', 'cancelled'))
			->get('invoices')->row_array()['total'];

		$open_quotes = $this->db->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->where_in('status', array('draft', 'sent'))->count_all_results('quotes');

		$recent_leads = $this->db->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->order_by('created_at', 'DESC')->limit(5)->get('leads')->result_array();

		$recent_invoices = $this->db->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->order_by('created_at', 'DESC')->limit(5)->get('invoices')->result_array();
		foreach ($recent_invoices as &$invoice)
		{
			$invoice['customer_name'] = $this->_customer_name($invoice['customer_id']);
		}
		unset($invoice);

		$this->render('dashboard', array(
			'page_title' => 'Dashboard',
			'active_nav' => 'dashboard',
			'revenue' => money_quantize($revenue),
			'expenses' => money_quantize($expenses),
			'profit' => money_quantize((float) $revenue - (float) $expenses),
			'outstanding' => money_quantize($outstanding),
			'open_quotes' => $open_quotes,
			'recent_leads' => $recent_leads,
			'recent_invoices' => $recent_invoices,
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
