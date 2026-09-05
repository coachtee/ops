<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /api/visits/ + POST /api/visits/{id}/photo/ — see docs/API_CONTRACT.md's
 * "Standard CRUD resources" and "Visit photo attachment". The photo itself
 * travels as a second, separate multipart phase (JSON push can't carry
 * binary) — see photo_post() below, mirroring expense receipts exactly. */
class Visits extends Api_Controller {

	public function __construct()
	{
		parent::__construct();
		$this->load->model('Visit_model');
	}

	public function index_get($id = NULL)
	{
		if ($id === NULL)
		{
			$rows = $this->Visit_model->all_for_business($this->business_id);
			$this->response(array_map(array($this, 'to_wire'), $rows), 200);
		}

		$row = $this->Visit_model->find($id, $this->business_id);
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
		$this->Visit_model->insert_row($id, $this->business_id, $this->Visit_model->from_wire($body));
		$this->response($this->to_wire($this->Visit_model->find($id, $this->business_id)), 201);
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
		$updated = $this->Visit_model->update_row($id, $this->business_id, $this->Visit_model->from_wire($body));
		if (!$updated)
		{
			$this->response(array('detail' => 'Not found.'), 404);
		}
		$this->response($this->to_wire($this->Visit_model->find($id, $this->business_id)), 200);
	}

	/** multipart/form-data, field 'photo', <=10MB, must decode as an image.
	 * Requires the visit to already exist under the caller's business —
	 * 404 otherwise, same tenant-scoped lookup as every other endpoint. */
	public function photo_post($id = NULL)
	{
		if ($id === NULL || $this->Visit_model->find($id, $this->business_id) === NULL)
		{
			$this->response(array('detail' => 'Not found.'), 404);
		}

		$url = $this->_store_upload('photo', "visits/{$id}/photo");
		$this->Visit_model->set_photo_url($id, $this->business_id, $url);
		$this->response($this->to_wire($this->Visit_model->find($id, $this->business_id)), 200);
	}

	/** Shared by Visits::photo_post() and Expenses::receipt_post() — kept
	 * private per-controller (rather than a shared helper) since it's this
	 * short and each caller's error-field name differs. */
	private function _store_upload($field, $relative_dir)
	{
		if (empty($_FILES[$field]) || $_FILES[$field]['error'] !== UPLOAD_ERR_OK)
		{
			$this->response(array('errors' => array($field => array('This field is required.'))), 400);
		}
		$file = $_FILES[$field];
		if ($file['size'] > 10 * 1024 * 1024)
		{
			$this->response(array('errors' => array($field => array('File too large (max 10MB).'))), 400);
		}
		$info = @getimagesize($file['tmp_name']);
		if ($info === FALSE)
		{
			$this->response(array('errors' => array($field => array('Not a valid image.'))), 400);
		}
		$ext = image_type_to_extension($info[2]);
		$dir = FCPATH . "uploads/{$this->business_id}/{$relative_dir}/";
		if (!is_dir($dir))
		{
			mkdir($dir, 0755, TRUE);
		}
		$filename = uuid4() . $ext;
		move_uploaded_file($file['tmp_name'], $dir.$filename);
		return base_url("uploads/{$this->business_id}/{$relative_dir}/{$filename}");
	}

	private function to_wire(array $row)
	{
		return array_merge(
			array('id' => $row['id']),
			$this->Visit_model->to_wire($row),
			array(
				'created_at' => iso8601($row['created_at']),
				'updated_at' => iso8601($row['updated_at']),
				'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			),
		);
	}
}
