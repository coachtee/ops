<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /api/quotes/ — see docs/API_CONTRACT.md's "Standard CRUD resources".
 * number/subtotal/vat_amount/total are server-derived and never accepted
 * from the request body here (Quote_model::from_wire() already excludes
 * them) — direct create/update does NOT run the number-assignment or
 * line-item-total recompute that Sync::push_post() does, matching
 * API_CONTRACT.md's framing that sync is the app's real read/write path. */
class Quotes extends Api_Controller {

	public function __construct()
	{
		parent::__construct();
		$this->load->model('Quote_model');
	}

	public function index_get($id = NULL)
	{
		if ($id === NULL)
		{
			$rows = $this->Quote_model->all_for_business($this->business_id);
			$this->response(array_map(array($this, 'to_wire'), $rows), 200);
		}

		$row = $this->Quote_model->find($id, $this->business_id);
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
		$this->Quote_model->insert_row($id, $this->business_id, $this->Quote_model->from_wire($body));
		$this->response($this->to_wire($this->Quote_model->find($id, $this->business_id)), 201);
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
		$updated = $this->Quote_model->update_row($id, $this->business_id, $this->Quote_model->from_wire($body));
		if (!$updated)
		{
			$this->response(array('detail' => 'Not found.'), 404);
		}
		$this->response($this->to_wire($this->Quote_model->find($id, $this->business_id)), 200);
	}

	private function to_wire(array $row)
	{
		return array_merge(
			array('id' => $row['id']),
			$this->Quote_model->to_wire($row),
			array(
				'created_at' => iso8601($row['created_at']),
				'updated_at' => iso8601($row['updated_at']),
				'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			),
		);
	}
}
