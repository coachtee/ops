<?php

use PHPUnit\Framework\TestCase;

abstract class ApiTestCase extends TestCase {

	protected function base_url()
	{
		return getenv('OPS_TEST_BASE_URL') ?: 'http://127.0.0.1:8080';
	}

	/** Returns array('status' => int, 'body' => decoded array|null). */
	protected function request($method, $path, $body = null, $token = null)
	{
		$ch = curl_init($this->base_url().$path);
		$headers = array('Content-Type: application/json');
		if ($token !== null)
		{
			$headers[] = 'Authorization: Bearer '.$token;
		}
		curl_setopt_array($ch, array(
			CURLOPT_CUSTOMREQUEST => $method,
			CURLOPT_HTTPHEADER => $headers,
			CURLOPT_RETURNTRANSFER => true,
		));
		if ($body !== null)
		{
			curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($body));
		}
		$raw = curl_exec($ch);
		$status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
		curl_close($ch);
		return array('status' => $status, 'body' => json_decode($raw, true));
	}

	/** A fresh business + owner per call — unique email avoids cross-test
	 * collisions in the shared dev database (see bootstrap.php's note on
	 * why this can't use Django's per-run throwaway test database). Returns
	 * array('access', 'refresh', 'user', 'business'). */
	protected function register_test_business($overrides = array())
	{
		$email = 'phpunit-'.bin2hex(random_bytes(6)).'@example.com';
		$response = $this->request('POST', '/api/auth/register/', array_replace(array(
			'email' => $email,
			'password' => 'testpass123',
			'first_name' => 'Test',
			'last_name' => 'User',
			'business' => array(
				'name' => 'PHPUnit Test Business',
				'province' => 'WC',
			),
		), $overrides));
		$this->assertSame(201, $response['status'], 'register failed: '.json_encode($response['body']));
		return $response['body'];
	}
}
