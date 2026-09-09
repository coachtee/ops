<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /search?q= — one box across the records an owner actually looks for by
 * name or number. Every query is business-scoped; a search can never
 * surface another business's record. */
class Web_search extends Web_Controller {

	public function index()
	{
		$q = trim((string) $this->input->get('q'));
		$groups = array();

		if ($q !== '')
		{
			$groups = array(
				array('label' => 'Customers', 'icon' => 'users', 'path' => 'customers',
					'rows' => $this->hunt('customers', array('name', 'phone', 'email'), $q, 'name', 'phone')),
				array('label' => 'Leads', 'icon' => 'target', 'path' => 'leads',
					'rows' => $this->hunt('leads', array('name', 'phone', 'email'), $q, 'name', 'status')),
				array('label' => 'Quotes', 'icon' => 'file-text', 'path' => 'quotes',
					'rows' => $this->hunt('quotes', array('number', 'notes'), $q, 'number', 'status')),
				array('label' => 'Invoices', 'icon' => 'receipt', 'path' => 'invoices',
					'rows' => $this->hunt('invoices', array('number', 'notes'), $q, 'number', 'status')),
				array('label' => 'Jobs', 'icon' => 'briefcase', 'path' => 'jobs',
					'rows' => $this->hunt('jobs', array('number', 'title', 'description'), $q, 'title', 'status')),
				array('label' => 'Suppliers', 'icon' => 'truck', 'path' => 'suppliers',
					'rows' => $this->hunt('suppliers', array('name', 'contact_person', 'phone'), $q, 'name', 'phone')),
				array('label' => 'Employees', 'icon' => 'badge', 'path' => 'employees',
					'rows' => $this->hunt('employees', array('name', 'role', 'phone'), $q, 'name', 'role')),
			);
			$groups = array_values(array_filter($groups, function ($group) {
				return !empty($group['rows']);
			}));
		}

		$total = 0;
		foreach ($groups as $group)
		{
			$total += count($group['rows']);
		}

		$this->render('search', array(
			'page_title' => $q !== '' ? 'Search: '.$q : 'Search',
			'active_nav' => '',
			'crumbs' => array(array('label' => 'Search')),
			'q' => $q,
			'search_q' => $q,
			'groups' => $groups,
			'total' => $total,
		));
	}

	/** Up to 6 matches per record type — enough to recognise the one you
	 * meant, few enough that the page stays scannable. */
	private function hunt($table, array $fields, $q, $title_field, $meta_field)
	{
		$this->db->where('business_id', $this->business_id)->where('deleted_at', NULL)->group_start();
		foreach ($fields as $i => $field)
		{
			$i === 0 ? $this->db->like($field, $q) : $this->db->or_like($field, $q);
		}
		$rows = $this->db->group_end()->limit(6)->get($table)->result_array();

		$out = array();
		foreach ($rows as $row)
		{
			$out[] = array(
				'id' => $row['id'],
				'title' => $row[$title_field] !== '' && $row[$title_field] !== NULL ? $row[$title_field] : '(untitled)',
				'meta' => $row[$meta_field] ?? '',
			);
		}
		return $out;
	}
}
