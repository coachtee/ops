<?php

/** Expense (VAT-inclusive extraction + validation) and Payslip (net_pay +
 * validation) via sync push — see docs/API_CONTRACT.md's validation rules
 * for both. */
final class ExpensePayslipValidationTest extends ApiTestCase {

	public function test_expense_vat_is_extracted_from_the_inclusive_amount()
	{
		$owner = $this->register_test_business();
		$id = $this->uuid();

		$result = $this->push($owner['access'], 'expense', $id, array(
			'category' => 'fuel_travel', 'amount' => '115.00', 'is_vat_applicable' => true, 'date' => '2026-09-01',
		));

		$this->assertSame('accepted', $result['status']);
		$this->assertSame('15.00', $result['server_record']['fields']['vat_amount']);
		$this->assertSame('115.00', $result['server_record']['fields']['amount'], 'amount must never be added to');
	}

	public function test_expense_without_vat_reports_zero_vat()
	{
		$owner = $this->register_test_business();
		$result = $this->push($owner['access'], 'expense', $this->uuid(), array(
			'category' => 'bank_charges', 'amount' => '50.00', 'is_vat_applicable' => false, 'date' => '2026-09-01',
		));
		$this->assertSame('0.00', $result['server_record']['fields']['vat_amount']);
	}

	public function test_expense_amount_must_be_greater_than_zero()
	{
		$owner = $this->register_test_business();
		$result = $this->push($owner['access'], 'expense', $this->uuid(), array(
			'category' => 'other', 'amount' => '0.00', 'is_vat_applicable' => false, 'date' => '2026-09-01',
		));
		$this->assertSame('error', $result['status']);
		$this->assertArrayHasKey('amount', $result['errors']);
	}

	public function test_expense_date_cannot_be_more_than_one_day_in_the_future()
	{
		$owner = $this->register_test_business();
		$far_future = gmdate('Y-m-d', strtotime('+30 days'));
		$result = $this->push($owner['access'], 'expense', $this->uuid(), array(
			'category' => 'other', 'amount' => '50.00', 'is_vat_applicable' => false, 'date' => $far_future,
		));
		$this->assertSame('error', $result['status']);
		$this->assertArrayHasKey('date', $result['errors']);
	}

	private function push_employee($token)
	{
		$id = $this->uuid();
		$this->push($token, 'employee', $id, array('name' => 'Payslip Test Employee', 'pay_rate_type' => 'monthly'));
		return $id;
	}

	public function test_payslip_net_pay_is_gross_minus_deductions()
	{
		$owner = $this->register_test_business();
		$employee_id = $this->push_employee($owner['access']);

		$result = $this->push($owner['access'], 'payslip', $this->uuid(), array(
			'employee_id' => $employee_id, 'period_start' => '2026-08-01', 'period_end' => '2026-08-31',
			'gross_pay' => '10000.00', 'deductions' => '1500.00',
		));

		$this->assertSame('accepted', $result['status']);
		$this->assertSame('8500.00', $result['server_record']['fields']['net_pay']);
	}

	public function test_payslip_deductions_cannot_exceed_gross_pay()
	{
		$owner = $this->register_test_business();
		$employee_id = $this->push_employee($owner['access']);

		$result = $this->push($owner['access'], 'payslip', $this->uuid(), array(
			'employee_id' => $employee_id, 'period_start' => '2026-08-01', 'period_end' => '2026-08-31',
			'gross_pay' => '1000.00', 'deductions' => '1500.00',
		));

		$this->assertSame('error', $result['status']);
		$this->assertArrayHasKey('deductions', $result['errors']);
	}

	public function test_payslip_period_end_cannot_be_before_period_start()
	{
		$owner = $this->register_test_business();
		$employee_id = $this->push_employee($owner['access']);

		$result = $this->push($owner['access'], 'payslip', $this->uuid(), array(
			'employee_id' => $employee_id, 'period_start' => '2026-08-31', 'period_end' => '2026-08-01',
			'gross_pay' => '1000.00', 'deductions' => '0.00',
		));

		$this->assertSame('error', $result['status']);
		$this->assertArrayHasKey('period_end', $result['errors']);
	}
}
