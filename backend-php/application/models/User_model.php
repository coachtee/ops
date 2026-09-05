<?php
defined('BASEPATH') OR exit('No direct script access allowed');

class User_model extends CI_Model {

	public function find_by_email($email)
	{
		return $this->db->get_where('users', array('email' => $email))->row_array();
	}

	public function find($id)
	{
		return $this->db->get_where('users', array('id' => $id))->row_array();
	}

	public function email_taken($email)
	{
		return $this->db->where('email', $email)->count_all_results('users') > 0;
	}

	public function create($email, $password, $first_name, $last_name, $phone = '')
	{
		$now = mysql_now();
		$id = uuid4();
		$this->db->insert('users', array(
			'id' => $id,
			'email' => $email,
			'password_hash' => password_hash($password, PASSWORD_DEFAULT),
			'first_name' => $first_name,
			'last_name' => $last_name,
			'phone' => $phone,
			'created_at' => $now,
			'updated_at' => $now,
		));
		return $id;
	}

	public function verify_password($user_row, $password)
	{
		return password_verify($password, $user_row['password_hash']);
	}

	/** Mirrors UserDto.kt exactly. */
	public function to_wire(array $row)
	{
		return array(
			'id' => $row['id'],
			'email' => $row['email'],
			'first_name' => $row['first_name'],
			'last_name' => $row['last_name'],
			'phone' => $row['phone'],
		);
	}
}
