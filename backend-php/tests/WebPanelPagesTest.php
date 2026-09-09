<?php

/**
 * Every page of the reworked web panel, driven over real HTTP with a real
 * session cookie: the public pages, the twelve module lists, the detail
 * pages, search, reports and CSV export. The point is that a route exists,
 * renders, and renders *this* business's data only — the previous suite
 * covered auth and scoping, this one covers reach.
 */
final class WebPanelPagesTest extends ApiTestCase {

	private $cookie_jar;

	protected function setUp(): void
	{
		$this->cookie_jar = tempnam(sys_get_temp_dir(), 'ops_panel_');
	}

	protected function tearDown(): void
	{
		@unlink($this->cookie_jar);
	}

	private function web($method, $path, $post = null)
	{
		$ch = curl_init($this->base_url().$path);
		curl_setopt_array($ch, array(
			CURLOPT_CUSTOMREQUEST => $method,
			CURLOPT_COOKIEJAR => $this->cookie_jar,
			CURLOPT_COOKIEFILE => $this->cookie_jar,
			CURLOPT_RETURNTRANSFER => true,
		));
		if ($post !== null)
		{
			curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query($post));
		}
		$body = curl_exec($ch);
		$status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
		$redirect = curl_getinfo($ch, CURLINFO_REDIRECT_URL);
		curl_close($ch);
		return array('status' => $status, 'body' => $body, 'redirect' => $redirect);
	}

	private function csrf($html)
	{
		preg_match('/name="csrf_token" value="([^"]*)"/', $html, $m);
		return $m[1] ?? '';
	}

	/** Registers via the API, then logs that same account into the panel —
	 * proving the two auth paths share one account, as documented. */
	private function login_fresh_business()
	{
        $email = 'panel-'.bin2hex(random_bytes(6)).'@example.com';
		$password = 'testpass123';
		$api = $this->request('POST', '/api/auth/register/', array(
			'email' => $email, 'password' => $password, 'first_name' => 'Panel', 'last_name' => 'Owner',
			'business' => array('name' => 'Panel Pages Business', 'province' => 'WC'),
		));
		$this->assertSame(201, $api['status']);

		$page = $this->web('GET', '/login/');
		$login = $this->web('POST', '/login/', array(
			'csrf_token' => $this->csrf($page['body']), 'email' => $email, 'password' => $password,
		));
		$this->assertStringContainsString('/dashboard', (string) $login['redirect']);

		return $api['body'];
	}

	public function test_landing_page_is_public_and_links_to_signup()
	{
		$response = $this->web('GET', '/');
		$this->assertSame(200, $response['status']);
		$this->assertStringContainsString('register', $response['body']);
		$this->assertStringContainsString('OPS', $response['body']);
	}

	public function test_register_page_is_public()
	{
		$response = $this->web('GET', '/register/');
		$this->assertSame(200, $response['status']);
		$this->assertNotSame('', $this->csrf($response['body']));
	}

	public function test_registering_through_the_web_creates_a_working_account()
	{
		$email = 'websignup-'.bin2hex(random_bytes(6)).'@example.com';

		$page = $this->web('GET', '/register/');
		$register = $this->web('POST', '/register/', array(
			'csrf_token' => $this->csrf($page['body']),
			'business_name' => 'Web Signup Plumbing',
			'first_name' => 'Web', 'last_name' => 'Owner',
			'email' => $email, 'password' => 'testpass123',
		));
		$this->assertStringContainsString('/dashboard', (string) $register['redirect'], 'sign-up should land on the dashboard');

		// The same credentials must work against the JSON API the phone uses.
		$api = $this->request('POST', '/api/auth/login/', array('email' => $email, 'password' => 'testpass123'));
		$this->assertSame(200, $api['status'], 'a web sign-up must be able to log in on the phone');
		$this->assertSame('Web Signup Plumbing', $api['body']['business']['name']);
	}

	public function test_registering_rejects_a_short_password_and_a_duplicate_email()
	{
		$page = $this->web('GET', '/register/');
		$short = $this->web('POST', '/register/', array(
			'csrf_token' => $this->csrf($page['body']), 'business_name' => 'X', 'first_name' => 'Y',
			'email' => 'dupe-'.bin2hex(random_bytes(4)).'@example.com', 'password' => 'short',
		));
		$this->assertSame(200, $short['status']);
		$this->assertStringContainsString('at least 8 characters', $short['body']);

		$owner = $this->login_fresh_business();
		$this->web('GET', '/logout/');
		$page = $this->web('GET', '/register/');
		$dupe = $this->web('POST', '/register/', array(
			'csrf_token' => $this->csrf($page['body']), 'business_name' => 'X', 'first_name' => 'Y',
			'email' => $owner['user']['email'], 'password' => 'testpass123',
		));
		$this->assertStringContainsString('already uses that email', $dupe['body']);
	}

	public function test_every_module_page_renders_for_a_logged_in_business()
	{
		$this->login_fresh_business();

		$paths = array(
			'/dashboard/', '/leads/', '/customers/', '/quotes/', '/invoices/', '/payments/',
			'/jobs/', '/schedule/', '/expenses/', '/suppliers/', '/employees/', '/payslips/',
			'/compliance/', '/reports/', '/settings/', '/search/?q=nothing',
		);
		foreach ($paths as $path)
		{
			$response = $this->web('GET', $path);
			$this->assertSame(200, $response['status'], $path.' should render');
			$this->assertStringNotContainsString('A PHP Error was encountered', $response['body'], $path.' rendered a PHP error');
			$this->assertStringNotContainsString('A Database Error Occurred', $response['body'], $path.' rendered a database error');
		}
	}

	public function test_list_filters_and_search_keep_working_together()
	{
		$owner = $this->login_fresh_business();
		$access = $owner['access'];

		$customer_id = $this->uuid();
		$this->push($access, 'customer', $customer_id, array('name' => 'Filterable Customer', 'customer_type' => 'company'));
		$this->push($access, 'lead', $this->uuid(), array('name' => 'Chasing Lead', 'source' => 'whatsapp', 'status' => 'new'));

		$found = $this->web('GET', '/customers/?q=Filterable');
		$this->assertStringContainsString('Filterable Customer', $found['body']);

		$filtered = $this->web('GET', '/customers/?status=individual');
		$this->assertStringNotContainsString('Filterable Customer', $filtered['body'], 'a company must not show under the individuals filter');

		$missing = $this->web('GET', '/customers/?q=zzzznotathing');
		$this->assertStringContainsString('No customers match that', $missing['body']);

		$leads = $this->web('GET', '/leads/?status=new');
		$this->assertStringContainsString('Chasing Lead', $leads['body']);
	}

	public function test_global_search_finds_records_across_types()
	{
		$owner = $this->login_fresh_business();
		$this->push($owner['access'], 'customer', $this->uuid(), array('name' => 'Searchable Roofing', 'customer_type' => 'company'));

		$response = $this->web('GET', '/search/?q=Searchable');
		$this->assertSame(200, $response['status']);
		$this->assertStringContainsString('Searchable Roofing', $response['body']);
	}

	public function test_reports_csv_export_streams_the_same_columns_as_the_api()
	{
		$this->login_fresh_business();
		$response = $this->web('GET', '/reports/export?months=6');
		$this->assertSame(200, $response['status']);
		$this->assertStringStartsWith('Month,Revenue,Expenses,Profit', trim($response['body']));
	}

	public function test_a_detail_page_for_another_businesss_record_is_404()
	{
		$other = $this->register_test_business();
		$hidden_id = $this->uuid();
		$this->push($other['access'], 'customer', $hidden_id, array('name' => 'Hidden Co', 'customer_type' => 'company'));

		$this->login_fresh_business();
		$response = $this->web('GET', '/customers/'.$hidden_id);
		$this->assertSame(404, $response['status']);
		$this->assertStringNotContainsString('Hidden Co', $response['body']);
	}

	public function test_invoice_detail_shows_line_items_and_recomputed_totals()
	{
		$owner = $this->login_fresh_business();
		$access = $owner['access'];

		$customer_id = $this->uuid();
		$this->push($access, 'customer', $customer_id, array('name' => 'Detail Customer', 'customer_type' => 'individual'));
		$invoice_id = $this->uuid();
		$this->push($access, 'invoice', $invoice_id, array(
			'customer_id' => $customer_id, 'status' => 'sent', 'issue_date' => '2026-09-01', 'is_vat_applicable' => true,
		));
		$this->push($access, 'invoice_line_item', $this->uuid(), array(
			'invoice_id' => $invoice_id, 'description' => 'Callout and repair', 'quantity' => '1', 'unit_price' => '1000.00',
		));

		$response = $this->web('GET', '/invoices/'.$invoice_id);
		$this->assertSame(200, $response['status']);
		$this->assertStringContainsString('Callout and repair', $response['body']);
		$this->assertStringContainsString('INV-0001', $response['body']);
		// 1000 + 15% VAT, recomputed server-side by the sync push path.
		$this->assertStringContainsString('1,150.00', $response['body']);
	}
}
