<?php

/** PATCH /api/business/me/ with a multipart body (a new logo attached) —
 * see OpsApiService.updateBusinessWithLogo() / BusinessRepository
 * .updateProfileWithLogo() on the Android side. PHP does not auto-parse a
 * multipart body on anything but POST, so Business::me_patch() hand-parses
 * it — see application/helpers/multipart_helper.php. This is the one path
 * through this endpoint Android actually exercises for a logo change. */
final class BusinessLogoTest extends ApiTestCase {

	public function test_multipart_patch_updates_fields_and_uploads_logo()
	{
		$owner = $this->register_test_business();
		$image_path = sys_get_temp_dir().'/phpunit_logo.png';
		file_put_contents($image_path, base64_decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII='));

		$ch = curl_init($this->base_url().'/api/business/me/');
		curl_setopt_array($ch, array(
			CURLOPT_CUSTOMREQUEST => 'PATCH',
			CURLOPT_HTTPHEADER => array('Authorization: Bearer '.$owner['access']),
			CURLOPT_POSTFIELDS => array(
				'name' => 'Logo Test Biz Updated',
				'trading_name' => '', 'registration_number' => '', 'tax_number' => '', 'vat_number' => '',
				'is_vat_registered' => 'true', 'industry' => 'plumbing', 'phone' => '', 'email' => '',
				'address_line1' => '', 'address_line2' => '', 'suburb' => '', 'city' => '', 'province' => 'WC', 'postal_code' => '',
				'logo' => new CURLFile($image_path, 'image/png', 'logo.png'),
			),
			CURLOPT_RETURNTRANSFER => true,
		));
		$raw = curl_exec($ch);
		$status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
		curl_close($ch);
		unlink($image_path);

		$body = json_decode($raw, true);
		$this->assertSame(200, $status, 'multipart PATCH failed: '.$raw);
		$this->assertSame('Logo Test Biz Updated', $body['name']);
		$this->assertTrue($body['is_vat_registered']);
		$this->assertNotNull($body['logo']);
	}
}
