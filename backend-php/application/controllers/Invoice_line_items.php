<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /api/invoice-line-items/ — see docs/API_CONTRACT.md's "Standard CRUD
 * resources". Same inline line_total computation as Quote_line_items,
 * without cascading into the parent invoice's totals. */
class Invoice_line_items extends Api_Controller {

	public function __construct()
	{
		parent::__construct();
		$this->load->model('Invoice_line_item_model');
	}

	public function index_get($id = NULL)
	{
		if ($id === NULL)
		{
			$rows = $this->Invoice_line_item_model->all_for_business($this->business_id);
			$this->response(array_map(array($this, 'to_wire'), $rows), 200);
		}

		$row = $this->Invoice_line_item_model->find($id, $this->business_id);
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
		$fields = $this->Invoice_line_item_model->from_wire($body);
		$fields['line_total'] = money_compute_line_total($fields['quantity'], $fields['unit_price']);
		$this->Invoice_line_item_model->insert_row($id, $this->business_id, $fields);
		$this->response($this->to_wire($this->Invoice_line_item_model->find($id, $this->business_id)), 201);
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
		$fields = $this->Invoice_line_item_model->from_wire($body);
		$fields['line_total'] = money_compute_line_total($fields['quantity'], $fields['unit_price']);
		$updated = $this->Invoice_line_item_model->update_row($id, $this->business_id, $fields);
		if (!$updated)
		{
			$this->response(array('detail' => 'Not found.'), 404);
		}
		$this->response($this->to_wire($this->Invoice_line_item_model->find($id, $this->business_id)), 200);
	}

	private function to_wire(array $row)
	{
		return array_merge(
			array('id' => $row['id']),
			$this->Invoice_line_item_model->to_wire($row),
			array(
				'created_at' => iso8601($row['created_at']),
				'updated_at' => iso8601($row['updated_at']),
				'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			),
		);
	}
}
