<?php

/** Invoice + InvoiceLineItem + Payment via the sync push path — number
 * assignment, totals recompute, and the payment-state derivation state
 * machine (draft/sent -> partially_paid -> paid, and back down if a
 * payment is reversed), mirroring backend/finance/services.py's
 * recompute_invoice_totals()/recompute_invoice_payment_state() exactly. */
final class InvoicePaymentSyncTest extends ApiTestCase {

	private function push_customer($token)
	{
		$id = $this->uuid();
		$this->push($token, 'customer', $id, array('name' => 'Invoice Test Customer', 'customer_type' => 'individual'));
		return $id;
	}

	private function push_invoice($token, $customer_id, $unit_price = '1000.00')
	{
		$invoice_id = $this->uuid();
		$this->push($token, 'invoice', $invoice_id, array(
			'customer_id' => $customer_id, 'status' => 'sent', 'issue_date' => '2026-09-01',
			'is_vat_applicable' => true, 'discount_amount' => '0.00',
		));
		$this->push($token, 'invoice_line_item', $this->uuid(), array(
			'invoice_id' => $invoice_id, 'description' => 'Work', 'quantity' => '1', 'unit_price' => $unit_price,
		));
		return $invoice_id;
	}

	private function get_invoice($token, $invoice_id)
	{
		return $this->request('GET', "/api/invoices/{$invoice_id}/", null, $token)['body'];
	}

	public function test_first_successful_sync_assigns_invoice_number_and_recomputes_totals()
	{
		$owner = $this->register_test_business();
		$customer_id = $this->push_customer($owner['access']);
		$invoice_id = $this->push_invoice($owner['access'], $customer_id, '1000.00');

		$invoice = $this->get_invoice($owner['access'], $invoice_id);
		$this->assertSame('INV-0001', $invoice['number']);
		$this->assertSame('1000.00', $invoice['subtotal']);
		$this->assertSame('150.00', $invoice['vat_amount']);
		$this->assertSame('1150.00', $invoice['total']);
		$this->assertSame('0.00', $invoice['amount_paid']);
		$this->assertSame('sent', $invoice['status']);
	}

	public function test_a_partial_payment_moves_status_to_partially_paid()
	{
		$owner = $this->register_test_business();
		$customer_id = $this->push_customer($owner['access']);
		$invoice_id = $this->push_invoice($owner['access'], $customer_id, '1000.00');

		$this->push($owner['access'], 'payment', $this->uuid(), array(
			'customer_id' => $customer_id, 'invoice_id' => $invoice_id, 'amount' => '500.00',
			'method' => 'eft', 'paid_date' => '2026-09-02',
		));

		$invoice = $this->get_invoice($owner['access'], $invoice_id);
		$this->assertSame('500.00', $invoice['amount_paid']);
		$this->assertSame('partially_paid', $invoice['status']);
	}

	public function test_a_full_payment_moves_status_to_paid()
	{
		$owner = $this->register_test_business();
		$customer_id = $this->push_customer($owner['access']);
		$invoice_id = $this->push_invoice($owner['access'], $customer_id, '1000.00');

		$this->push($owner['access'], 'payment', $this->uuid(), array(
			'customer_id' => $customer_id, 'invoice_id' => $invoice_id, 'amount' => '1150.00',
			'method' => 'eft', 'paid_date' => '2026-09-02',
		));

		$invoice = $this->get_invoice($owner['access'], $invoice_id);
		$this->assertSame('1150.00', $invoice['amount_paid']);
		$this->assertSame('paid', $invoice['status']);
	}

	public function test_reversing_a_payment_pulls_the_invoice_back_out_of_paid()
	{
		$owner = $this->register_test_business();
		$customer_id = $this->push_customer($owner['access']);
		$invoice_id = $this->push_invoice($owner['access'], $customer_id, '1000.00');
		$payment_id = $this->uuid();

		$this->push($owner['access'], 'payment', $payment_id, array(
			'customer_id' => $customer_id, 'invoice_id' => $invoice_id, 'amount' => '1150.00',
			'method' => 'eft', 'paid_date' => '2026-09-02',
		));
		$this->assertSame('paid', $this->get_invoice($owner['access'], $invoice_id)['status']);

		// Soft-delete the payment (a correction/reversal) — must never
		// leave a false "Paid" sitting in the owner's outstanding picture.
		$this->push($owner['access'], 'payment', $payment_id, array(
			'customer_id' => $customer_id, 'invoice_id' => $invoice_id, 'amount' => '1150.00',
			'method' => 'eft', 'paid_date' => '2026-09-02',
		), '2027-01-01T00:00:00.000000Z', '2027-01-01T00:00:00.000000Z');

		$invoice = $this->get_invoice($owner['access'], $invoice_id);
		$this->assertSame('0.00', $invoice['amount_paid']);
		$this->assertSame('sent', $invoice['status'], 'reversing the only payment must fall back to sent, not stay paid');
	}

	public function test_cancelled_invoices_are_never_touched_by_payment_state()
	{
		$owner = $this->register_test_business();
		$customer_id = $this->push_customer($owner['access']);
		$invoice_id = $this->push_invoice($owner['access'], $customer_id, '1000.00');

		$this->push($owner['access'], 'invoice', $invoice_id, array(
			'customer_id' => $customer_id, 'status' => 'cancelled', 'issue_date' => '2026-09-01', 'is_vat_applicable' => true,
		), null, '2027-01-01T00:00:00.000000Z');

		$this->push($owner['access'], 'payment', $this->uuid(), array(
			'customer_id' => $customer_id, 'invoice_id' => $invoice_id, 'amount' => '1150.00',
			'method' => 'eft', 'paid_date' => '2026-09-03',
		));

		$invoice = $this->get_invoice($owner['access'], $invoice_id);
		$this->assertSame('cancelled', $invoice['status'], 'a cancelled invoice must never be flipped back to paid/partially_paid');
	}
}
