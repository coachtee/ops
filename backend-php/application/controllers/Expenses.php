<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /api/expenses/ + POST /api/expenses/{id}/receipt/ — see
 * docs/API_CONTRACT.md's "Standard CRUD resources" and "Expense receipt
 * attachments". vat_amount is computed inline (VAT-inclusive extraction,
 * see money_helper.php) on every write, and amount>0 / date<=tomorrow are
 * enforced here too (see Expense_model::validate()), matching
 * Sync::push_post(). */
class Expenses extends Api_Controller {

	public function __construct()
	{
		parent::__construct();
		$this->load->model('Expense_model');
	}

	public function index_get($id = NULL)
	{
		if ($id === NULL)
		{
			$rows = $this->Expense_model->all_for_business($this->business_id);
			$this->response(array_map(array($this, 'to_wire'), $rows), 200);
		}

		$row = $this->Expense_model->find($id, $this->business_id);
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
		$fields = $this->Expense_model->from_wire($body);
		$errors = $this->Expense_model->validate($fields);
		if (!empty($errors))
		{
			$this->response(array('errors' => $errors), 400);
		}
		$fields['vat_amount'] = money_extract_vat_from_inclusive($fields['amount'], $fields['is_vat_applicable']);
		$this->Expense_model->insert_row($id, $this->business_id, $fields);
		$this->response($this->to_wire($this->Expense_model->find($id, $this->business_id)), 201);
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
		$fields = $this->Expense_model->from_wire($body);
		$errors = $this->Expense_model->validate($fields);
		if (!empty($errors))
		{
			$this->response(array('errors' => $errors), 400);
		}
		$fields['vat_amount'] = money_extract_vat_from_inclusive($fields['amount'], $fields['is_vat_applicable']);
		$updated = $this->Expense_model->update_row($id, $this->business_id, $fields);
		if (!$updated)
		{
			$this->response(array('detail' => 'Not found.'), 404);
		}
		$this->response($this->to_wire($this->Expense_model->find($id, $this->business_id)), 200);
	}

	/** multipart/form-data, field 'receipt', <=10MB, must decode as an
	 * image. Requires the expense to already exist under the caller's
	 * business — 404 otherwise. Uploading bumps updated_at (see
	 * Expense_model::set_receipt_url()) so the receipt reaches other
	 * devices through the normal sync pull path. */
	public function receipt_post($id = NULL)
	{
		if ($id === NULL || $this->Expense_model->find($id, $this->business_id) === NULL)
		{
			$this->response(array('detail' => 'Not found.'), 404);
		}

		if (empty($_FILES['receipt']) || $_FILES['receipt']['error'] !== UPLOAD_ERR_OK)
		{
			$this->response(array('errors' => array('receipt' => array('This field is required.'))), 400);
		}
		$file = $_FILES['receipt'];
		if ($file['size'] > 10 * 1024 * 1024)
		{
			$this->response(array('errors' => array('receipt' => array('File too large (max 10MB).'))), 400);
		}
		$info = @getimagesize($file['tmp_name']);
		if ($info === FALSE)
		{
			$this->response(array('errors' => array('receipt' => array('Not a valid image.'))), 400);
		}
		$ext = image_type_to_extension($info[2]);
		$dir = FCPATH."uploads/{$this->business_id}/expenses/{$id}/receipt/";
		if (!is_dir($dir))
		{
			mkdir($dir, 0755, TRUE);
		}
		$filename = uuid4().$ext;
		move_uploaded_file($file['tmp_name'], $dir.$filename);
		$url = base_url("uploads/{$this->business_id}/expenses/{$id}/receipt/{$filename}");

		$this->Expense_model->set_receipt_url($id, $this->business_id, $url);
		$this->response($this->to_wire($this->Expense_model->find($id, $this->business_id)), 200);
	}

	private function to_wire(array $row)
	{
		return array_merge(
			array('id' => $row['id']),
			$this->Expense_model->to_wire($row),
			array(
				'created_at' => iso8601($row['created_at']),
				'updated_at' => iso8601($row['updated_at']),
				'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			),
		);
	}
}
