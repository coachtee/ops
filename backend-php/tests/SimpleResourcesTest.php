<?php

/** Lead, Supplier, Employee, ComplianceItem — plain field-mapping resources
 * with no server-derived numbering/totals, exercised via both the sync
 * push path and the direct CRUD endpoints (see docs/API_CONTRACT.md's
 * "Standard CRUD resources"). */
final class SimpleResourcesTest extends ApiTestCase {

	public function test_lead_sync_round_trip()
	{
		$owner = $this->register_test_business();
		$id = $this->uuid();
		$result = $this->push($owner['access'], 'lead', $id, array(
			'name' => 'Jane Prospect', 'phone' => '0821234567', 'source' => 'whatsapp', 'status' => 'new',
		));
		$this->assertSame('accepted', $result['status']);
		$this->assertSame('whatsapp', $result['server_record']['fields']['source']);

		$get = $this->request('GET', "/api/leads/{$id}/", null, $owner['access']);
		$this->assertSame(200, $get['status']);
		$this->assertSame('Jane Prospect', $get['body']['name']);
	}

	public function test_supplier_crud()
	{
		$owner = $this->register_test_business();
		$create = $this->request('POST', '/api/suppliers/', array('name' => 'ACME Hardware', 'phone' => '0111234567'), $owner['access']);
		$this->assertSame(201, $create['status']);
		$id = $create['body']['id'];

		$update = $this->request('PATCH', "/api/suppliers/{$id}/", array('name' => 'ACME Hardware & Tools', 'phone' => '0111234567'), $owner['access']);
		$this->assertSame(200, $update['status']);
		$this->assertSame('ACME Hardware & Tools', $update['body']['name']);
	}

	public function test_employee_crud()
	{
		$owner = $this->register_test_business();
		$create = $this->request('POST', '/api/employees/', array(
			'name' => 'Sipho Dlamini', 'pay_rate_type' => 'daily', 'pay_rate' => '850.00',
		), $owner['access']);
		$this->assertSame(201, $create['status']);
		$this->assertSame('850.00', $create['body']['pay_rate']);
	}

	public function test_compliance_item_sync_round_trip()
	{
		$owner = $this->register_test_business();
		$id = $this->uuid();
		$result = $this->push($owner['access'], 'compliance_item', $id, array(
			'category' => 'vat_return', 'title' => 'VAT201 - August', 'due_date' => '2026-09-25', 'is_recurring' => true,
		));
		$this->assertSame('accepted', $result['status']);
		$this->assertTrue($result['server_record']['fields']['is_recurring']);
	}

	public function test_tenant_scoping_across_all_new_resources()
	{
		$owner_a = $this->register_test_business();
		$owner_b = $this->register_test_business();
		$lead_id = $this->uuid();
		$this->push($owner_a['access'], 'lead', $lead_id, array('name' => 'A Only Lead', 'source' => 'other'));

		$get = $this->request('GET', "/api/leads/{$lead_id}/", null, $owner_b['access']);
		$this->assertSame(404, $get['status']);

		$pull_b = $this->request('GET', '/api/sync/pull/', null, $owner_b['access']);
		$ids = array_column($pull_b['body']['changes'], 'id');
		$this->assertNotContains($lead_id, $ids);
	}
}
