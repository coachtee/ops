<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /schedule — visits across every job, newest-first by scheduled date.
 * A visit is scheduling detail under a job, so there's no standalone detail
 * page: each row links through to its parent job. */
class Web_visits extends Web_resource_controller {

	protected $table = 'visits';
	protected $view_dir = 'visits';
	protected $base_path = 'schedule';
	protected $nav_key = 'visits';
	protected $title = 'Schedule';
	protected $subtitle = 'Every site visit booked against a job.';
	protected $search_fields = array('notes');
	protected $search_placeholder = 'Search visit notes…';
	protected $status_options = array(
		'scheduled' => 'Scheduled',
		'en_route' => 'En route',
		'in_progress' => 'In progress',
		'completed' => 'Completed',
		'needs_follow_up' => 'Needs follow-up',
		'cancelled' => 'Cancelled',
	);
	protected $order_by = 'scheduled_date';

	protected function decorate(array $rows)
	{
		$jobs = $this->name_map('jobs', array_column($rows, 'job_id'), 'title');
		$job_numbers = $this->name_map('jobs', array_column($rows, 'job_id'), 'number');
		$employees = $this->name_map('employees', array_column($rows, 'employee_id'));

		foreach ($rows as &$row)
		{
			$row['job_title'] = $jobs[$row['job_id']] ?? '—';
			$row['job_number'] = $job_numbers[$row['job_id']] ?? NULL;
			$row['employee_name'] = $row['employee_id'] ? ($employees[$row['employee_id']] ?? '—') : NULL;
		}
		return $rows;
	}

	protected function extra_index_data()
	{
		$upcoming = $this->db->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->where('scheduled_date >=', date('Y-m-d'))
			->where_in('status', array('scheduled', 'en_route'))
			->count_all_results('visits');

		$follow_up = $this->db->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->where('status', 'needs_follow_up')->count_all_results('visits');

		return array('upcoming_count' => $upcoming, 'follow_up_count' => $follow_up);
	}
}
