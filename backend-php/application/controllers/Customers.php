<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /api/customers/ — see docs/API_CONTRACT.md's "Standard CRUD resources"
 * section: convenience reads/writes, not the app's main sync path (that's
 * Sync::push/pull). No hard delete — soft delete only happens via sync's
 * deleted_at, matching every other resource. */
class Customers extends Api_Controller {

	public function __construct()
	{
		parent::__construct();
		$this->load->model('Customer_model');
	}

	public function index_get($id = NULL)
	{
		if ($id === NULL)
		{
			$rows = $this->Customer_model->all_for_business($this->business_id);
			$this->response(array_map(array($this, 'to_wire'), $rows), 200);
		}

		$row = $this->Customer_model->find($id, $this->business_id);
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
		$this->Customer_model->insert_row($id, $this->business_id, $this->Customer_model->from_wire($body));
		$this->response($this->to_wire($this->Customer_model->find($id, $this->business_id)), 201);
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
		$updated = $this->Customer_model->update_row($id, $this->business_id, $this->Customer_model->from_wire($body));
		if (!$updated)
		{
			$this->response(array('detail' => 'Not found.'), 404);
		}
		$this->response($this->to_wire($this->Customer_model->find($id, $this->business_id)), 200);
	}

	/** Adds the DRF-standard id/created_at/updated_at/deleted_at envelope
	 * around Customer_model::to_wire()'s fields — see API_CONTRACT.md's
	 * "Field shapes are exactly the sync fields payloads... plus the
	 * DRF-standard id/created_at/updated_at/deleted_at". */
	private function to_wire(array $row)
	{
		return array_merge(
			array('id' => $row['id']),
			$this->Customer_model->to_wire($row),
			array(
				'created_at' => iso8601($row['created_at']),
				'updated_at' => iso8601($row['updated_at']),
				'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			),
		);
	}
}
