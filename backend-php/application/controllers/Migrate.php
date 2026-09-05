<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * `php index.php migrate` — the equivalent of `manage.py migrate` in the
 * Django backend this replaces. CLI-only (see the is_cli_request() guard):
 * migrations should never be triggerable over HTTP.
 */
class Migrate extends CI_Controller {

	public function index()
	{
		if (!$this->input->is_cli_request())
		{
			show_404();
		}
		$this->load->library('migration');
		if ($this->migration->latest() === FALSE)
		{
			echo 'Migration failed: '.$this->migration->error_string().PHP_EOL;
			exit(1);
		}
		echo 'Migrated to the latest version.'.PHP_EOL;
	}
}
