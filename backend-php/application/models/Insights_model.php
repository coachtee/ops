<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * Read-only aggregations for the web panel's dashboard and reports pages.
 *
 * Deliberately the same definitions the JSON Reports API uses (see
 * application/controllers/Reports.php), so a figure on the dashboard and
 * the same figure in an exported CSV can never disagree: "revenue" is
 * cash-basis (payments actually received, by paid_date), never invoice
 * totals; expenses are the VAT-inclusive amounts paid; profit is simply
 * revenue minus expenses over the same window.
 */
class Insights_model extends CI_Model {

	/** Calendar months, oldest first, always fully populated — a month with
	 * no activity is 0.00 across the board rather than a gap in the chart. */
	public function monthly_series($business_id, $months = 6)
	{
		$months = max(1, min((int) $months, 24));
		$start = new DateTimeImmutable(date('Y-m-01'), new DateTimeZone('UTC'));

		$series = array();
		for ($offset = $months - 1; $offset >= 0; $offset--)
		{
			$month = $start->modify("-{$offset} months");
			$series[$month->format('Y-m')] = array(
				'month' => $month->format('Y-m'),
				'revenue' => '0.00',
				'expenses' => '0.00',
				'profit' => '0.00',
			);
		}
		$range_start = $start->modify('-'.($months - 1).' months')->format('Y-m-d');

		$revenue = $this->db->select("DATE_FORMAT(paid_date, '%Y-%m') AS ym, SUM(amount) AS total")
			->where('business_id', $business_id)->where('deleted_at', NULL)
			->where('paid_date >=', $range_start)
			->group_by('ym')->get('payments')->result_array();
		foreach ($revenue as $row)
		{
			if (isset($series[$row['ym']]))
			{
				$series[$row['ym']]['revenue'] = money_quantize($row['total']);
			}
		}

		$expenses = $this->db->select("DATE_FORMAT(date, '%Y-%m') AS ym, SUM(amount) AS total")
			->where('business_id', $business_id)->where('deleted_at', NULL)
			->where('date >=', $range_start)
			->group_by('ym')->get('expenses')->result_array();
		foreach ($expenses as $row)
		{
			if (isset($series[$row['ym']]))
			{
				$series[$row['ym']]['expenses'] = money_quantize($row['total']);
			}
		}

		foreach ($series as &$month)
		{
			$month['profit'] = money_quantize((float) $month['revenue'] - (float) $month['expenses']);
		}

		return array_values($series);
	}

	/** Spend per category, biggest first. Categories with nothing spent are
	 * omitted rather than listed at zero. */
	public function expense_categories($business_id, $since = NULL)
	{
		$this->db->select('category, SUM(amount) AS total')
			->where('business_id', $business_id)->where('deleted_at', NULL);
		if ($since)
		{
			$this->db->where('date >=', $since);
		}
		$rows = $this->db->group_by('category')->order_by('total', 'DESC')->get('expenses')->result_array();

		$out = array();
		foreach ($rows as $row)
		{
			if ((float) $row['total'] == 0.0)
			{
				continue;
			}
			$out[] = array('category' => $row['category'], 'total' => money_quantize($row['total']));
		}
		return $out;
	}

	/** Output VAT (on invoices actually issued) vs input VAT (on expenses).
	 * Informational only — this product never files anything with SARS. */
	public function vat_summary($business_id, $since, $until)
	{
		$collected = $this->db->select('COALESCE(SUM(vat_amount), 0) AS total')
			->where('business_id', $business_id)->where('deleted_at', NULL)
			->where('issue_date >=', $since)->where('issue_date <=', $until)
			->where_not_in('status', array('draft', 'cancelled'))
			->get('invoices')->row_array();

		$paid = $this->db->select('COALESCE(SUM(vat_amount), 0) AS total')
			->where('business_id', $business_id)->where('deleted_at', NULL)
			->where('date >=', $since)->where('date <=', $until)
			->get('expenses')->row_array();

		return array(
			'vat_collected' => money_quantize($collected['total']),
			'vat_paid' => money_quantize($paid['total']),
			'net_vat_position' => money_quantize((float) $collected['total'] - (float) $paid['total']),
		);
	}

	/** The "what needs me today" counts behind the dashboard's attention
	 * card — each one is a thing the owner can act on, not a vanity metric. */
	public function attention($business_id)
	{
		$today = date('Y-m-d');

		$overdue = $this->db->select('COUNT(*) AS n, COALESCE(SUM(total - amount_paid), 0) AS amount')
			->where('business_id', $business_id)->where('deleted_at', NULL)
			->where_not_in('status', array('draft', 'paid', 'cancelled'))
			->where('due_date <', $today)->where('due_date IS NOT NULL', NULL, FALSE)
			->get('invoices')->row_array();

		return array(
			'overdue_invoices' => (int) $overdue['n'],
			'overdue_amount' => money_quantize($overdue['amount']),
			'leads_to_chase' => $this->db->where('business_id', $business_id)->where('deleted_at', NULL)
				->where_in('status', array('new', 'contacted'))->count_all_results('leads'),
			'quotes_awaiting' => $this->db->where('business_id', $business_id)->where('deleted_at', NULL)
				->where('status', 'sent')->count_all_results('quotes'),
			'visits_follow_up' => $this->db->where('business_id', $business_id)->where('deleted_at', NULL)
				->where('status', 'needs_follow_up')->count_all_results('visits'),
			'compliance_overdue' => $this->db->where('business_id', $business_id)->where('deleted_at', NULL)
				->where('completed_date', NULL)->where('due_date <', $today)
				->count_all_results('compliance_items'),
		);
	}
}
