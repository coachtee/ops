<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** /suppliers, /suppliers/{id} — a contact record plus everything bought
 * from them (their expenses), not a separate purchase ledger. */
class Web_suppliers extends Web_resource_controller {

	protected $table = 'suppliers';
	protected $view_dir = 'suppliers';
	protected $base_path = 'suppliers';
	protected $nav_key = 'suppliers';
	protected $title = 'Suppliers';
	protected $subtitle = 'Who you buy from.';
	protected $search_fields = array('name', 'contact_person', 'phone', 'email');
	protected $search_placeholder = 'Search name, contact, phone…';
	protected $order_by = 'name';
	protected $order_dir = 'ASC';

	protected function decorate(array $rows)
	{
		$ids = array_column($rows, 'id');
		if (empty($ids))
		{
			return $rows;
		}

		$totals = $this->db->select('supplier_id, COALESCE(SUM(amount), 0) AS spent, COUNT(*) AS n')
			->where('business_id', $this->business_id)->where('deleted_at', NULL)
			->where_in('supplier_id', $ids)->group_by('supplier_id')
			->get('expenses')->result_array();

		$by_supplier = array();
		foreach ($totals as $row)
		{
			$by_supplier[$row['supplier_id']] = $row;
		}
		foreach ($rows as &$row)
		{
			$row['spent'] = $by_supplier[$row['id']]['spent'] ?? '0.00';
			$row['expense_count'] = $by_supplier[$row['id']]['n'] ?? 0;
		}
		return $rows;
	}

	public function show($id)
	{
		$supplier = $this->find_or_404($id);

		$expenses = $this->db->where('business_id', $this->business_id)->where('supplier_id', $id)
			->where('deleted_at', NULL)->order_by('date', 'DESC')->limit(50)->get('expenses')->result_array();

		$spent = $this->db->select('COALESCE(SUM(amount), 0) AS total')
			->where('business_id', $this->business_id)->where('supplier_id', $id)
			->where('deleted_at', NULL)->get('expenses')->row_array();

		$this->render('suppliers/show', array(
			'page_title' => $supplier['name'],
			'active_nav' => 'suppliers',
			'crumbs' => array(array('label' => 'Suppliers', 'url' => 'suppliers'), array('label' => $supplier['name'])),
			'supplier' => $supplier,
			'expenses' => $expenses,
			'spent' => $spent['total'],
		));
	}
}
