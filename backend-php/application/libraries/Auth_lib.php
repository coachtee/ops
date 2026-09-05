<?php
defined('BASEPATH') OR exit('No direct script access allowed');

use Firebase\JWT\JWT;
use Firebase\JWT\Key;

/**
 * JWT issue/verify — mirrors djangorestframework-simplejwt's role in the
 * Django backend this replaces (see docs/API_CONTRACT.md's "Auth" section,
 * unchanged by this rewrite): access token short-lived, refresh token
 * long-lived, `Authorization: Bearer <access>` on every authenticated
 * request. HS256 signed with config('encryption_key') (OPS_SECRET_KEY).
 */
class Auth_lib {

	const ACCESS_TOKEN_LIFETIME_SECONDS = 8 * 3600;   // 8 hours, matches SIMPLE_JWT ACCESS_TOKEN_LIFETIME
	const REFRESH_TOKEN_LIFETIME_SECONDS = 30 * 86400; // 30 days, matches SIMPLE_JWT REFRESH_TOKEN_LIFETIME

	private $secret;

	public function __construct()
	{
		$ci =& get_instance();
		$ci->config->load('config', TRUE); // no-op if already loaded; ensures encryption_key is available
		$this->secret = $ci->config->item('encryption_key');
	}

	private function issue($user_id, $type, $lifetime_seconds)
	{
		$now = time();
		$payload = array(
			'user_id' => $user_id,
			'type' => $type, // 'access' | 'refresh' — mirrors simplejwt's token_type claim
			'iat' => $now,
			'exp' => $now + $lifetime_seconds,
		);
		return JWT::encode($payload, $this->secret, 'HS256');
	}

	public function issue_access_token($user_id)
	{
		return $this->issue($user_id, 'access', self::ACCESS_TOKEN_LIFETIME_SECONDS);
	}

	public function issue_refresh_token($user_id)
	{
		return $this->issue($user_id, 'refresh', self::REFRESH_TOKEN_LIFETIME_SECONDS);
	}

	/**
	 * Returns the decoded payload object, or FALSE if the token is missing,
	 * malformed, expired, or not of the expected type — the caller never
	 * needs to catch a JWT library exception directly.
	 */
	public function verify($token, $expected_type)
	{
		try
		{
			$payload = JWT::decode($token, new Key($this->secret, 'HS256'));
		}
		catch (Exception $e)
		{
			return FALSE;
		}
		if (!isset($payload->type) || $payload->type !== $expected_type)
		{
			return FALSE;
		}
		return $payload;
	}

	public function bearer_token_from_request()
	{
		$header = $this->authorization_header();
		if ($header === NULL)
		{
			return NULL;
		}
		if (stripos($header, 'Bearer ') !== 0)
		{
			return NULL;
		}
		return trim(substr($header, 7));
	}

	private function authorization_header()
	{
		// getallheaders() is the normal path; some SAPIs/proxies only expose
		// it via $_SERVER, so both are checked — a missing header here must
		// never be silently treated as "no auth needed".
		if (function_exists('getallheaders'))
		{
			foreach (getallheaders() as $name => $value)
			{
				if (strcasecmp($name, 'Authorization') === 0)
				{
					return $value;
				}
			}
		}
		if (!empty($_SERVER['HTTP_AUTHORIZATION']))
		{
			return $_SERVER['HTTP_AUTHORIZATION'];
		}
		return NULL;
	}
}
