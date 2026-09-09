<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** / — the public landing page. Someone already logged in gets taken
 * straight to their dashboard instead of a marketing page. */
class Web_landing extends CI_Controller {

	public function index()
	{
		$this->load->library('session');
		$this->load->helper('url');
		$this->load->helper('web');

		if ($this->session->userdata('business_id'))
		{
			redirect('dashboard');
		}

		$this->load->view('web/landing');
	}
}
