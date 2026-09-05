<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * Every remaining syncable resource from docs/API_CONTRACT.md, plus
 * document_sequences (the MySQL equivalent of backend/common/models.py's
 * DocumentSequence — atomic per-business/per-doc-type counters used to
 * assign quote/job/invoice numbers). Column shapes mirror the Django
 * models this replaces (see backend/crm, backend/sales, backend/work,
 * backend/finance, backend/people, backend/compliance's models.py) —
 * every syncable table gets the same id/business_id/created_at/updated_at/
 * deleted_at shape as customers (see 20260822000001).
 */
class Migration_Create_resource_tables extends CI_Migration {

	public function up()
	{
		$this->dbforge->add_field(array(
			'id' => array('type' => 'CHAR', 'constraint' => 36),
			'business_id' => array('type' => 'CHAR', 'constraint' => 36),
			'name' => array('type' => 'VARCHAR', 'constraint' => 255),
			'phone' => array('type' => 'VARCHAR', 'constraint' => 30, 'default' => ''),
			'email' => array('type' => 'VARCHAR', 'constraint' => 255, 'default' => ''),
			'source' => array('type' => "ENUM('whatsapp','call','facebook','website','email','referral','walkin','tender','other')", 'default' => 'other'),
			'enquiry' => array('type' => 'TEXT', 'null' => TRUE),
			'notes' => array('type' => 'TEXT', 'null' => TRUE),
			'status' => array('type' => "ENUM('new','contacted','quoted','converted','lost')", 'default' => 'new'),
			'follow_up_date' => array('type' => 'DATE', 'null' => TRUE),
			'converted_customer_id' => array('type' => 'CHAR', 'constraint' => 36, 'null' => TRUE),
			'created_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'updated_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'deleted_at' => array('type' => 'DATETIME', 'constraint' => 6, 'null' => TRUE),
		));
		$this->dbforge->add_key('id', TRUE);
		$this->dbforge->create_table('leads');
		$this->db->query('ALTER TABLE leads ADD KEY leads_business_id_idx (business_id)');

		$this->dbforge->add_field(array(
			'id' => array('type' => 'CHAR', 'constraint' => 36),
			'business_id' => array('type' => 'CHAR', 'constraint' => 36),
			'customer_id' => array('type' => 'CHAR', 'constraint' => 36),
			'lead_id' => array('type' => 'CHAR', 'constraint' => 36, 'null' => TRUE),
			'number' => array('type' => 'VARCHAR', 'constraint' => 20, 'null' => TRUE),
			'status' => array('type' => "ENUM('draft','sent','accepted','declined','expired')", 'default' => 'draft'),
			'issue_date' => array('type' => 'DATE'),
			'valid_until' => array('type' => 'DATE', 'null' => TRUE),
			'notes' => array('type' => 'TEXT', 'null' => TRUE),
			'terms' => array('type' => 'TEXT', 'null' => TRUE),
			'is_vat_applicable' => array('type' => 'TINYINT', 'constraint' => 1, 'default' => 1),
			'discount_amount' => array('type' => 'DECIMAL', 'constraint' => '12,2', 'default' => 0),
			'subtotal' => array('type' => 'DECIMAL', 'constraint' => '12,2', 'default' => 0),
			'vat_amount' => array('type' => 'DECIMAL', 'constraint' => '12,2', 'default' => 0),
			'total' => array('type' => 'DECIMAL', 'constraint' => '12,2', 'default' => 0),
			'sent_at' => array('type' => 'DATETIME', 'constraint' => 6, 'null' => TRUE),
			'accepted_at' => array('type' => 'DATETIME', 'constraint' => 6, 'null' => TRUE),
			'declined_at' => array('type' => 'DATETIME', 'constraint' => 6, 'null' => TRUE),
			'created_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'updated_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'deleted_at' => array('type' => 'DATETIME', 'constraint' => 6, 'null' => TRUE),
		));
		$this->dbforge->add_key('id', TRUE);
		$this->dbforge->create_table('quotes');
		$this->db->query('ALTER TABLE quotes ADD KEY quotes_business_id_idx (business_id)');

		$this->dbforge->add_field(array(
			'id' => array('type' => 'CHAR', 'constraint' => 36),
			'business_id' => array('type' => 'CHAR', 'constraint' => 36),
			'quote_id' => array('type' => 'CHAR', 'constraint' => 36),
			'description' => array('type' => 'VARCHAR', 'constraint' => 255),
			'quantity' => array('type' => 'DECIMAL', 'constraint' => '10,2', 'default' => 1),
			'unit_price' => array('type' => 'DECIMAL', 'constraint' => '12,2', 'default' => 0),
			'line_total' => array('type' => 'DECIMAL', 'constraint' => '12,2', 'default' => 0),
			'sort_order' => array('type' => 'INT', 'constraint' => 10, 'unsigned' => TRUE, 'default' => 0),
			'created_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'updated_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'deleted_at' => array('type' => 'DATETIME', 'constraint' => 6, 'null' => TRUE),
		));
		$this->dbforge->add_key('id', TRUE);
		$this->dbforge->create_table('quote_line_items');
		$this->db->query('ALTER TABLE quote_line_items ADD KEY quote_line_items_business_id_idx (business_id)');
		$this->db->query('ALTER TABLE quote_line_items ADD KEY quote_line_items_quote_id_idx (quote_id)');

		$this->dbforge->add_field(array(
			'id' => array('type' => 'CHAR', 'constraint' => 36),
			'business_id' => array('type' => 'CHAR', 'constraint' => 36),
			'customer_id' => array('type' => 'CHAR', 'constraint' => 36),
			'quote_id' => array('type' => 'CHAR', 'constraint' => 36, 'null' => TRUE),
			'number' => array('type' => 'VARCHAR', 'constraint' => 20, 'null' => TRUE),
			'title' => array('type' => 'VARCHAR', 'constraint' => 255),
			'description' => array('type' => 'TEXT', 'null' => TRUE),
			'status' => array('type' => "ENUM('not_started','in_progress','completed','cancelled')", 'default' => 'not_started'),
			'start_date' => array('type' => 'DATE', 'null' => TRUE),
			'due_date' => array('type' => 'DATE', 'null' => TRUE),
			'completed_date' => array('type' => 'DATE', 'null' => TRUE),
			'created_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'updated_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'deleted_at' => array('type' => 'DATETIME', 'constraint' => 6, 'null' => TRUE),
		));
		$this->dbforge->add_key('id', TRUE);
		$this->dbforge->create_table('jobs');
		$this->db->query('ALTER TABLE jobs ADD KEY jobs_business_id_idx (business_id)');

		$this->dbforge->add_field(array(
			'id' => array('type' => 'CHAR', 'constraint' => 36),
			'business_id' => array('type' => 'CHAR', 'constraint' => 36),
			'job_id' => array('type' => 'CHAR', 'constraint' => 36),
			'employee_id' => array('type' => 'CHAR', 'constraint' => 36, 'null' => TRUE),
			'scheduled_date' => array('type' => 'DATE'),
			'start_time' => array('type' => 'TIME', 'null' => TRUE),
			'end_time' => array('type' => 'TIME', 'null' => TRUE),
			'status' => array('type' => "ENUM('scheduled','en_route','in_progress','completed','cancelled','needs_follow_up')", 'default' => 'scheduled'),
			'notes' => array('type' => 'TEXT', 'null' => TRUE),
			'started_at' => array('type' => 'DATETIME', 'constraint' => 6, 'null' => TRUE),
			'completed_at' => array('type' => 'DATETIME', 'constraint' => 6, 'null' => TRUE),
			'photo_url' => array('type' => 'VARCHAR', 'constraint' => 500, 'null' => TRUE),
			'created_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'updated_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'deleted_at' => array('type' => 'DATETIME', 'constraint' => 6, 'null' => TRUE),
		));
		$this->dbforge->add_key('id', TRUE);
		$this->dbforge->create_table('visits');
		$this->db->query('ALTER TABLE visits ADD KEY visits_business_id_idx (business_id)');
		$this->db->query('ALTER TABLE visits ADD KEY visits_job_id_idx (job_id)');

		$this->dbforge->add_field(array(
			'id' => array('type' => 'CHAR', 'constraint' => 36),
			'business_id' => array('type' => 'CHAR', 'constraint' => 36),
			'customer_id' => array('type' => 'CHAR', 'constraint' => 36),
			'job_id' => array('type' => 'CHAR', 'constraint' => 36, 'null' => TRUE),
			'quote_id' => array('type' => 'CHAR', 'constraint' => 36, 'null' => TRUE),
			'number' => array('type' => 'VARCHAR', 'constraint' => 20, 'null' => TRUE),
			'status' => array('type' => "ENUM('draft','sent','partially_paid','paid','overdue','cancelled')", 'default' => 'draft'),
			'issue_date' => array('type' => 'DATE'),
			'due_date' => array('type' => 'DATE', 'null' => TRUE),
			'notes' => array('type' => 'TEXT', 'null' => TRUE),
			'terms' => array('type' => 'TEXT', 'null' => TRUE),
			'is_vat_applicable' => array('type' => 'TINYINT', 'constraint' => 1, 'default' => 1),
			'discount_amount' => array('type' => 'DECIMAL', 'constraint' => '12,2', 'default' => 0),
			'subtotal' => array('type' => 'DECIMAL', 'constraint' => '12,2', 'default' => 0),
			'vat_amount' => array('type' => 'DECIMAL', 'constraint' => '12,2', 'default' => 0),
			'total' => array('type' => 'DECIMAL', 'constraint' => '12,2', 'default' => 0),
			'amount_paid' => array('type' => 'DECIMAL', 'constraint' => '12,2', 'default' => 0),
			'sent_at' => array('type' => 'DATETIME', 'constraint' => 6, 'null' => TRUE),
			'created_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'updated_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'deleted_at' => array('type' => 'DATETIME', 'constraint' => 6, 'null' => TRUE),
		));
		$this->dbforge->add_key('id', TRUE);
		$this->dbforge->create_table('invoices');
		$this->db->query('ALTER TABLE invoices ADD KEY invoices_business_id_idx (business_id)');

		$this->dbforge->add_field(array(
			'id' => array('type' => 'CHAR', 'constraint' => 36),
			'business_id' => array('type' => 'CHAR', 'constraint' => 36),
			'invoice_id' => array('type' => 'CHAR', 'constraint' => 36),
			'description' => array('type' => 'VARCHAR', 'constraint' => 255),
			'quantity' => array('type' => 'DECIMAL', 'constraint' => '10,2', 'default' => 1),
			'unit_price' => array('type' => 'DECIMAL', 'constraint' => '12,2', 'default' => 0),
			'line_total' => array('type' => 'DECIMAL', 'constraint' => '12,2', 'default' => 0),
			'sort_order' => array('type' => 'INT', 'constraint' => 10, 'unsigned' => TRUE, 'default' => 0),
			'created_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'updated_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'deleted_at' => array('type' => 'DATETIME', 'constraint' => 6, 'null' => TRUE),
		));
		$this->dbforge->add_key('id', TRUE);
		$this->dbforge->create_table('invoice_line_items');
		$this->db->query('ALTER TABLE invoice_line_items ADD KEY invoice_line_items_business_id_idx (business_id)');
		$this->db->query('ALTER TABLE invoice_line_items ADD KEY invoice_line_items_invoice_id_idx (invoice_id)');

		$this->dbforge->add_field(array(
			'id' => array('type' => 'CHAR', 'constraint' => 36),
			'business_id' => array('type' => 'CHAR', 'constraint' => 36),
			'customer_id' => array('type' => 'CHAR', 'constraint' => 36),
			'invoice_id' => array('type' => 'CHAR', 'constraint' => 36, 'null' => TRUE),
			'amount' => array('type' => 'DECIMAL', 'constraint' => '12,2'),
			'method' => array('type' => "ENUM('cash','eft','card','snapscan','other')", 'default' => 'eft'),
			'reference' => array('type' => 'VARCHAR', 'constraint' => 100, 'default' => ''),
			'paid_date' => array('type' => 'DATE'),
			'notes' => array('type' => 'TEXT', 'null' => TRUE),
			'created_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'updated_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'deleted_at' => array('type' => 'DATETIME', 'constraint' => 6, 'null' => TRUE),
		));
		$this->dbforge->add_key('id', TRUE);
		$this->dbforge->create_table('payments');
		$this->db->query('ALTER TABLE payments ADD KEY payments_business_id_idx (business_id)');
		$this->db->query('ALTER TABLE payments ADD KEY payments_invoice_id_idx (invoice_id)');

		$this->dbforge->add_field(array(
			'id' => array('type' => 'CHAR', 'constraint' => 36),
			'business_id' => array('type' => 'CHAR', 'constraint' => 36),
			'name' => array('type' => 'VARCHAR', 'constraint' => 255),
			'contact_person' => array('type' => 'VARCHAR', 'constraint' => 255, 'default' => ''),
			'phone' => array('type' => 'VARCHAR', 'constraint' => 30, 'default' => ''),
			'email' => array('type' => 'VARCHAR', 'constraint' => 255, 'default' => ''),
			'notes' => array('type' => 'TEXT', 'null' => TRUE),
			'created_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'updated_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'deleted_at' => array('type' => 'DATETIME', 'constraint' => 6, 'null' => TRUE),
		));
		$this->dbforge->add_key('id', TRUE);
		$this->dbforge->create_table('suppliers');
		$this->db->query('ALTER TABLE suppliers ADD KEY suppliers_business_id_idx (business_id)');

		$this->dbforge->add_field(array(
			'id' => array('type' => 'CHAR', 'constraint' => 36),
			'business_id' => array('type' => 'CHAR', 'constraint' => 36),
			'supplier_id' => array('type' => 'CHAR', 'constraint' => 36, 'null' => TRUE),
			'job_id' => array('type' => 'CHAR', 'constraint' => 36, 'null' => TRUE),
			'category' => array('type' => "ENUM('materials_stock','fuel_travel','tools_equipment','rent','utilities','insurance','bank_charges','professional_fees','marketing','telephone_internet','vehicle','repairs_maintenance','wages_subcontractors','other')", 'default' => 'other'),
			'description' => array('type' => 'VARCHAR', 'constraint' => 255, 'default' => ''),
			'amount' => array('type' => 'DECIMAL', 'constraint' => '12,2'),
			'is_vat_applicable' => array('type' => 'TINYINT', 'constraint' => 1, 'default' => 0),
			'vat_amount' => array('type' => 'DECIMAL', 'constraint' => '12,2', 'default' => 0),
			'date' => array('type' => 'DATE'),
			'receipt_image_url' => array('type' => 'VARCHAR', 'constraint' => 500, 'null' => TRUE),
			'created_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'updated_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'deleted_at' => array('type' => 'DATETIME', 'constraint' => 6, 'null' => TRUE),
		));
		$this->dbforge->add_key('id', TRUE);
		$this->dbforge->create_table('expenses');
		$this->db->query('ALTER TABLE expenses ADD KEY expenses_business_id_idx (business_id)');

		$this->dbforge->add_field(array(
			'id' => array('type' => 'CHAR', 'constraint' => 36),
			'business_id' => array('type' => 'CHAR', 'constraint' => 36),
			'name' => array('type' => 'VARCHAR', 'constraint' => 255),
			'role' => array('type' => 'VARCHAR', 'constraint' => 255, 'default' => ''),
			'phone' => array('type' => 'VARCHAR', 'constraint' => 30, 'default' => ''),
			'email' => array('type' => 'VARCHAR', 'constraint' => 255, 'default' => ''),
			'pay_rate_type' => array('type' => "ENUM('hourly','daily','monthly')", 'default' => 'monthly'),
			'pay_rate' => array('type' => 'DECIMAL', 'constraint' => '12,2', 'default' => 0),
			'start_date' => array('type' => 'DATE', 'null' => TRUE),
			'notes' => array('type' => 'TEXT', 'null' => TRUE),
			'created_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'updated_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'deleted_at' => array('type' => 'DATETIME', 'constraint' => 6, 'null' => TRUE),
		));
		$this->dbforge->add_key('id', TRUE);
		$this->dbforge->create_table('employees');
		$this->db->query('ALTER TABLE employees ADD KEY employees_business_id_idx (business_id)');

		$this->dbforge->add_field(array(
			'id' => array('type' => 'CHAR', 'constraint' => 36),
			'business_id' => array('type' => 'CHAR', 'constraint' => 36),
			'employee_id' => array('type' => 'CHAR', 'constraint' => 36),
			'period_start' => array('type' => 'DATE'),
			'period_end' => array('type' => 'DATE'),
			'gross_pay' => array('type' => 'DECIMAL', 'constraint' => '12,2'),
			'deductions' => array('type' => 'DECIMAL', 'constraint' => '12,2', 'default' => 0),
			'deductions_note' => array('type' => 'VARCHAR', 'constraint' => 255, 'default' => ''),
			'net_pay' => array('type' => 'DECIMAL', 'constraint' => '12,2', 'default' => 0),
			'paid_date' => array('type' => 'DATE', 'null' => TRUE),
			'notes' => array('type' => 'TEXT', 'null' => TRUE),
			'created_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'updated_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'deleted_at' => array('type' => 'DATETIME', 'constraint' => 6, 'null' => TRUE),
		));
		$this->dbforge->add_key('id', TRUE);
		$this->dbforge->create_table('payslips');
		$this->db->query('ALTER TABLE payslips ADD KEY payslips_business_id_idx (business_id)');
		$this->db->query('ALTER TABLE payslips ADD KEY payslips_employee_id_idx (employee_id)');

		$this->dbforge->add_field(array(
			'id' => array('type' => 'CHAR', 'constraint' => 36),
			'business_id' => array('type' => 'CHAR', 'constraint' => 36),
			'category' => array('type' => "ENUM('vat_return','paye_uif_sdl','provisional_tax','cipc_annual_return','other')", 'default' => 'other'),
			'title' => array('type' => 'VARCHAR', 'constraint' => 255),
			'due_date' => array('type' => 'DATE'),
			'completed_date' => array('type' => 'DATE', 'null' => TRUE),
			'is_recurring' => array('type' => 'TINYINT', 'constraint' => 1, 'default' => 1),
			'notes' => array('type' => 'TEXT', 'null' => TRUE),
			'created_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'updated_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'deleted_at' => array('type' => 'DATETIME', 'constraint' => 6, 'null' => TRUE),
		));
		$this->dbforge->add_key('id', TRUE);
		$this->dbforge->create_table('compliance_items');
		$this->db->query('ALTER TABLE compliance_items ADD KEY compliance_items_business_id_idx (business_id)');

		// MySQL equivalent of DocumentSequence.objects.select_for_update() —
		// see Document_sequence_model::next_number(), which uses
		// INSERT ... ON DUPLICATE KEY UPDATE last_number = LAST_INSERT_ID(last_number + 1)
		// for an atomic increment-and-fetch without needing an explicit lock.
		$this->dbforge->add_field(array(
			'id' => array('type' => 'INT', 'constraint' => 10, 'unsigned' => TRUE, 'auto_increment' => TRUE),
			'business_id' => array('type' => 'CHAR', 'constraint' => 36),
			'doc_type' => array('type' => 'VARCHAR', 'constraint' => 20),
			'last_number' => array('type' => 'INT', 'constraint' => 10, 'unsigned' => TRUE, 'default' => 0),
		));
		$this->dbforge->add_key('id', TRUE);
		$this->dbforge->create_table('document_sequences');
		$this->db->query('ALTER TABLE document_sequences ADD UNIQUE KEY document_sequences_business_doc_type_unique (business_id, doc_type)');
	}

	public function down()
	{
		$this->dbforge->drop_table('document_sequences');
		$this->dbforge->drop_table('compliance_items');
		$this->dbforge->drop_table('payslips');
		$this->dbforge->drop_table('employees');
		$this->dbforge->drop_table('expenses');
		$this->dbforge->drop_table('suppliers');
		$this->dbforge->drop_table('payments');
		$this->dbforge->drop_table('invoice_line_items');
		$this->dbforge->drop_table('invoices');
		$this->dbforge->drop_table('visits');
		$this->dbforge->drop_table('jobs');
		$this->dbforge->drop_table('quote_line_items');
		$this->dbforge->drop_table('quotes');
		$this->dbforge->drop_table('leads');
	}
}
