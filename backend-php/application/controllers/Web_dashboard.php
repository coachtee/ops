<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * /dashboard — the one screen an owner checks first: what came in this
 * month, what went out, what's still owed to them, and what needs doing
 * today. Every figure uses the same definitions as the Reports API (see
 * Insights_model) so nothing on this page can disagree with an export.
 */
class Web_dashboard extends Web_Controller {

	public function __construct()
	{
		parent::__construct();
		$this->load->model('Insights_model');
	}

	public function index()
	{
		$series = $this->Insights_model->monthly_series($this->business_id, 6);
		$this_month = end($series);
		$prev_month = count($series) > 1 ? $series[count($series) - 2] : NULL;
		reset($series);

		$outstanding = $this->db->select('COALESCE(SUM(total - amount_paid), 0) AS total')
			->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->where_not_in('status', array('draft', 'paid', 'cancelled'))
			->get('invoices')->row_array();

		$recent_invoices = $this->db->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->where_not_in('status', array('draft'))
			->order_by('created_at', 'DESC')->limit(6)->get('invoices')->result_array();
		$customer_names = $this->name_map('customers', array_column($recent_invoices, 'customer_id'));
		foreach ($recent_invoices as &$invoice)
		{
			$invoice['customer_name'] = $customer_names[$invoice['customer_id']] ?? '—';
		}
		unset($invoice);

		$upcoming_visits = $this->db->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->where('scheduled_date >=', date('Y-m-d'))
			->where_in('status', array('scheduled', 'en_route', 'in_progress'))
			->order_by('scheduled_date', 'ASC')->limit(5)->get('visits')->result_array();
		$job_titles = $this->name_map('jobs', array_column($upcoming_visits, 'job_id'), 'title');
		foreach ($upcoming_visits as &$visit)
		{
			$visit['job_title'] = $job_titles[$visit['job_id']] ?? '—';
		}
		unset($visit);

		$recent_leads = $this->db->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->order_by('created_at', 'DESC')->limit(5)->get('leads')->result_array();

		$categories = $this->Insights_model->expense_categories($this->business_id, date('Y-m-01'));
		$this->load->model('Expense_model');

		$this->render('dashboard', array(
			'page_title' => 'Dashboard',
			'active_nav' => 'dashboard',
			'series' => $series,
			'this_month' => $this_month,
			'prev_month' => $prev_month,
			'outstanding' => $outstanding['total'],
			'attention' => $this->Insights_model->attention($this->business_id),
			'recent_invoices' => $recent_invoices,
			'upcoming_visits' => $upcoming_visits,
			'recent_leads' => $recent_leads,
			'categories' => array_slice($categories, 0, 5),
			'category_labels' => $this->category_labels(),
			'has_any_data' => $this->has_any_data(),
		));
	}

	/** A brand-new business should get a "here's how this fills up" screen,
	 * not six empty widgets that look broken. */
	private function has_any_data()
	{
		foreach (array('customers', 'leads', 'invoices', 'expenses', 'jobs') as $table)
		{
			$exists = $this->db->where('business_id', $this->business_id)->where('deleted_at', NULL)
				->count_all_results($table);
			if ($exists > 0)
			{
				return TRUE;
			}
		}
		return FALSE;
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
