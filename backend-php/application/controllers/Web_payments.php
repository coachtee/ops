<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /payments — money actually received. This is the "revenue" the dashboard
 * and reports count (cash basis), not invoice totals. */
class Web_payments extends Web_resource_controller {

	protected $table = 'payments';
	protected $view_dir = 'payments';
	protected $base_path = 'payments';
	protected $nav_key = 'payments';
	protected $title = 'Payments';
	protected $subtitle = 'Money in — the cash-basis figure every report uses.';
	protected $search_fields = array('reference', 'notes');
	protected $search_related = array('column' => 'customer_id', 'table' => 'customers', 'field' => 'name');
	protected $search_placeholder = 'Search reference or customer…';
	protected $status_field = 'method';
	protected $status_options = array(
		'cash' => 'Cash',
		'eft' => 'EFT',
		'card' => 'Card',
		'snapscan' => 'SnapScan',
		'other' => 'Other',
	);
	protected $order_by = 'paid_date';

	protected function decorate(array $rows)
	{
		$customers = $this->name_map('customers', array_column($rows, 'customer_id'));
		$invoices = $this->name_map('invoices', array_column($rows, 'invoice_id'), 'number');
		foreach ($rows as &$row)
		{
			$row['customer_name'] = $customers[$row['customer_id']] ?? '—';
			$row['invoice_number'] = $row['invoice_id'] ? ($invoices[$row['invoice_id']] ?? '—') : NULL;
		}
		return $rows;
	}

	protected function extra_index_data()
	{
		$month = $this->db->select('COALESCE(SUM(amount), 0) AS total')
			->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->where('paid_date >=', date('Y-m-01'))
			->get('payments')->row_array();

		return array('month_total' => $month['total']);
	}
}
