<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * /login, /register, /logout — the web panel's own session-based auth,
 * entirely separate from the JWT the Android app uses (see Web_Controller's
 * doc comment), the same way Perfex CRM keeps its panel login apart from
 * any API auth.
 *
 * Registration writes exactly what POST /api/auth/register/ writes — user,
 * business, owner membership — through the same models, so an account made
 * here logs into the phone app and vice versa. It asks for the four things
 * that can't be guessed; the rest of the business profile is filled in on
 * the phone.
 */
class Web_auth extends CI_Controller {

	public function __construct()
	{
		parent::__construct();
		$this->load->library('session');
		$this->load->helper('url');
		$this->load->helper('web');
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

		$csrf = $this->csrf_token();
		$error = NULL;
		$email = '';

		if ($this->input->method() === 'post')
		{
			$email = trim((string) $this->input->post('email'));

			if (!hash_equals($csrf, (string) $this->input->post('csrf_token')))
			{
				$error = 'Your session expired — please try again.';
			}
			else
			{
				$user = $this->User_model->find_by_email($email);
				if ($user === NULL || !$this->User_model->verify_password($user, (string) $this->input->post('password')))
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
						$this->start_session($user['id'], $membership['business_id']);
						redirect('dashboard');
					}
				}
			}
		}

		$this->load->view('web/login', array('error' => $error, 'csrf_token' => $csrf, 'email' => $email));
	}

	public function register()
	{
		if ($this->session->userdata('business_id'))
		{
			redirect('dashboard');
		}

		$csrf = $this->csrf_token();
		$errors = array();
		$values = array('business_name' => '', 'first_name' => '', 'last_name' => '', 'email' => '');

		if ($this->input->method() === 'post')
		{
			foreach (array_keys($values) as $field)
			{
				$values[$field] = trim((string) $this->input->post($field));
			}
			$password = (string) $this->input->post('password');

			if (!hash_equals($csrf, (string) $this->input->post('csrf_token')))
			{
				$errors['form'] = 'Your session expired — please try again.';
			}
			if ($values['business_name'] === '')
			{
				$errors['business_name'] = 'What is the business called?';
			}
			if ($values['first_name'] === '')
			{
				$errors['first_name'] = 'Your name is required.';
			}
			if (!filter_var($values['email'], FILTER_VALIDATE_EMAIL))
			{
				$errors['email'] = 'That email address doesn\'t look right.';
			}
			elseif ($this->User_model->email_taken($values['email']))
			{
				$errors['email'] = 'An account already uses that email.';
			}
			if (strlen($password) < 8)
			{
				$errors['password'] = 'Use at least 8 characters.';
			}

			if (empty($errors))
			{
				// One transaction: a user without a business (or a business
				// with no owner) would be a broken half-account.
				$this->db->trans_begin();
				$user_id = $this->User_model->create($values['email'], $password, $values['first_name'], $values['last_name']);
				$business_id = $this->Business_model->create(array('name' => $values['business_name']));
				$this->Membership_model->create($user_id, $business_id, 'owner');

				if ($this->db->trans_status() === FALSE)
				{
					$this->db->trans_rollback();
					$errors['form'] = 'Something went wrong creating the account. Please try again.';
				}
				else
				{
					$this->db->trans_commit();
					$this->start_session($user_id, $business_id);
					redirect('dashboard');
				}
			}
		}

		$this->load->view('web/register', array('errors' => $errors, 'csrf_token' => $csrf, 'values' => $values));
	}

	public function logout()
	{
		$this->session->sess_destroy();
		redirect('login');
	}

	private function csrf_token()
	{
		$token = $this->session->userdata('csrf_token') ?: bin2hex(random_bytes(16));
		$this->session->set_userdata('csrf_token', $token);
		return $token;
	}

	/** New session id on privilege change — an attacker who fixed the
	 * pre-login session id gains nothing from it. */
	private function start_session($user_id, $business_id)
	{
		$this->session->sess_regenerate(TRUE);
		$this->session->set_userdata(array('user_id' => $user_id, 'business_id' => $business_id));
	}
}
