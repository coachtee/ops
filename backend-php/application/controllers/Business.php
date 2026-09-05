<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** GET/PATCH /api/business/me/ — see docs/API_CONTRACT.md. Logo upload
 * (multipart) is a later milestone, matching how the Django backend's own
 * receipt/photo attachments were built as a second pass — not needed for
 * this vertical slice's proof of the auth+CRUD+sync pattern. */
class Business extends Api_Controller {

	public function __construct()
	{
		parent::__construct();
		$this->load->model('Business_model');
	}

	public function me_get()
	{
		$this->response($this->Business_model->to_wire($this->Business_model->find($this->business_id)), 200);
	}

	public function me_patch()
	{
		$body = json_decode($this->input->raw_input_stream, TRUE) ?: array();
		unset($body['id'], $body['created_at'], $body['updated_at'], $body['logo']);
		$this->Business_model->update($this->business_id, $body);
		$this->response($this->Business_model->to_wire($this->Business_model->find($this->business_id)), 200);
	}
}
