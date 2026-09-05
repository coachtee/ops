<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * POST /api/sync/push/, GET /api/sync/pull/ — see docs/API_CONTRACT.md's
 * "Sync" section (unchanged by this rewrite): this is the Android app's
 * real read/write path, not the per-resource CRUD controllers. Every
 * model in config('sync_registry') is a Business_owned_model subclass
 * exposing from_wire()/to_wire()/to_sync_change() — see Customer_model
 * for the pattern every future resource follows.
 */
class Sync extends Api_Controller {

	private $registry;

	public function __construct()
	{
		parent::__construct();
		$this->config->load('sync_registry');
		$this->registry = $this->config->item('sync_registry');
		foreach ($this->registry as $model_class)
		{
			$this->load->model($model_class);
		}
	}

	private function model_for($key)
	{
		$class = $this->registry[$key] ?? NULL;
		if ($class === NULL)
		{
			return NULL;
		}
		return $this->{$class};
	}

	public function push_post()
	{
		$body = json_decode($this->input->raw_input_stream, TRUE) ?: array();
		$changes = $body['changes'] ?? array();
		$results = array();

		$this->db->trans_begin();

		foreach ($changes as $change)
		{
			$model = $this->model_for($change['model'] ?? '');
			if ($model === NULL)
			{
				$results[] = array(
					'model' => $change['model'] ?? null,
					'id' => $change['id'] ?? null,
					'status' => 'error',
					'errors' => array('model' => array('Unknown model.')),
				);
				continue;
			}

			$id = $change['id'];
			$incoming_updated_at = mysql_datetime_from_iso($change['updated_at']);
			$existing = $model->find_any($id, $this->business_id);

			// Last-write-wins, string comparison is safe here since both
			// sides are the same fixed-width, zero-padded 'Y-m-d H:i:s.u'
			// format — see docs/API_CONTRACT.md's "Sync" acceptance rule.
			if ($existing !== NULL && $existing['updated_at'] >= $incoming_updated_at)
			{
				$results[] = array(
					'model' => $change['model'],
					'id' => $id,
					'status' => 'conflict',
					'server_record' => $model->to_sync_change($existing),
				);
				continue;
			}

			$fields = $model->from_wire($change['fields'] ?? array());
			if (!empty($change['deleted_at']))
			{
				$fields['deleted_at'] = mysql_datetime_from_iso($change['deleted_at']);
			}
			$fields['updated_at'] = $incoming_updated_at;

			if ($existing === NULL)
			{
				$model->insert_row($id, $this->business_id, $fields);
			}
			else
			{
				$this->db->where('id', $id)->where('business_id', $this->business_id)
					->update($model->table_name(), $fields);
			}

			$results[] = array(
				'model' => $change['model'],
				'id' => $id,
				'status' => 'accepted',
				'server_record' => $model->to_sync_change($model->find_any($id, $this->business_id)),
			);
		}

		if ($this->db->trans_status() === FALSE)
		{
			$this->db->trans_rollback();
			$this->response(array('detail' => 'Push failed, no changes were applied.'), 500);
		}
		$this->db->trans_commit();

		$this->response(array('results' => $results), 200);
	}

	public function pull_get()
	{
		// Captured before the query runs, per API_CONTRACT.md's "server_time
		// is captured before the query runs and must be used as the next
		// since — this avoids missing a row written during the request."
		$server_time = mysql_now();
		$since = $this->input->get('since');

		$changes = array();
		foreach ($this->registry as $key => $model_class)
		{
			$model = $this->{$model_class};
			$this->db->where('business_id', $this->business_id);
			if ($since)
			{
				$this->db->where('updated_at >', mysql_datetime_from_iso($since));
			}
			$rows = $this->db->get($model->table_name())->result_array();
			foreach ($rows as $row)
			{
				$changes[] = $model->to_sync_change($row);
			}
		}

		$this->response(array(
			'server_time' => iso8601($server_time),
			'changes' => $changes,
		), 200);
	}
}
