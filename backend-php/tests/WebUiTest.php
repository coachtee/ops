<?php

/** The Perfex-CRM-style web admin panel (application/controllers/Web_*.php,
 * application/views/web/*) — session-cookie auth, entirely separate from
 * the JWT the Android app uses. Registers a business via the JSON API (the
 * only way to create a user/password in this app) then drives the web
 * panel with a real cookie jar over real HTTP, same rigor as the rest of
 * this suite. */
final class WebUiTest extends ApiTestCase {

	private $cookie_jar;

	protected function setUp(): void
	{
		$this->cookie_jar = tempnam(sys_get_temp_dir(), 'ops_cookies_');
	}

	protected function tearDown(): void
	{
		@unlink($this->cookie_jar);
	}

	/** Returns array('status', 'body', 'redirect_url'). */
	private function web_request($method, $path, $post_fields = null)
	{
		$ch = curl_init($this->base_url().$path);
		curl_setopt_array($ch, array(
			CURLOPT_CUSTOMREQUEST => $method,
			CURLOPT_COOKIEJAR => $this->cookie_jar,
			CURLOPT_COOKIEFILE => $this->cookie_jar,
			CURLOPT_RETURNTRANSFER => true,
		));
		if ($post_fields !== null)
		{
			curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query($post_fields));
		}
		$body = curl_exec($ch);
		$status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
		$redirect_url = curl_getinfo($ch, CURLINFO_REDIRECT_URL);
		curl_close($ch);
		return array('status' => $status, 'body' => $body, 'redirect_url' => $redirect_url);
	}

	private function extract_csrf_token($html)
	{
		preg_match('/name="csrf_token" value="([^"]*)"/', $html, $matches);
		return $matches[1] ?? null;
	}

	private function register_web_user()
	{
		$email = 'webui-'.bin2hex(random_bytes(6)).'@example.com';
		$password = 'testpass123';
		$response = $this->request('POST', '/api/auth/register/', array(
			'email' => $email, 'password' => $password, 'first_name' => 'Web', 'last_name' => 'Owner',
			'business' => array('name' => 'WebUiTest Business', 'province' => 'WC'),
		));
		$this->assertSame(201, $response['status']);
		return array('email' => $email, 'password' => $password, 'body' => $response['body']);
	}

	public function test_dashboard_redirects_to_login_when_unauthenticated()
	{
		$response = $this->web_request('GET', '/dashboard/');
		$this->assertStringContainsString('/login', $response['redirect_url']);
	}

	public function test_login_page_loads_and_carries_a_csrf_token()
	{
		$response = $this->web_request('GET', '/login/');
		$this->assertSame(200, $response['status']);
		$this->assertNotNull($this->extract_csrf_token($response['body']));
	}

	public function test_login_with_wrong_password_shows_an_error()
	{
		$user = $this->register_web_user();
		$login_page = $this->web_request('GET', '/login/');
		$csrf = $this->extract_csrf_token($login_page['body']);

		$response = $this->web_request('POST', '/login/', array(
			'csrf_token' => $csrf, 'email' => $user['email'], 'password' => 'wrong-password',
		));
		$this->assertSame(200, $response['status']);
		$this->assertStringContainsString('Incorrect email or password', $response['body']);
	}

	public function test_login_with_correct_credentials_reaches_the_dashboard()
	{
		$user = $this->register_web_user();
		$login_page = $this->web_request('GET', '/login/');
		$csrf = $this->extract_csrf_token($login_page['body']);

		$login = $this->web_request('POST', '/login/', array(
			'csrf_token' => $csrf, 'email' => $user['email'], 'password' => $user['password'],
		));
		$this->assertStringContainsString('/dashboard', $login['redirect_url']);

		$dashboard = $this->web_request('GET', '/dashboard/');
		$this->assertSame(200, $dashboard['status']);
		$this->assertStringContainsString('WebUiTest Business', $dashboard['body']);
	}

	public function test_customer_list_and_detail_reflect_synced_data()
	{
		$user = $this->register_web_user();
		$access = $user['body']['access'];
		$customer_id = $this->uuid();
		$this->push($access, 'customer', $customer_id, array('name' => 'Web Panel Customer', 'customer_type' => 'individual'));

		$login_page = $this->web_request('GET', '/login/');
		$csrf = $this->extract_csrf_token($login_page['body']);
		$this->web_request('POST', '/login/', array('csrf_token' => $csrf, 'email' => $user['email'], 'password' => $user['password']));

		$list = $this->web_request('GET', '/customers/');
		$this->assertStringContainsString('Web Panel Customer', $list['body']);

		$detail = $this->web_request('GET', "/customers/{$customer_id}/");
		$this->assertSame(200, $detail['status']);
		$this->assertStringContainsString('Web Panel Customer', $detail['body']);
	}

	public function test_two_businesses_only_see_their_own_customers_in_the_web_panel()
	{
		$user_a = $this->register_web_user();
		$user_b = $this->register_web_user();
		$customer_id = $this->uuid();
		$this->push($user_a['body']['access'], 'customer', $customer_id, array('name' => 'Only Business A', 'customer_type' => 'individual'));

		$login_page = $this->web_request('GET', '/login/');
		$csrf = $this->extract_csrf_token($login_page['body']);
		$this->web_request('POST', '/login/', array('csrf_token' => $csrf, 'email' => $user_b['email'], 'password' => $user_b['password']));

		$list = $this->web_request('GET', '/customers/');
		$this->assertStringNotContainsString('Only Business A', $list['body']);

		$detail = $this->web_request('GET', "/customers/{$customer_id}/");
		$this->assertSame(404, $detail['status']);
	}

	public function test_logout_clears_the_session()
	{
		$user = $this->register_web_user();
		$login_page = $this->web_request('GET', '/login/');
		$csrf = $this->extract_csrf_token($login_page['body']);
		$this->web_request('POST', '/login/', array('csrf_token' => $csrf, 'email' => $user['email'], 'password' => $user['password']));

		$this->web_request('GET', '/logout/');
		$after_logout = $this->web_request('GET', '/dashboard/');
		$this->assertStringContainsString('/login', $after_logout['redirect_url']);
	}
}
