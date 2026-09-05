<?php
defined('BASEPATH') OR exit('No direct script access allowed');

class MY_Model extends CI_Model {}

/**
 * Every tenant-scoped resource model extends this — mirrors
 * BusinessOwnedModel + BusinessScopedViewSet in the Django backend this
 * replaces (see backend/common/models.py, backend/common/views.py): every
 * row belongs to exactly one business, soft-deleted (never a hard DELETE
 * via the API), id is client-generated (see docs/API_CONTRACT.md's sync
 * protocol). A subclass sets $table and, if it has more than the generic
 * shape needs, overrides to_wire()/from_wire() — every read/write here
 * stays scoped to the caller's own business, so a wrong/foreign id simply
 * doesn't match any row rather than needing an explicit ownership check
 * at every call site.
 */
class Business_owned_model extends MY_Model {

	protected $table;

	public function table_name()
	{
		return $this->table;
	}

	public function all_for_business($business_id)
	{
		return $this->db->where('business_id', $business_id)
			->where('deleted_at', NULL)
			->order_by('created_at', 'ASC')
			->get($this->table)
			->result_array();
	}

	public function find($id, $business_id)
	{
		$row = $this->db->where('id', $id)
			->where('business_id', $business_id)
			->where('deleted_at', NULL)
			->get($this->table)
			->row_array();
		return $row ?: NULL;
	}

	/** Includes soft-deleted rows — used by sync's pull (a deletion must
	 * still be reported so other devices learn about it) and by push's
	 * upsert-vs-insert decision. */
	public function find_any($id, $business_id)
	{
		$row = $this->db->where('id', $id)
			->where('business_id', $business_id)
			->get($this->table)
			->row_array();
		return $row ?: NULL;
	}

	/** created_at/updated_at default to now only if not already present in
	 * $fields — sync's push (see Sync::push_post()) sets updated_at itself
	 * to the client's own last-write-wins timestamp, which must survive
	 * here; the plain CRUD controllers (see Customers::index_post()) don't
	 * set it, so it falls back to "now" for them, same as before. */
	public function insert_row($id, $business_id, array $fields)
	{
		$now = mysql_now();
		$fields['id'] = $id;
		$fields['business_id'] = $business_id;
		$fields['created_at'] = $fields['created_at'] ?? $now;
		$fields['updated_at'] = $fields['updated_at'] ?? $now;
		$this->db->insert($this->table, $fields);
	}

	/** Returns TRUE if a row matching (id, business_id) existed to update. */
	public function update_row($id, $business_id, array $fields)
	{
		$fields['updated_at'] = mysql_now();
		$this->db->where('id', $id)->where('business_id', $business_id)->update($this->table, $fields);
		return $this->db->affected_rows() > 0;
	}

	public function soft_delete($id, $business_id)
	{
		$now = mysql_now();
		$this->db->where('id', $id)->where('business_id', $business_id)
			->update($this->table, array('deleted_at' => $now, 'updated_at' => $now));
	}

	/** Mirrors Django's `model_cls.objects.filter(id=record_id).exclude(business=business).exists()`
	 * guard in sync/services.py::apply_change() — client-generated UUIDs are
	 * assumed collision-free, but if two businesses' devices ever generated
	 * the same id, the second one must be rejected rather than silently
	 * overwriting/adopting the first business's row. */
	public function used_by_other_business($id, $business_id)
	{
		return $this->db->where('id', $id)->where('business_id !=', $business_id)
			->get($this->table)->num_rows() > 0;
	}
}
