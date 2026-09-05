<?php
defined('BASEPATH') OR exit('No direct script access allowed');
require_once __DIR__.'/env.php';

/**
 * OPS backend — Perfex CRM's stack (PHP + CodeIgniter 3 + MySQL/MariaDB).
 * Every value overridable by environment variable, same "zero-config local
 * dev, explicit env vars for anything else" convention the Django backend
 * used (see ../../backend/ops/settings.py, the backend this replaces).
 * See env.php's doc comment for why this goes through ops_env() rather
 * than a plain getenv() call.
 */
$active_group = 'default';
$query_builder = TRUE;

$db['default'] = array(
	'dsn'	=> '',
	'hostname' => ops_env('OPS_DB_HOST', 'localhost'),
	'username' => ops_env('OPS_DB_USER', 'ops'),
	'password' => ops_env('OPS_DB_PASSWORD', 'ops'),
	'database' => ops_env('OPS_DB_NAME', 'ops_ci'),
	'dbdriver' => 'mysqli',
	'dbprefix' => '',
	'pconnect' => FALSE,
	'db_debug' => (ENVIRONMENT !== 'production'),
	'cache_on' => FALSE,
	'cachedir' => '',
	'char_set' => 'utf8mb4',
	'dbcollat' => 'utf8mb4_unicode_ci',
	'swap_pre' => '',
	'encrypt' => FALSE,
	'compress' => FALSE,
	'stricton' => FALSE,
	'failover' => array(),
	'save_queries' => TRUE,
);
