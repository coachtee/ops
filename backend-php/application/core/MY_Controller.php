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

/**
 * Base for the Perfex-CRM-style web admin panel (application/views/web/*,
 * application/controllers/Web_*.php) — a completely separate login/session
 * from the JWT the Android app uses (see Api_Controller above), same as how
 * Perfex CRM itself keeps its web panel login apart from any API/module
 * auth. A plain PHP session (CI3's built-in session library), not a token:
 * this is a server-rendered, cookie-based admin UI for the business owner
 * to view their data in a browser, not something a mobile client talks to.
 */
class Web_Controller extends CI_Controller {

	protected $business_id;
	protected $user_id;
	protected $business;

	public function __construct()
	{
		parent::__construct();
		$this->load->library('session');
		$this->load->helper('url');
		$this->load->helper('web');

		$business_id = $this->session->userdata('business_id');
		if (!$business_id)
		{
			redirect('login');
		}

		$this->load->model('Business_model');
		$business = $this->Business_model->find($business_id);
		if ($business === NULL)
		{
			$this->session->sess_destroy();
			redirect('login');
		}

		$this->business_id = $business_id;
		$this->user_id = $this->session->userdata('user_id');
		$this->business = $business;
	}

	/** Renders $view inside application/views/web/layout.php's shared
	 * sidebar/topbar chrome — every web page but the login form uses this. */
	protected function render($view, array $data = array())
	{
		$data['business'] = $this->business;
		$data['active_nav'] = $data['active_nav'] ?? '';
		$data['content_html'] = $this->load->view('web/'.$view, $data, TRUE);
		$this->load->view('web/layout', $data);
	}
}
