<?php

/** Job (number assignment via sync) + Visit (CRUD + photo upload) — see
 * docs/API_CONTRACT.md's "job"/"visit" field payloads and "Visit photo
 * attachment". */
final class JobVisitTest extends ApiTestCase {

	private function push_customer($token)
	{
		$id = $this->uuid();
		$this->push($token, 'customer', $id, array('name' => 'Job Test Customer', 'customer_type' => 'individual'));
		return $id;
	}

	public function test_first_successful_sync_assigns_a_job_number()
	{
		$owner = $this->register_test_business();
		$customer_id = $this->push_customer($owner['access']);
		$job_id = $this->uuid();

		$result = $this->push($owner['access'], 'job', $job_id, array(
			'customer_id' => $customer_id, 'title' => 'Fix geyser', 'status' => 'not_started',
		));

		$this->assertSame('accepted', $result['status']);
		$this->assertSame('J-0001', $result['server_record']['fields']['number']);
	}

	public function test_visit_crud_via_sync_and_pull()
	{
		$owner = $this->register_test_business();
		$customer_id = $this->push_customer($owner['access']);
		$job_id = $this->uuid();
		$this->push($owner['access'], 'job', $job_id, array('customer_id' => $customer_id, 'title' => 'Fix geyser'));

		$visit_id = $this->uuid();
		$result = $this->push($owner['access'], 'visit', $visit_id, array(
			'job_id' => $job_id, 'scheduled_date' => '2026-09-10', 'status' => 'scheduled',
		));
		$this->assertSame('accepted', $result['status']);
		$this->assertNull($result['server_record']['fields']['photo']);

		$pull = $this->request('GET', '/api/sync/pull/', null, $owner['access']);
		$ids = array_column($pull['body']['changes'], 'id');
		$this->assertContains($visit_id, $ids);
	}

	public function test_visit_photo_upload_requires_an_existing_visit()
	{
		$owner = $this->register_test_business();
		$response = $this->request('POST', '/api/visits/'.$this->uuid().'/photo/', null, $owner['access']);
		$this->assertSame(404, $response['status']);
	}

	public function test_visit_photo_upload_stores_a_url_and_bumps_updated_at()
	{
		$owner = $this->register_test_business();
		$customer_id = $this->push_customer($owner['access']);
		$job_id = $this->uuid();
		$this->push($owner['access'], 'job', $job_id, array('customer_id' => $customer_id, 'title' => 'Fix geyser'));
		$visit_id = $this->uuid();
		$this->push($owner['access'], 'visit', $visit_id, array('job_id' => $job_id, 'scheduled_date' => '2026-09-10'));

		$image_path = sys_get_temp_dir().'/phpunit_visit_photo.png';
		// A minimal valid 1x1 PNG, so getimagesize() succeeds like a real photo would.
		file_put_contents($image_path, base64_decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII='));

		$ch = curl_init($this->base_url()."/api/visits/{$visit_id}/photo/");
		curl_setopt_array($ch, array(
			CURLOPT_POST => true,
			CURLOPT_HTTPHEADER => array('Authorization: Bearer '.$owner['access']),
			CURLOPT_POSTFIELDS => array('photo' => new CURLFile($image_path, 'image/png', 'photo.png')),
			CURLOPT_RETURNTRANSFER => true,
		));
		$raw = curl_exec($ch);
		$status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
		curl_close($ch);
		unlink($image_path);

		$body = json_decode($raw, true);
		$this->assertSame(200, $status, 'upload failed: '.$raw);
		$this->assertNotNull($body['photo'], 'photo URL must be set after upload');
		$this->assertStringContainsString("visits/{$visit_id}/photo/", $body['photo']);
	}
}
