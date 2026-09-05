<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * chriskacerguis/codeigniter-restserver config. Used only for its HTTP-verb
 * method dispatch (index_get/index_post/...) and JSON formatting — auth is
 * fully custom (see application/core/MY_Controller.php's Api_Controller,
 * which checks a JWT before a request ever reaches a resource controller),
 * so every one of this library's own auth mechanisms is disabled here.
 */
$config['rest_auth'] = FALSE;
$config['rest_ip_whitelist_enabled'] = FALSE;
$config['rest_enable_keys'] = FALSE;
$config['rest_enable_logging'] = FALSE;
$config['rest_enable_limits'] = FALSE;
$config['rest_language'] = 'english';
$config['rest_default_format'] = 'json';
$config['rest_status_field_name'] = 'status';
$config['rest_message_field_name'] = 'error';
