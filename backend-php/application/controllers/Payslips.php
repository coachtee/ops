<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /api/payslips/ — see docs/API_CONTRACT.md's "Standard CRUD resources".
 * net_pay is computed inline (gross_pay - deductions) on every write, and
 * the deductions<=gross_pay / period_end>=period_start rules are enforced
 * here too (see Payslip_model::validate()), matching Sync::push_post(). */
class Payslips extends Api_Controller {

	public function __construct()
	{
		parent::__construct();
		$this->load->model('Payslip_model');
	}

	public function index_get($id = NULL)
	{
		if ($id === NULL)
		{
			$rows = $this->Payslip_model->all_for_business($this->business_id);
			$this->response(array_map(array($this, 'to_wire'), $rows), 200);
		}

		$row = $this->Payslip_model->find($id, $this->business_id);
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
		$fields = $this->Payslip_model->from_wire($body);
		$errors = $this->Payslip_model->validate($fields);
		if (!empty($errors))
		{
			$this->response(array('errors' => $errors), 400);
		}
		$fields['net_pay'] = money_compute_net_pay($fields['gross_pay'], $fields['deductions']);
		$this->Payslip_model->insert_row($id, $this->business_id, $fields);
		$this->response($this->to_wire($this->Payslip_model->find($id, $this->business_id)), 201);
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
		$fields = $this->Payslip_model->from_wire($body);
		$errors = $this->Payslip_model->validate($fields);
		if (!empty($errors))
		{
			$this->response(array('errors' => $errors), 400);
		}
		$fields['net_pay'] = money_compute_net_pay($fields['gross_pay'], $fields['deductions']);
		$updated = $this->Payslip_model->update_row($id, $this->business_id, $fields);
		if (!$updated)
		{
			$this->response(array('detail' => 'Not found.'), 404);
		}
		$this->response($this->to_wire($this->Payslip_model->find($id, $this->business_id)), 200);
	}

	private function to_wire(array $row)
	{
		return array_merge(
			array('id' => $row['id']),
			$this->Payslip_model->to_wire($row),
			array(
				'created_at' => iso8601($row['created_at']),
				'updated_at' => iso8601($row['updated_at']),
				'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			),
		);
	}
}
