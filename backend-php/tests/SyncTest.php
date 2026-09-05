<?php

/** POST /api/sync/push/, GET /api/sync/pull/ — the app's real read/write
 * path (see docs/API_CONTRACT.md's "Sync" section). Mirrors the Django
 * backend's own sync tests: accept, idempotent replay, and cross-tenant
 * scoping on pull. */
final class SyncTest extends ApiTestCase {

	public function test_push_accepts_a_new_customer()
	{
		$owner = $this->register_test_business();
		$id = $this->uuid();

		$response = $this->request('POST', '/api/sync/push/', array(
			'changes' => array(array(
				'model' => 'customer',
				'id' => $id,
				'updated_at' => $this->iso_now(),
				'deleted_at' => null,
				'fields' => array('name' => 'Sync Test Customer', 'customer_type' => 'individual'),
			)),
		), $owner['access']);

		$this->assertSame(200, $response['status']);
		$this->assertSame('accepted', $response['body']['results'][0]['status']);
		$this->assertSame($id, $response['body']['results'][0]['server_record']['id']);
	}

	public function test_pull_returns_a_previously_pushed_customer()
	{
		$owner = $this->register_test_business();
		$id = $this->uuid();
		$this->request('POST', '/api/sync/push/', array(
			'changes' => array(array(
				'model' => 'customer', 'id' => $id, 'updated_at' => $this->iso_now(), 'deleted_at' => null,
				'fields' => array('name' => 'Pulled Customer', 'customer_type' => 'individual'),
			)),
		), $owner['access']);

		$pull = $this->request('GET', '/api/sync/pull/', null, $owner['access']);
		$this->assertSame(200, $pull['status']);
		$ids = array_column($pull['body']['changes'], 'id');
		$this->assertContains($id, $ids);
	}

	public function test_replaying_the_same_push_is_idempotent_not_a_duplicate()
	{
		$owner = $this->register_test_business();
		$id = $this->uuid();
		$change = array(
			'model' => 'customer', 'id' => $id, 'updated_at' => $this->iso_now(), 'deleted_at' => null,
			'fields' => array('name' => 'Idempotency Test', 'customer_type' => 'individual'),
		);

		$first = $this->request('POST', '/api/sync/push/', array('changes' => array($change)), $owner['access']);
		$this->assertSame('accepted', $first['body']['results'][0]['status']);

		$second = $this->request('POST', '/api/sync/push/', array('changes' => array($change)), $owner['access']);
		$this->assertSame('conflict', $second['body']['results'][0]['status'], 'a replayed push must not create a duplicate');

		$pull = $this->request('GET', '/api/sync/pull/', null, $owner['access']);
		$matching = array_filter($pull['body']['changes'], fn($c) => $c['id'] === $id);
		$this->assertCount(1, $matching, 'exactly one row must exist after the replay');
	}

	public function test_a_genuinely_newer_update_overwrites_an_older_one()
	{
		$owner = $this->register_test_business();
		$id = $this->uuid();
		$older = '2026-01-01T00:00:00.000000Z';
		$newer = '2026-06-01T00:00:00.000000Z';

		$this->request('POST', '/api/sync/push/', array('changes' => array(array(
			'model' => 'customer', 'id' => $id, 'updated_at' => $older, 'deleted_at' => null,
			'fields' => array('name' => 'Old Name', 'customer_type' => 'individual'),
		))), $owner['access']);

		$second = $this->request('POST', '/api/sync/push/', array('changes' => array(array(
			'model' => 'customer', 'id' => $id, 'updated_at' => $newer, 'deleted_at' => null,
			'fields' => array('name' => 'New Name', 'customer_type' => 'individual'),
		))), $owner['access']);

		$this->assertSame('accepted', $second['body']['results'][0]['status']);
		$this->assertSame('New Name', $second['body']['results'][0]['server_record']['fields']['name']);
	}

	public function test_pull_never_returns_another_businesss_records()
	{
		$owner_a = $this->register_test_business();
		$owner_b = $this->register_test_business();
		$id = $this->uuid();

		$this->request('POST', '/api/sync/push/', array('changes' => array(array(
			'model' => 'customer', 'id' => $id, 'updated_at' => $this->iso_now(), 'deleted_at' => null,
			'fields' => array('name' => 'Business A Only', 'customer_type' => 'individual'),
		))), $owner_a['access']);

		$pull_b = $this->request('GET', '/api/sync/pull/', null, $owner_b['access']);
		$ids = array_column($pull_b['body']['changes'], 'id');
		$this->assertNotContains($id, $ids);
	}
}
