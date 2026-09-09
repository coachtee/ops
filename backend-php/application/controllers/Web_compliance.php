<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * /compliance — an owner-managed reminder list of SARS/CIPC deadlines.
 * Nothing here files, submits, or claims to know the business's actual
 * status with SARS or CIPC; a completed_date is only ever set by the owner
 * ticking it off.
 */
class Web_compliance extends Web_resource_controller {

	protected $table = 'compliance_items';
	protected $view_dir = 'compliance';
	protected $base_path = 'compliance';
	protected $nav_key = 'compliance';
	protected $title = 'Compliance';
	protected $subtitle = 'Your own reminder list of SARS and CIPC deadlines.';
	protected $search_fields = array('title', 'notes');
	protected $search_placeholder = 'Search title or note…';
	protected $status_field = 'category';
	protected $status_options = array(
		'vat_return' => 'VAT return',
		'paye_uif_sdl' => 'PAYE / UIF / SDL',
		'provisional_tax' => 'Provisional tax',
		'cipc_annual_return' => 'CIPC annual return',
		'other' => 'Other',
	);
	protected $order_by = 'due_date';
	protected $order_dir = 'ASC';

	protected function decorate(array $rows)
	{
		$today = date('Y-m-d');
		foreach ($rows as &$row)
		{
			$row['category_label'] = $this->status_options[$row['category']] ?? $row['category'];
			if (!empty($row['completed_date']))
			{
				$row['state'] = 'done';
			}
			elseif ($row['due_date'] < $today)
			{
				$row['state'] = 'overdue';
			}
			elseif ($row['due_date'] <= date('Y-m-d', strtotime('+14 days')))
			{
				$row['state'] = 'due_soon';
			}
			else
			{
				$row['state'] = 'upcoming';
			}
		}
		return $rows;
	}

	protected function extra_index_data()
	{
		$today = date('Y-m-d');

		$overdue = $this->db->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->where('completed_date', NULL)->where('due_date <', $today)
			->count_all_results('compliance_items');

		$soon = $this->db->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->where('completed_date', NULL)->where('due_date >=', $today)
			->where('due_date <=', date('Y-m-d', strtotime('+30 days')))
			->count_all_results('compliance_items');

		return array('overdue_count' => $overdue, 'due_soon_count' => $soon);
	}
}
