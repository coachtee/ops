<?php

/** GET /api/reports/* — see docs/API_CONTRACT.md's "Reports" section and
 * backend/reports/views.py (the definitive spec this ports). Read-only
 * aggregations over Payment/Expense/Invoice, cash-basis revenue. */
final class ReportsTest extends ApiTestCase {

	private function push_customer($token)
	{
		$id = $this->uuid();
		$this->push($token, 'customer', $id, array('name' => 'Reports Test Customer', 'customer_type' => 'individual'));
		return $id;
	}

	public function test_profit_summary_default_shape_and_zero_filled_months()
	{
		$owner = $this->register_test_business();
		$response = $this->request('GET', '/api/reports/profit-summary/', null, $owner['access']);

		$this->assertSame(200, $response['status']);
		$this->assertCount(6, $response['body']['months'], 'default window is 6 months');
		foreach ($response['body']['months'] as $row)
		{
			$this->assertMatchesRegularExpression('/^\d{4}-\d{2}$/', $row['month']);
			$this->assertMatchesRegularExpression('/^\d+\.\d{2}$/', $row['revenue']);
		}
	}

	public function test_profit_summary_reflects_this_months_payments_and_expenses()
	{
		$owner = $this->register_test_business();
		$customer_id = $this->push_customer($owner['access']);
		$today = gmdate('Y-m-d');
		$this_month = gmdate('Y-m');

		$this->push($owner['access'], 'payment', $this->uuid(), array(
			'customer_id' => $customer_id, 'amount' => '500.00', 'method' => 'cash', 'paid_date' => $today,
		));
		$this->push($owner['access'], 'expense', $this->uuid(), array(
			'category' => 'other', 'amount' => '200.00', 'is_vat_applicable' => false, 'date' => $today,
		));

		$response = $this->request('GET', '/api/reports/profit-summary/?months=1', null, $owner['access']);
		$row = $response['body']['months'][0];
		$this->assertSame($this_month, $row['month']);
		$this->assertSame('500.00', $row['revenue']);
		$this->assertSame('200.00', $row['expenses']);
		$this->assertSame('300.00', $row['profit']);
	}

	public function test_profit_summary_csv_export()
	{
		$owner = $this->register_test_business();
		$ch = curl_init($this->base_url().'/api/reports/profit-summary/?export=csv');
		curl_setopt_array($ch, array(
			CURLOPT_HTTPHEADER => array('Authorization: Bearer '.$owner['access']),
			CURLOPT_RETURNTRANSFER => true,
		));
		$raw = curl_exec($ch);
		curl_close($ch);
		$this->assertStringStartsWith('Month,Revenue,Expenses,Profit', trim($raw));
	}

	public function test_expense_categories_sorted_biggest_first_and_zero_categories_omitted()
	{
		$owner = $this->register_test_business();
		$today = gmdate('Y-m-d');
		$this->push($owner['access'], 'expense', $this->uuid(), array('category' => 'fuel_travel', 'amount' => '100.00', 'is_vat_applicable' => false, 'date' => $today));
		$this->push($owner['access'], 'expense', $this->uuid(), array('category' => 'rent', 'amount' => '5000.00', 'is_vat_applicable' => false, 'date' => $today));

		$response = $this->request('GET', '/api/reports/expense-categories/', null, $owner['access']);
		$this->assertSame(200, $response['status']);
		$categories = $response['body']['categories'];
		$this->assertSame('rent', $categories[0]['category']);
		$this->assertSame('Rent', $categories[0]['label']);
		$this->assertSame('5000.00', $categories[0]['total']);

		$found = array_column($categories, null, 'category');
		$this->assertArrayNotHasKey('bank_charges', $found, 'a category with nothing spent must be omitted');
	}

	public function test_vat_summary_excludes_draft_and_cancelled_invoices()
	{
		$owner = $this->register_test_business();
		$customer_id = $this->push_customer($owner['access']);
		$today = gmdate('Y-m-d');

		$sent_invoice = $this->uuid();
		$this->push($owner['access'], 'invoice', $sent_invoice, array('customer_id' => $customer_id, 'status' => 'sent', 'issue_date' => $today, 'is_vat_applicable' => true));
		$this->push($owner['access'], 'invoice_line_item', $this->uuid(), array('invoice_id' => $sent_invoice, 'description' => 'Work', 'quantity' => '1', 'unit_price' => '1000.00'));

		$draft_invoice = $this->uuid();
		$this->push($owner['access'], 'invoice', $draft_invoice, array('customer_id' => $customer_id, 'status' => 'draft', 'issue_date' => $today, 'is_vat_applicable' => true));
		$this->push($owner['access'], 'invoice_line_item', $this->uuid(), array('invoice_id' => $draft_invoice, 'description' => 'Work', 'quantity' => '1', 'unit_price' => '9999.00'));

		$this->push($owner['access'], 'expense', $this->uuid(), array('category' => 'other', 'amount' => '115.00', 'is_vat_applicable' => true, 'date' => $today));

		$response = $this->request('GET', '/api/reports/vat-summary/', null, $owner['access']);
		$this->assertSame(200, $response['status']);
		$this->assertSame('150.00', $response['body']['vat_collected'], 'the draft invoice must be excluded');
		$this->assertSame('15.00', $response['body']['vat_paid']);
		$this->assertSame('135.00', $response['body']['net_vat_position']);
	}
}
