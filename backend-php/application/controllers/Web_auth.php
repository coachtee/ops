<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /login, /logout — the web admin panel's own session-based login,
 * entirely separate from the JWT the Android app uses (see
 * Web_Controller's doc comment). Same User/Membership/Business_model as
 * the API's Auth controller, just a different credential/session
 * mechanism on top of them. */
class Web_auth extends CI_Controller {

	public function __construct()
	{
		parent::__construct();
		$this->load->library('session');
		$this->load->helper('url');
		$this->load->model('User_model');
		$this->load->model('Membership_model');
		$this->load->model('Business_model');
	}

	public function login()
	{
		if ($this->session->userdata('business_id'))
		{
			redirect('dashboard');
		}

		$csrf_token = $this->session->userdata('csrf_token') ?: bin2hex(random_bytes(16));
		$this->session->set_userdata('csrf_token', $csrf_token);

		$error = NULL;
		if ($this->input->method() === 'post')
		{
			if (!hash_equals($csrf_token, (string) $this->input->post('csrf_token')))
			{
				$error = 'Your session expired — please try again.';
			}
			else
			{
				$email = trim((string) $this->input->post('email'));
				$password = (string) $this->input->post('password');
				$user = $this->User_model->find_by_email($email);

				if ($user === NULL || !$this->User_model->verify_password($user, $password))
				{
					$error = 'Incorrect email or password.';
				}
				else
				{
					$membership = $this->Membership_model->for_user($user['id']);
					if ($membership === NULL)
					{
						$error = 'This user has no business to manage.';
					}
					else
					{
						$this->session->set_userdata(array(
							'user_id' => $user['id'],
							'business_id' => $membership['business_id'],
						));
						redirect('dashboard');
					}
				}
			}
		}

		$this->load->view('web/login', array('error' => $error, 'csrf_token' => $csrf_token));
	}

	public function logout()
	{
		$this->session->sess_destroy();
		redirect('login');
	}
}
