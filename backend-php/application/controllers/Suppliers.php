<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /api/suppliers/ — see docs/API_CONTRACT.md's "Standard CRUD resources". */
class Suppliers extends Api_Controller {

	public function __construct()
	{
		parent::__construct();
		$this->load->model('Supplier_model');
	}

	public function index_get($id = NULL)
	{
		if ($id === NULL)
		{
			$rows = $this->Supplier_model->all_for_business($this->business_id);
			$this->response(array_map(array($this, 'to_wire'), $rows), 200);
		}

		$row = $this->Supplier_model->find($id, $this->business_id);
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
		$this->Supplier_model->insert_row($id, $this->business_id, $this->Supplier_model->from_wire($body));
		$this->response($this->to_wire($this->Supplier_model->find($id, $this->business_id)), 201);
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
		$updated = $this->Supplier_model->update_row($id, $this->business_id, $this->Supplier_model->from_wire($body));
		if (!$updated)
		{
			$this->response(array('detail' => 'Not found.'), 404);
		}
		$this->response($this->to_wire($this->Supplier_model->find($id, $this->business_id)), 200);
	}

	private function to_wire(array $row)
	{
		return array_merge(
			array('id' => $row['id']),
			$this->Supplier_model->to_wire($row),
			array(
				'created_at' => iso8601($row['created_at']),
				'updated_at' => iso8601($row['updated_at']),
				'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			),
		);
	}
}
