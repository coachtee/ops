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

	protected function uuid()
	{
		$data = random_bytes(16);
		$data[6] = chr((ord($data[6]) & 0x0f) | 0x40);
		$data[8] = chr((ord($data[8]) & 0x3f) | 0x80);
		return vsprintf('%s%s-%s-%s-%s-%s%s%s', str_split(bin2hex($data), 4));
	}

	protected function iso_now()
	{
		return gmdate('Y-m-d\TH:i:s.000000\Z');
	}

	/** Pushes one sync change and returns the decoded response body. */
	protected function push($token, $model, $id, array $fields, $deleted_at = null, $updated_at = null)
	{
		$response = $this->request('POST', '/api/sync/push/', array(
			'changes' => array(array(
				'model' => $model,
				'id' => $id,
				'updated_at' => $updated_at ?? $this->iso_now(),
				'deleted_at' => $deleted_at,
				'fields' => $fields,
			)),
		), $token);
		return $response['body']['results'][0];
	}
}
