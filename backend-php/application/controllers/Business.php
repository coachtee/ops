<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** GET/PATCH /api/business/me/ — see docs/API_CONTRACT.md. A text-only PATCH
 * arrives as ordinary JSON; a PATCH that also replaces the logo arrives as
 * multipart/form-data (see OpsApiService.updateBusinessWithLogo() /
 * BusinessRepository.updateProfileWithLogo() on the Android side) — PHP
 * doesn't parse multipart bodies for anything but POST, so that branch is
 * hand-parsed via multipart_helper.php. */
class Business extends Api_Controller {

	public function __construct()
	{
		parent::__construct();
		$this->load->model('Business_model');
		$this->load->helper('multipart');
	}

	public function me_get()
	{
		$this->response($this->Business_model->to_wire($this->Business_model->find($this->business_id)), 200);
	}

	public function me_patch()
	{
		$content_type = $_SERVER['CONTENT_TYPE'] ?? '';

		if (stripos($content_type, 'multipart/form-data') !== FALSE)
		{
			list($fields, $files) = parse_multipart_form_data(file_get_contents('php://input'), $content_type);
			if (isset($fields['is_vat_registered']))
			{
				$fields['is_vat_registered'] = in_array(strtolower($fields['is_vat_registered']), array('true', '1'), TRUE) ? 1 : 0;
			}
			if (!empty($files['logo']))
			{
				$info = @getimagesize($files['logo']['tmp_name']);
				if ($info === FALSE)
				{
					@unlink($files['logo']['tmp_name']);
					$this->response(array('errors' => array('logo' => array('Not a valid image.'))), 400);
				}
				$ext = image_type_to_extension($info[2]);
				$dir = FCPATH."uploads/{$this->business_id}/logo/";
				if (!is_dir($dir))
				{
					mkdir($dir, 0755, TRUE);
				}
				$filename = uuid4().$ext;
				rename($files['logo']['tmp_name'], $dir.$filename);
				$fields['logo_url'] = base_url("uploads/{$this->business_id}/logo/{$filename}");
			}
		}
		else
		{
			$fields = json_decode($this->input->raw_input_stream, TRUE) ?: array();
		}

		unset($fields['id'], $fields['created_at'], $fields['updated_at'], $fields['logo']);
		$this->Business_model->update($this->business_id, $fields);
		$this->response($this->Business_model->to_wire($this->Business_model->find($this->business_id)), 200);
	}
}
