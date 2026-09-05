<?php

/** Cross-tenant IDOR guards — mirrors the Django backend's own such tests
 * (see backend/tests/test_*.py's cross-tenant assertions). Every
 * Business_owned_model method takes business_id explicitly and filters by
 * it, so a wrong-tenant id simply doesn't match any row — this proves that
 * holds for real over HTTP, not just by reading the model code. */
final class TenantScopingTest extends ApiTestCase {

	public function test_a_business_cannot_read_another_businesss_customer()
	{
		$owner_a = $this->register_test_business();
		$owner_b = $this->register_test_business();

		$create = $this->request('POST', '/api/customers/', array(
			'name' => 'Business A\'s Customer',
			'customer_type' => 'individual',
		), $owner_a['access']);
		$this->assertSame(201, $create['status']);
		$customer_id = $create['body']['id'];

		$as_b = $this->request('GET', '/api/customers/'.$customer_id.'/', null, $owner_b['access']);
		$this->assertSame(404, $as_b['status'], 'business B must not be able to read business A\'s customer');
	}

	public function test_a_business_cannot_update_another_businesss_customer()
	{
		$owner_a = $this->register_test_business();
		$owner_b = $this->register_test_business();

		$create = $this->request('POST', '/api/customers/', array('name' => 'Original name'), $owner_a['access']);
		$customer_id = $create['body']['id'];

		$as_b = $this->request('PUT', '/api/customers/'.$customer_id.'/', array('name' => 'Hijacked'), $owner_b['access']);
		$this->assertSame(404, $as_b['status']);

		$still_a = $this->request('GET', '/api/customers/'.$customer_id.'/', null, $owner_a['access']);
		$this->assertSame('Original name', $still_a['body']['name'], 'the record must be unchanged');
	}

	public function test_customer_list_only_shows_the_caller_own_business()
	{
		$owner_a = $this->register_test_business();
		$owner_b = $this->register_test_business();

		$this->request('POST', '/api/customers/', array('name' => 'A1'), $owner_a['access']);
		$this->request('POST', '/api/customers/', array('name' => 'B1'), $owner_b['access']);

		$list_a = $this->request('GET', '/api/customers/', null, $owner_a['access']);
		$names = array_column($list_a['body'], 'name');
		$this->assertContains('A1', $names);
		$this->assertNotContains('B1', $names);
	}
}
