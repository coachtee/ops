<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/** Mirrors backend/finance/models.py's InvoiceLineItem — see
 * docs/API_CONTRACT.md's "invoice_line_item" field payload. Same
 * server-derived-line_total pattern as Quote_line_item_model. */
class Invoice_line_item_model extends Business_owned_model {

	protected $table = 'invoice_line_items';

	public function from_wire(array $fields)
	{
		return array(
			'invoice_id' => $fields['invoice_id'],
			'description' => $fields['description'],
			'quantity' => $fields['quantity'] ?? '1.00',
			'unit_price' => $fields['unit_price'] ?? '0.00',
			'sort_order' => $fields['sort_order'] ?? 0,
		);
	}

	public function to_wire(array $row)
	{
		return array(
			'invoice_id' => $row['invoice_id'],
			'description' => $row['description'],
			'quantity' => $row['quantity'],
			'unit_price' => $row['unit_price'],
			'line_total' => $row['line_total'],
			'sort_order' => (int) $row['sort_order'],
		);
	}

	public function to_sync_change(array $row)
	{
		return array(
			'model' => 'invoice_line_item',
			'id' => $row['id'],
			'updated_at' => iso8601($row['updated_at']),
			'deleted_at' => $row['deleted_at'] ? iso8601($row['deleted_at']) : NULL,
			'fields' => $this->to_wire($row),
		);
	}
}
