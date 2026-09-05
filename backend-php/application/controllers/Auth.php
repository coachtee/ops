<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** POST /api/auth/register/, /api/auth/login/, /api/auth/refresh/ — see
 * docs/API_CONTRACT.md's "Auth" section (unchanged by this rewrite). */
class Auth extends Public_Api_Controller {

	public function __construct()
	{
		parent::__construct();
		$this->load->library('Auth_lib');
		$this->load->model('User_model');
		$this->load->model('Business_model');
		$this->load->model('Membership_model');
	}

	private function issue_tokens_for($user_id)
	{
		return array(
			'access' => $this->auth_lib->issue_access_token($user_id),
			'refresh' => $this->auth_lib->issue_refresh_token($user_id),
		);
	}

	public function register_post()
	{
		$body = json_decode($this->input->raw_input_stream, TRUE) ?: array();
		$errors = array();

		$email = trim($body['email'] ?? '');
		$password = (string) ($body['password'] ?? '');
		$business = $body['business'] ?? array();

		if ($email === '' || !filter_var($email, FILTER_VALIDATE_EMAIL))
		{
			$errors['email'] = array('Enter a valid email address.');
		}
		elseif ($this->User_model->email_taken($email))
		{
			$errors['email'] = array('A user with that email already exists.');
		}
		if (strlen($password) < 8)
		{
			$errors['password'] = array('Password must be at least 8 characters.');
		}
		if (empty($business['name']))
		{
			$errors['business'] = array('name' => array('This field is required.'));
		}

		if (!empty($errors))
		{
			$this->response($errors, 400);
		}

		$user_id = $this->User_model->create($email, $password, $body['first_name'] ?? '', $body['last_name'] ?? '');
		$business_id = $this->Business_model->create($business);
		$this->Membership_model->create($user_id, $business_id, 'owner');

		$tokens = $this->issue_tokens_for($user_id);
		$this->response(array(
			'access' => $tokens['access'],
			'refresh' => $tokens['refresh'],
			'user' => $this->User_model->to_wire($this->User_model->find($user_id)),
			'business' => $this->Business_model->to_wire($this->Business_model->find($business_id)),
		), 201);
	}

	public function login_post()
	{
		$body = json_decode($this->input->raw_input_stream, TRUE) ?: array();
		$email = trim($body['email'] ?? '');
		$password = (string) ($body['password'] ?? '');

		$user = $this->User_model->find_by_email($email);
		if ($user === NULL || !$this->User_model->verify_password($user, $password))
		{
			$this->response(array('detail' => 'No active account found with the given credentials.'), 401);
		}

		$membership = $this->Membership_model->for_user($user['id']);
		if ($membership === NULL)
		{
			$this->response(array('detail' => 'This user has no business membership.'), 401);
		}

		$tokens = $this->issue_tokens_for($user['id']);
		$this->response(array(
			'access' => $tokens['access'],
			'refresh' => $tokens['refresh'],
			'user' => $this->User_model->to_wire($user),
			'business' => $this->Business_model->to_wire($this->Business_model->find($membership['business_id'])),
		), 200);
	}

	public function refresh_post()
	{
		$body = json_decode($this->input->raw_input_stream, TRUE) ?: array();
		$refresh_token = $body['refresh'] ?? '';

		$payload = $refresh_token ? $this->auth_lib->verify($refresh_token, 'refresh') : FALSE;
		if ($payload === FALSE)
		{
			$this->response(array('detail' => 'Token is invalid or expired.'), 401);
		}

		$this->response(array('access' => $this->auth_lib->issue_access_token($payload->user_id)), 200);
	}
}
