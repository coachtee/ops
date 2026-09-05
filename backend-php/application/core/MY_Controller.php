<?php
defined('BASEPATH') OR exit('No direct script access allowed');

use chriskacerguis\RestServer\RestController;

/**
 * Every authenticated endpoint extends this — mirrors BusinessScopedViewSet
 * in the Django backend this replaces (see backend/common/views.py): verify
 * the bearer token, resolve the caller's single business via Membership
 * (V1 assumption unchanged — see docs/DISCOVERY.md), and expose $this->
 * business_id/$this->user_id to the resource controller. A 401 is returned
 * (request never reaches the resource controller) for a missing, malformed,
 * expired, or wrong-type token — no endpoint below this class ever has to
 * re-check auth itself.
 */
class Api_Controller extends RestController {

	protected $business_id;
	protected $user_id;

	public function __construct()
	{
		parent::__construct();
		$this->load->library('Auth_lib');
		$this->load->model('Membership_model');

		$token = $this->auth_lib->bearer_token_from_request();
		$payload = $token ? $this->auth_lib->verify($token, 'access') : FALSE;

		if ($payload === FALSE)
		{
			// response() with its default $continue=false calls exit()
			// internally — execution genuinely stops here, the requested
			// controller method is never invoked.
			$this->response(array('detail' => 'Authentication credentials were not provided or are invalid.'), 401);
		}

		$membership = $this->Membership_model->for_user($payload->user_id);
		if ($membership === NULL)
		{
			$this->response(array('detail' => 'This user has no business membership.'), 401);
		}

		$this->user_id = $payload->user_id;
		$this->business_id = $membership['business_id'];
	}
}

/** register/login/refresh/health — the only endpoints reachable without a
 * token, per docs/API_CONTRACT.md's "Auth" section (unchanged by this
 * rewrite). */
class Public_Api_Controller extends RestController {

	public function __construct()
	{
		parent::__construct();
	}
}
