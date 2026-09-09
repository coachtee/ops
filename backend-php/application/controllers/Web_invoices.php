<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /invoices, /invoices/{id} — totals, amount_paid and status are all
 * server-derived by the sync push path; this only reads them. */
class Web_invoices extends Web_resource_controller {

	protected $table = 'invoices';
	protected $view_dir = 'invoices';
	protected $base_path = 'invoices';
	protected $nav_key = 'invoices';
	protected $title = 'Invoices';
	protected $subtitle = 'Who owes you money, and how much.';
	protected $search_fields = array('number', 'notes');
	protected $search_related = array('column' => 'customer_id', 'table' => 'customers', 'field' => 'name');
	protected $search_placeholder = 'Search number or customer…';
	protected $status_options = array(
		'draft' => 'Draft',
		'sent' => 'Sent',
		'partially_paid' => 'Partially paid',
		'paid' => 'Paid',
		'overdue' => 'Overdue',
		'cancelled' => 'Cancelled',
	);

	protected function decorate(array $rows)
	{
		$names = $this->name_map('customers', array_column($rows, 'customer_id'));
		foreach ($rows as &$row)
		{
			$row['customer_name'] = $names[$row['customer_id']] ?? '—';
			$row['outstanding'] = (float) $row['total'] - (float) $row['amount_paid'];
		}
		return $rows;
	}

	protected function extra_index_data()
	{
		$row = $this->db->select('COALESCE(SUM(total - amount_paid), 0) AS outstanding, COALESCE(SUM(total), 0) AS billed')
			->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->where_not_in('status', array('draft', 'cancelled'))
			->get('invoices')->row_array();

		$overdue = $this->db->select('COALESCE(SUM(total - amount_paid), 0) AS amount, COUNT(*) AS n')
			->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->where_not_in('status', array('draft', 'paid', 'cancelled'))
			->where('due_date <', date('Y-m-d'))
			->where('due_date IS NOT NULL', NULL, FALSE)
			->get('invoices')->row_array();

		return array(
			'sum_outstanding' => $row['outstanding'],
			'sum_billed' => $row['billed'],
			'overdue_amount' => $overdue['amount'],
			'overdue_count' => $overdue['n'],
		);
	}

	public function show($id)
	{
		$invoice = $this->find_or_404($id);
		$customer = $this->db->where('id', $invoice['customer_id'])->where('business_id', $this->business_id)
			->get('customers')->row_array();

		$line_items = $this->db->where('business_id', $this->business_id)->where('invoice_id', $id)
			->where('deleted_at', NULL)->order_by('sort_order', 'ASC')->get('invoice_line_items')->result_array();
		$payments = $this->db->where('business_id', $this->business_id)->where('invoice_id', $id)
			->where('deleted_at', NULL)->order_by('paid_date', 'DESC')->get('payments')->result_array();

		$job = NULL;
		if (!empty($invoice['job_id']))
		{
			$job = $this->db->where('id', $invoice['job_id'])->where('business_id', $this->business_id)
				->get('jobs')->row_array();
		}

		$this->render('invoices/show', array(
			'page_title' => $invoice['number'] ?: 'Draft invoice',
			'active_nav' => 'invoices',
			'crumbs' => array(array('label' => 'Invoices', 'url' => 'invoices'), array('label' => $invoice['number'] ?: 'Draft invoice')),
			'invoice' => $invoice,
			'customer' => $customer,
			'line_items' => $line_items,
			'payments' => $payments,
			'job' => $job,
		));
	}
}
