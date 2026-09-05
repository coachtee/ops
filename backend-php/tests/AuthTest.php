<?php

final class AuthTest extends ApiTestCase {

	public function test_register_creates_business_and_issues_tokens()
	{
		$result = $this->register_test_business();
		$this->assertNotEmpty($result['access']);
		$this->assertNotEmpty($result['refresh']);
		$this->assertSame('PHPUnit Test Business', $result['business']['name']);
		$this->assertSame('WC', $result['business']['province']);
	}

	public function test_register_rejects_duplicate_email()
	{
		$first = $this->register_test_business();
		$response = $this->request('POST', '/api/auth/register/', array(
			'email' => $first['user']['email'],
			'password' => 'anotherpass123',
			'first_name' => 'Someone',
			'last_name' => 'Else',
			'business' => array('name' => 'Another Business', 'province' => 'GP'),
		));
		$this->assertSame(400, $response['status']);
		$this->assertArrayHasKey('email', $response['body']);
	}

	public function test_register_rejects_short_password()
	{
		$response = $this->request('POST', '/api/auth/register/', array(
			'email' => 'phpunit-'.bin2hex(random_bytes(6)).'@example.com',
			'password' => 'short',
			'first_name' => 'Test',
			'last_name' => 'User',
			'business' => array('name' => 'A Business'),
		));
		$this->assertSame(400, $response['status']);
		$this->assertArrayHasKey('password', $response['body']);
	}

	public function test_login_succeeds_with_correct_credentials()
	{
		$registered = $this->register_test_business();
		$response = $this->request('POST', '/api/auth/login/', array(
			'email' => $registered['user']['email'],
			'password' => 'testpass123',
		));
		$this->assertSame(200, $response['status']);
		$this->assertNotEmpty($response['body']['access']);
		$this->assertSame($registered['business']['id'], $response['body']['business']['id']);
	}

	public function test_login_rejects_wrong_password()
	{
		$registered = $this->register_test_business();
		$response = $this->request('POST', '/api/auth/login/', array(
			'email' => $registered['user']['email'],
			'password' => 'wrong-password',
		));
		$this->assertSame(401, $response['status']);
	}

	public function test_refresh_issues_a_new_access_token()
	{
		$registered = $this->register_test_business();
		$response = $this->request('POST', '/api/auth/refresh/', array('refresh' => $registered['refresh']));
		$this->assertSame(200, $response['status']);
		$this->assertNotEmpty($response['body']['access']);
	}

	public function test_refresh_rejects_an_access_token_used_as_refresh()
	{
		// The access and refresh tokens carry a 'type' claim precisely so
		// one can't be swapped for the other — see Auth_lib::verify().
		$registered = $this->register_test_business();
		$response = $this->request('POST', '/api/auth/refresh/', array('refresh' => $registered['access']));
		$this->assertSame(401, $response['status']);
	}

	public function test_protected_endpoint_rejects_missing_token()
	{
		$response = $this->request('GET', '/api/business/me/');
		$this->assertSame(401, $response['status']);
	}

	public function test_protected_endpoint_rejects_garbage_token()
	{
		$response = $this->request('GET', '/api/business/me/', null, 'not-a-real-token');
		$this->assertSame(401, $response['status']);
	}
}
