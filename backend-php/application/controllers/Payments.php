<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /api/payments/ — see docs/API_CONTRACT.md's "Standard CRUD resources".
 * Direct create/update here does NOT cascade into the parent invoice's
 * amount_paid/status recompute (that only happens via Sync::push_post()'s
 * post-batch recompute) — matches API_CONTRACT.md's framing that the sync
 * endpoints are the app's real read/write path and these are for direct
 * reads/testing only. */
class Payments extends Api_Controller {

	public function __construct()
	{
		parent::__construct();
		$this->load->model('Payment_model');
	}

	public function index_get($id = NULL)
	{
		if ($id === NULL)
		{
			$rows = $this->Payment_model->all_for_business($this->business_id);
			$this->response(array_map(array($this, 'to_wire'), $rows), 200);
		}

		$row = $this->Payment_model->find($id, $this->business_id);
		if ($row === NULL)
		{
			$this->response(array('detail' => 'Not found.'), 404);
		}
		$this->response($this->to_wire($row), 200);
	}

	public function index_post()
	{
		$body = json_decode($this->input->raw_input_stream, TRUE) ?: array();
		$id = $body['id'] ?? uuid4();
		$this->Payment_model->insert_row($id, $this->business_id, $this->Payment_model->from_wire($body));
		$this->response($this->to_wire($this->Payment_model->find($id, $this->business_id)), 201);
	}

	public function index_put($id = NULL)
	{
		$this->update($id);
	}

	public function index_patch($id = NULL)
	{
		$this->update($id);
	}

	private function update($id)
	{
		if ($id === NULL)
		{
			$this->response(array('detail' => 'Not found.'), 404);
		}
		$body = json_decode($this->input->raw_input_stream, TRUE) ?: array();
		$updated = $this->Payment_model->update_row($id, $this->business_id, $this->Payment_model->from_wire($body));
		if (!$updated)
		{
			$this->response(array('detail' => 'Not found.'), 404);
		}
		$this->response($this->to_wire($this->Payment_model->find($id, $this->business_id)), 200);
	}

	private function to_wire(array $row)
	{
		return array_merge(
			array('id' => $row['id']),
			$this->Payment_model->to_wire($row),
			array(
				'created_at' => iso8601($row['created_at']),
				'updated_at' => iso8601($row['updated_at']),
				'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			),
		);
	}
}
