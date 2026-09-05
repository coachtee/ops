<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * GET /api/reports/profit-summary/, /expense-categories/, /vat-summary/ —
 * ports backend/reports/views.py exactly (see that file for the definitive
 * spec this mirrors). Read-only, computed-on-demand aggregations over
 * Payment/Expense/Invoice — not new stored tables, not synced. "Revenue" is
 * cash-basis (actual payments received via paid_date), matching the Home
 * dashboard's "money in this month" definition — one financial vocabulary
 * throughout the API, not an accrual figure based on invoice totals.
 */
class Reports extends Api_Controller {

	const MAX_MONTHS = 24;
	const DEFAULT_MONTHS = 6;

	const CATEGORY_LABELS = array(
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

	private function utc_now()
	{
		return new DateTimeImmutable('now', new DateTimeZone('UTC'));
	}

	private function month_start_of($date)
	{
		return new DateTimeImmutable($date->format('Y-m-01'), new DateTimeZone('UTC'));
	}

	/** GET ?months=6[&export=csv] */
	public function profit_summary_get()
	{
		$months = (int) ($this->input->get('months') ?: self::DEFAULT_MONTHS);
		$months = max(1, min($months, self::MAX_MONTHS));

		$current_month_start = $this->month_start_of($this->utc_now());
		$month_starts = array();
		for ($offset = $months - 1; $offset >= 0; $offset--)
		{
			$month_starts[] = $current_month_start->modify("-{$offset} months");
		}
		$range_start = $month_starts[0]->format('Y-m-d');

		$revenue_by_month = array();
		$payment_rows = $this->db->select("DATE_FORMAT(paid_date, '%Y-%m-01') AS month, SUM(amount) AS total")
			->where('business_id', $this->business_id)
			->where('deleted_at', NULL)
			->where('paid_date >=', $range_start)
			->group_by('month')
			->get('payments')->result_array();
		foreach ($payment_rows as $row)
		{
			$revenue_by_month[$row['month']] = $row['total'];
		}

		$expenses_by_month = array();
		$expense_rows = $this->db->select("DATE_FORMAT(date, '%Y-%m-01') AS month, SUM(amount) AS total")
			->where('business_id', $this->business_id)
			->where('deleted_at', NULL)
			->where('date >=', $range_start)
			->group_by('month')
			->get('expenses')->result_array();
		foreach ($expense_rows as $row)
		{
			$expenses_by_month[$row['month']] = $row['total'];
		}

		$rows = array();
		foreach ($month_starts as $month_start)
		{
			$key = $month_start->format('Y-m-d');
			$revenue = money_quantize($revenue_by_month[$key] ?? 0);
			$expenses = money_quantize($expenses_by_month[$key] ?? 0);
			$rows[] = array(
				'month' => $month_start->format('Y-m'),
				'revenue' => $revenue,
				'expenses' => $expenses,
				'profit' => money_quantize((float) $revenue - (float) $expenses),
			);
		}

		if ($this->input->get('export') === 'csv')
		{
			header('Content-Type: text/csv');
			header('Content-Disposition: attachment; filename="profit-summary.csv"');
			$out = fopen('php://output', 'w');
			fputcsv($out, array('Month', 'Revenue', 'Expenses', 'Profit'));
			foreach ($rows as $row)
			{
				fputcsv($out, array($row['month'], $row['revenue'], $row['expenses'], $row['profit']));
			}
			fclose($out);
			exit;
		}

		$this->response(array('months' => $rows), 200);
	}

	/** GET ?period=this_month|all_time — defaults to, and silently falls
	 * back to, this_month for any other value. Categories with nothing
	 * spent are omitted, not listed at zero. */
	public function expense_categories_get()
	{
		$period = $this->input->get('period');
		if (!in_array($period, array('this_month', 'all_time'), TRUE))
		{
			$period = 'this_month';
		}

		$this->db->select('category, SUM(amount) AS total')
			->where('business_id', $this->business_id)
			->where('deleted_at', NULL);
		if ($period === 'this_month')
		{
			$this->db->where('date >=', $this->month_start_of($this->utc_now())->format('Y-m-d'));
		}
		$totals = $this->db->group_by('category')->order_by('total', 'DESC')->get('expenses')->result_array();

		$categories = array();
		foreach ($totals as $row)
		{
			if ((float) $row['total'] == 0.0)
			{
				continue;
			}
			$categories[] = array(
				'category' => $row['category'],
				'label' => self::CATEGORY_LABELS[$row['category']] ?? $row['category'],
				'total' => money_quantize($row['total']),
			);
		}

		$this->response(array('period' => $period, 'categories' => $categories), 200);
	}

	/** GET ?since=YYYY-MM-DD&until=YYYY-MM-DD — defaults to the current
	 * calendar month. Informational only — never files/submits anything. */
	public function vat_summary_get()
	{
		$today = $this->utc_now();
		$since = $this->_valid_date($this->input->get('since')) ?? $this->month_start_of($today)->format('Y-m-d');
		$until = $this->_valid_date($this->input->get('until')) ?? $today->format('Y-m-d');

		$vat_collected_row = $this->db->select('COALESCE(SUM(vat_amount), 0) AS total')
			->where('business_id', $this->business_id)
			->where('deleted_at', NULL)
			->where('issue_date >=', $since)
			->where('issue_date <=', $until)
			->where_not_in('status', array('draft', 'cancelled'))
			->get('invoices')->row_array();
		$vat_paid_row = $this->db->select('COALESCE(SUM(vat_amount), 0) AS total')
			->where('business_id', $this->business_id)
			->where('deleted_at', NULL)
			->where('date >=', $since)
			->where('date <=', $until)
			->get('expenses')->row_array();

		$vat_collected = money_quantize($vat_collected_row['total']);
		$vat_paid = money_quantize($vat_paid_row['total']);

		$this->response(array(
			'since' => $since,
			'until' => $until,
			'vat_collected' => $vat_collected,
			'vat_paid' => $vat_paid,
			'net_vat_position' => money_quantize((float) $vat_collected - (float) $vat_paid),
		), 200);
	}

	private function _valid_date($value)
	{
		if (!$value || !preg_match('/^\d{4}-\d{2}-\d{2}$/', $value))
		{
			return NULL;
		}
		$parsed = DateTimeImmutable::createFromFormat('Y-m-d', $value);
		return ($parsed && $parsed->format('Y-m-d') === $value) ? $value : NULL;
	}
}
