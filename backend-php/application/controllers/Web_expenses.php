<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /expenses — money out. `amount` is the VAT-inclusive total that was
 * actually paid; `vat_amount` is the portion of it that was already VAT,
 * extracted server-side (never added on top). */
class Web_expenses extends Web_resource_controller {

	protected $table = 'expenses';
	protected $view_dir = 'expenses';
	protected $base_path = 'expenses';
	protected $nav_key = 'expenses';
	protected $title = 'Expenses';
	protected $subtitle = 'Money out, VAT-inclusive, with the input VAT split out for SARS.';
	protected $search_fields = array('description');
	protected $search_related = array('column' => 'supplier_id', 'table' => 'suppliers', 'field' => 'name');
	protected $search_placeholder = 'Search description or supplier…';
	protected $status_field = 'category';
	protected $order_by = 'date';

	public function __construct()
	{
		parent::__construct();
		// Same vocabulary as the API's Reports controller, kept in one shape
		// so a category label reads identically in the panel and the CSV.
		$this->status_options = array(
			'materials_stock' => 'Materials & stock',
			'fuel_travel' => 'Fuel & travel',
			'tools_equipment' => 'Tools & equipment',
			'rent' => 'Rent',
			'utilities' => 'Utilities',
			'insurance' => 'Insurance',
			'bank_charges' => 'Bank charges',
			'professional_fees' => 'Professional fees',
			'marketing' => 'Marketing & advertising',
			'telephone_internet' => 'Telephone & internet',
			'vehicle' => 'Vehicle expenses',
			'repairs_maintenance' => 'Repairs & maintenance',
			'wages_subcontractors' => 'Wages & subcontractors',
			'other' => 'Other',
		);
	}

	protected function decorate(array $rows)
	{
		$suppliers = $this->name_map('suppliers', array_column($rows, 'supplier_id'));
		$jobs = $this->name_map('jobs', array_column($rows, 'job_id'), 'title');
		foreach ($rows as &$row)
		{
			$row['supplier_name'] = $row['supplier_id'] ? ($suppliers[$row['supplier_id']] ?? '—') : NULL;
			$row['job_title'] = $row['job_id'] ? ($jobs[$row['job_id']] ?? '—') : NULL;
			$row['category_label'] = $this->status_options[$row['category']] ?? $row['category'];
		}
		return $rows;
	}

	protected function extra_index_data()
	{
		$month = $this->db->select('COALESCE(SUM(amount), 0) AS total, COALESCE(SUM(vat_amount), 0) AS vat')
			->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->where('date >=', date('Y-m-01'))
			->get('expenses')->row_array();

		return array('month_total' => $month['total'], 'month_vat' => $month['vat']);
	}
}
