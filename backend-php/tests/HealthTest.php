<?php

final class HealthTest extends ApiTestCase {

	public function test_health_check_requires_no_auth()
	{
		$response = $this->request('GET', '/api/health/');
		$this->assertSame(200, $response['status']);
		$this->assertSame('ok', $response['body']['status']);
		$this->assertSame('ops-api', $response['body']['service']);
		$this->assertSame('ok', $response['body']['database']);
	}
}
