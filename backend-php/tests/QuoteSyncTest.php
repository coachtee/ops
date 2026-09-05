<?php

/** Quote + QuoteLineItem via the sync push path — number assignment
 * (Q-0001 on first successful sync, never reassigned) and server-side
 * totals recompute from line items, mirroring backend/sales/services.py's
 * recompute_quote_totals()/assign_quote_number_if_needed() exactly. */
final class QuoteSyncTest extends ApiTestCase {

	private function push_customer($token)
	{
		$id = $this->uuid();
		$this->push($token, 'customer', $id, array('name' => 'Quote Test Customer', 'customer_type' => 'individual'));
		return $id;
	}

	public function test_first_successful_sync_assigns_a_sequential_number()
	{
		$owner = $this->register_test_business();
		$customer_id = $this->push_customer($owner['access']);
		$quote_id = $this->uuid();

		$result = $this->push($owner['access'], 'quote', $quote_id, array(
			'customer_id' => $customer_id, 'status' => 'draft', 'issue_date' => '2026-09-01',
			'is_vat_applicable' => true, 'discount_amount' => '0.00',
		));

		$this->assertSame('accepted', $result['status']);
		$this->assertSame('Q-0001', $result['server_record']['fields']['number']);
	}

	public function test_number_is_not_reassigned_on_a_later_update()
	{
		$owner = $this->register_test_business();
		$customer_id = $this->push_customer($owner['access']);
		$quote_id = $this->uuid();

		$first = $this->push($owner['access'], 'quote', $quote_id, array(
			'customer_id' => $customer_id, 'status' => 'draft', 'issue_date' => '2026-09-01', 'is_vat_applicable' => true,
		));
		$number = $first['server_record']['fields']['number'];

		$second = $this->push($owner['access'], 'quote', $quote_id, array(
			'customer_id' => $customer_id, 'status' => 'sent', 'issue_date' => '2026-09-01', 'is_vat_applicable' => true,
		), null, '2027-01-01T00:00:00.000000Z');

		$this->assertSame($number, $second['server_record']['fields']['number']);
	}

	public function test_totals_are_recomputed_from_line_items_never_trusted_from_client()
	{
		$owner = $this->register_test_business();
		$customer_id = $this->push_customer($owner['access']);
		$quote_id = $this->uuid();

		$this->push($owner['access'], 'quote', $quote_id, array(
			'customer_id' => $customer_id, 'status' => 'draft', 'issue_date' => '2026-09-01',
			'is_vat_applicable' => true, 'discount_amount' => '0.00',
			// A client sending a bogus total must never survive — it's
			// always recomputed server-side from line items.
			'subtotal' => '999999.00', 'vat_amount' => '999999.00', 'total' => '999999.00',
		));

		$li1 = $this->uuid();
		$li2 = $this->uuid();
		$response = $this->request('POST', '/api/sync/push/', array('changes' => array(
			array('model' => 'quote_line_item', 'id' => $li1, 'updated_at' => $this->iso_now(), 'deleted_at' => null,
				'fields' => array('quote_id' => $quote_id, 'description' => 'Labour', 'quantity' => '2', 'unit_price' => '950.005')),
			array('model' => 'quote_line_item', 'id' => $li2, 'updated_at' => $this->iso_now(), 'deleted_at' => null,
				'fields' => array('quote_id' => $quote_id, 'description' => 'Parts', 'quantity' => '1', 'unit_price' => '100.00')),
		)), $owner['access']);

		$results = $response['body']['results'];
		$this->assertSame('1900.01', $results[0]['server_record']['fields']['line_total']);
		$this->assertSame('100.00', $results[1]['server_record']['fields']['line_total']);

		$quote = $this->request('GET', "/api/quotes/{$quote_id}/", null, $owner['access'])['body'];
		$this->assertSame('2000.01', $quote['subtotal']);
		$this->assertSame('300.00', $quote['vat_amount']);
		$this->assertSame('2300.01', $quote['total']);
	}

	public function test_deleting_a_line_item_recomputes_the_quote_back_down()
	{
		$owner = $this->register_test_business();
		$customer_id = $this->push_customer($owner['access']);
		$quote_id = $this->uuid();
		$this->push($owner['access'], 'quote', $quote_id, array(
			'customer_id' => $customer_id, 'status' => 'draft', 'issue_date' => '2026-09-01', 'is_vat_applicable' => false,
		));
		$li1 = $this->uuid();
		$li2 = $this->uuid();
		$this->push($owner['access'], 'quote_line_item', $li1, array('quote_id' => $quote_id, 'description' => 'A', 'quantity' => '1', 'unit_price' => '100.00'));
		$this->push($owner['access'], 'quote_line_item', $li2, array('quote_id' => $quote_id, 'description' => 'B', 'quantity' => '1', 'unit_price' => '50.00'));

		$quote = $this->request('GET', "/api/quotes/{$quote_id}/", null, $owner['access'])['body'];
		$this->assertSame('150.00', $quote['total']);

		$this->push($owner['access'], 'quote_line_item', $li2, array('quote_id' => $quote_id, 'description' => 'B', 'quantity' => '1', 'unit_price' => '50.00'), $this->iso_now(), '2027-01-01T00:00:00.000000Z');

		$quote = $this->request('GET', "/api/quotes/{$quote_id}/", null, $owner['access'])['body'];
		$this->assertSame('100.00', $quote['total'], 'a soft-deleted line item must be excluded from the recomputed total');
	}

	public function test_document_sequences_are_scoped_per_business()
	{
		$owner_a = $this->register_test_business();
		$owner_b = $this->register_test_business();
		$customer_a = $this->push_customer($owner_a['access']);
		$customer_b = $this->push_customer($owner_b['access']);

		$result_a = $this->push($owner_a['access'], 'quote', $this->uuid(), array(
			'customer_id' => $customer_a, 'status' => 'draft', 'issue_date' => '2026-09-01', 'is_vat_applicable' => true,
		));
		$result_b = $this->push($owner_b['access'], 'quote', $this->uuid(), array(
			'customer_id' => $customer_b, 'status' => 'draft', 'issue_date' => '2026-09-01', 'is_vat_applicable' => true,
		));

		$this->assertSame('Q-0001', $result_a['server_record']['fields']['number'], 'a brand new business must start its own numbering at 1');
		$this->assertSame('Q-0001', $result_b['server_record']['fields']['number'], 'each business has its own independent sequence');
	}
}
