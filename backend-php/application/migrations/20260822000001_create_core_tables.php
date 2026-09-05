<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * businesses/users/memberships (the accounts app's tables in the Django
 * backend this replaces) plus the first ported resource, customers — see
 * backend/accounts/models.py and backend/crm/models.py for the exact
 * shapes this mirrors. Every syncable resource's id is CHAR(36) and
 * client-generated (see docs/API_CONTRACT.md's sync protocol); businesses/
 * users/memberships are server-only rows, not synced, but use the same
 * id shape for consistency.
 */
class Migration_Create_core_tables extends CI_Migration {

	public function up()
	{
		$this->dbforge->add_field(array(
			'id' => array('type' => 'CHAR', 'constraint' => 36),
			'name' => array('type' => 'VARCHAR', 'constraint' => 255),
			'trading_name' => array('type' => 'VARCHAR', 'constraint' => 255, 'default' => ''),
			'registration_number' => array('type' => 'VARCHAR', 'constraint' => 100, 'default' => ''),
			'tax_number' => array('type' => 'VARCHAR', 'constraint' => 100, 'default' => ''),
			'vat_number' => array('type' => 'VARCHAR', 'constraint' => 100, 'default' => ''),
			'is_vat_registered' => array('type' => 'TINYINT', 'constraint' => 1, 'default' => 0),
			'phone' => array('type' => 'VARCHAR', 'constraint' => 30, 'default' => ''),
			'email' => array('type' => 'VARCHAR', 'constraint' => 255, 'default' => ''),
			'address_line1' => array('type' => 'VARCHAR', 'constraint' => 255, 'default' => ''),
			'address_line2' => array('type' => 'VARCHAR', 'constraint' => 255, 'default' => ''),
			'suburb' => array('type' => 'VARCHAR', 'constraint' => 100, 'default' => ''),
			'city' => array('type' => 'VARCHAR', 'constraint' => 100, 'default' => ''),
			// Two-letter SA province code (EC/FS/GP/KZN/LP/MP/NC/NW/WC) — see
			// backend/accounts/models.py's PROVINCE_CHOICES.
			'province' => array('type' => 'VARCHAR', 'constraint' => 3, 'default' => ''),
			'postal_code' => array('type' => 'VARCHAR', 'constraint' => 10, 'default' => ''),
			'industry' => array('type' => 'VARCHAR', 'constraint' => 50, 'default' => 'other'),
			'logo_url' => array('type' => 'VARCHAR', 'constraint' => 500, 'null' => TRUE),
			'created_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'updated_at' => array('type' => 'DATETIME', 'constraint' => 6),
		));
		$this->dbforge->add_key('id', TRUE);
		$this->dbforge->create_table('businesses');

		$this->dbforge->add_field(array(
			'id' => array('type' => 'CHAR', 'constraint' => 36),
			'email' => array('type' => 'VARCHAR', 'constraint' => 255),
			'password_hash' => array('type' => 'VARCHAR', 'constraint' => 255),
			'first_name' => array('type' => 'VARCHAR', 'constraint' => 100, 'default' => ''),
			'last_name' => array('type' => 'VARCHAR', 'constraint' => 100, 'default' => ''),
			'phone' => array('type' => 'VARCHAR', 'constraint' => 30, 'default' => ''),
			'created_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'updated_at' => array('type' => 'DATETIME', 'constraint' => 6),
		));
		$this->dbforge->add_key('id', TRUE);
		$this->dbforge->create_table('users');
		$this->db->query('ALTER TABLE users ADD UNIQUE KEY users_email_unique (email)');

		$this->dbforge->add_field(array(
			'id' => array('type' => 'CHAR', 'constraint' => 36),
			'user_id' => array('type' => 'CHAR', 'constraint' => 36),
			'business_id' => array('type' => 'CHAR', 'constraint' => 36),
			'role' => array('type' => "ENUM('owner','staff')", 'default' => 'owner'),
			'created_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'updated_at' => array('type' => 'DATETIME', 'constraint' => 6),
		));
		$this->dbforge->add_key('id', TRUE);
		$this->dbforge->create_table('memberships');
		$this->db->query('ALTER TABLE memberships ADD KEY memberships_user_id_idx (user_id)');
		$this->db->query('ALTER TABLE memberships ADD KEY memberships_business_id_idx (business_id)');

		$this->dbforge->add_field(array(
			'id' => array('type' => 'CHAR', 'constraint' => 36),
			'business_id' => array('type' => 'CHAR', 'constraint' => 36),
			'name' => array('type' => 'VARCHAR', 'constraint' => 255),
			'customer_type' => array('type' => "ENUM('individual','company')", 'default' => 'individual'),
			'phone' => array('type' => 'VARCHAR', 'constraint' => 30, 'default' => ''),
			'email' => array('type' => 'VARCHAR', 'constraint' => 255, 'default' => ''),
			'address_line1' => array('type' => 'VARCHAR', 'constraint' => 255, 'default' => ''),
			'address_line2' => array('type' => 'VARCHAR', 'constraint' => 255, 'default' => ''),
			'suburb' => array('type' => 'VARCHAR', 'constraint' => 100, 'default' => ''),
			'city' => array('type' => 'VARCHAR', 'constraint' => 100, 'default' => ''),
			'province' => array('type' => 'VARCHAR', 'constraint' => 3, 'default' => ''),
			'postal_code' => array('type' => 'VARCHAR', 'constraint' => 10, 'default' => ''),
			'notes' => array('type' => 'TEXT', 'null' => TRUE),
			'source_lead_id' => array('type' => 'CHAR', 'constraint' => 36, 'null' => TRUE),
			'created_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'updated_at' => array('type' => 'DATETIME', 'constraint' => 6),
			'deleted_at' => array('type' => 'DATETIME', 'constraint' => 6, 'null' => TRUE),
		));
		$this->dbforge->add_key('id', TRUE);
		$this->dbforge->create_table('customers');
		$this->db->query('ALTER TABLE customers ADD KEY customers_business_id_idx (business_id)');
	}

	public function down()
	{
		$this->dbforge->drop_table('customers');
		$this->dbforge->drop_table('memberships');
		$this->dbforge->drop_table('users');
		$this->dbforge->drop_table('businesses');
	}
}
