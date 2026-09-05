<?php
defined('BASEPATH') OR exit('No direct script access allowed');

use chriskacerguis\RestServer\RestController;

/** GET /api/health/ — see docs/API_CONTRACT.md's "Health check" section
 * (unchanged by this rewrite, just reimplemented). No auth required. */
class Health extends RestController {

	public function index_get()
	{
		try
		{
			$this->db->query('SELECT 1');
			$database_status = 'ok';
		}
		catch (Exception $e)
		{
			$database_status = 'error: '.$e->getMessage();
		}

		$this->response(array(
			'status' => 'ok',
			'service' => 'ops-api',
			'database' => $database_status,
		), 200);
	}
}
