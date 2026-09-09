<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * /reports — the same three aggregations the JSON Reports API exposes,
 * rendered for a browser, plus CSV export. Export goes through this
 * controller rather than linking at /api/reports/…?export=csv because the
 * panel authenticates with a session cookie and the API expects a bearer
 * token; the numbers come from the same Insights_model either way.
 */
class Web_reports extends Web_Controller {

	public function __construct()
	{
		parent::__construct();
		$this->load->model('Insights_model');
	}

	public function index()
	{
		$months = (int) ($this->input->get('months') ?: 6);
		$months = in_array($months, array(3, 6, 12, 24), TRUE) ? $months : 6;

		$period = $this->input->get('period') === 'all_time' ? 'all_time' : 'this_month';
		$since = $this->input->get('since') ?: date('Y-m-01');
		$until = $this->input->get('until') ?: date('Y-m-d');
		if (!$this->valid_date($since)) { $since = date('Y-m-01'); }
		if (!$this->valid_date($until)) { $until = date('Y-m-d'); }

		$series = $this->Insights_model->monthly_series($this->business_id, $months);

		$totals = array('revenue' => 0.0, 'expenses' => 0.0, 'profit' => 0.0);
		foreach ($series as $month)
		{
			$totals['revenue'] += (float) $month['revenue'];
			$totals['expenses'] += (float) $month['expenses'];
			$totals['profit'] += (float) $month['profit'];
		}

		$this->render('reports', array(
			'page_title' => 'Reports',
			'active_nav' => 'reports',
			'crumbs' => array(array('label' => 'Reports')),
			'series' => $series,
			'totals' => $totals,
			'months' => $months,
			'period' => $period,
			'categories' => $this->Insights_model->expense_categories(
				$this->business_id, $period === 'this_month' ? date('Y-m-01') : NULL
			),
			'category_labels' => $this->category_labels(),
			'vat' => $this->Insights_model->vat_summary($this->business_id, $since, $until),
			'since' => $since,
			'until' => $until,
		));
	}

	/** Streams the month-by-month figures as CSV — same columns as the API's
	 * own export so a spreadsheet built on one still opens on the other. */
	public function export()
	{
		$months = (int) ($this->input->get('months') ?: 6);
		$months = in_array($months, array(3, 6, 12, 24), TRUE) ? $months : 6;
		$series = $this->Insights_model->monthly_series($this->business_id, $months);

		header('Content-Type: text/csv; charset=utf-8');
		header('Content-Disposition: attachment; filename="profit-summary.csv"');
		$out = fopen('php://output', 'w');
		fputcsv($out, array('Month', 'Revenue', 'Expenses', 'Profit'));
		foreach ($series as $month)
		{
			fputcsv($out, array($month['month'], $month['revenue'], $month['expenses'], $month['profit']));
		}
		fclose($out);
		exit;
	}

	private function valid_date($value)
	{
		if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', (string) $value))
		{
			return FALSE;
		}
		$parsed = DateTimeImmutable::createFromFormat('Y-m-d', $value);
		return $parsed && $parsed->format('Y-m-d') === $value;
	}

	private function category_labels()
	{
		return array(
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
}
