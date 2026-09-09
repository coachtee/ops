<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /customers, /customers/{id} */
class Web_customers extends Web_resource_controller {

	protected $table = 'customers';
	protected $view_dir = 'customers';
	protected $base_path = 'customers';
	protected $nav_key = 'customers';
	protected $title = 'Customers';
	protected $subtitle = 'Everyone you invoice.';
	protected $search_fields = array('name', 'phone', 'email', 'city', 'suburb');
	protected $search_placeholder = 'Search name, phone, email, city…';
	protected $status_field = 'customer_type';
	protected $status_options = array('individual' => 'Individuals', 'company' => 'Companies');
	protected $order_by = 'name';
	protected $order_dir = 'ASC';

	/** Lifetime invoiced + still-outstanding per customer, batched into two
	 * queries for the whole page rather than two per row. */
	protected function decorate(array $rows)
	{
		$ids = array_column($rows, 'id');
		if (empty($ids))
		{
			return $rows;
		}

		$totals = $this->db->select('customer_id, SUM(total) AS invoiced, SUM(total - amount_paid) AS outstanding')
			->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->where_not_in('status', array('draft', 'cancelled'))
			->where_in('customer_id', $ids)->group_by('customer_id')
			->get('invoices')->result_array();

		$by_customer = array();
		foreach ($totals as $row)
		{
			$by_customer[$row['customer_id']] = $row;
		}

		foreach ($rows as &$row)
		{
			$row['invoiced'] = $by_customer[$row['id']]['invoiced'] ?? '0.00';
			$row['outstanding'] = $by_customer[$row['id']]['outstanding'] ?? '0.00';
		}
		return $rows;
	}

	public function show($id)
	{
		$customer = $this->find_or_404($id);

		$related = function ($table, $order) use ($id) {
			return $this->db->where('business_id', $this->business_id)->where('customer_id', $id)
				->where('deleted_at', NULL)->order_by($order, 'DESC')->get($table)->result_array();
		};

		$invoices = $related('invoices', 'created_at');
		$outstanding = 0.0;
		$invoiced = 0.0;
		foreach ($invoices as $invoice)
		{
			if (in_array($invoice['status'], array('draft', 'cancelled'), TRUE))
			{
				continue;
			}
			$invoiced += (float) $invoice['total'];
			$outstanding += (float) $invoice['total'] - (float) $invoice['amount_paid'];
		}

		$this->render('customers/show', array(
			'page_title' => $customer['name'],
			'active_nav' => 'customers',
			'crumbs' => array(array('label' => 'Customers', 'url' => 'customers'), array('label' => $customer['name'])),
			'customer' => $customer,
			'quotes' => $related('quotes', 'created_at'),
			'jobs' => $related('jobs', 'created_at'),
			'invoices' => $invoices,
			'payments' => $related('payments', 'paid_date'),
			'invoiced' => $invoiced,
			'outstanding' => $outstanding,
		));
	}
}
