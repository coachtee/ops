<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/*
| -------------------------------------------------------------------------
| URI ROUTING
| -------------------------------------------------------------------------
| This file lets you re-map URI requests to specific controller functions.
|
| Typically there is a one-to-one relationship between a URL string
| and its corresponding controller class/method. The segments in a
| URL normally follow this pattern:
|
|	example.com/class/method/id/
|
| In some instances, however, you may want to remap this relationship
| so that a different class/function is called than the one
| corresponding to the URL.
|
| Please see the user guide for complete details:
|
|	https://codeigniter.com/userguide3/general/routing.html
|
| -------------------------------------------------------------------------
| RESERVED ROUTES
| -------------------------------------------------------------------------
|
| There are three reserved routes:
|
|	$route['default_controller'] = 'welcome';
|
| This route indicates which controller class should be loaded if the
| URI contains no data. In the above example, the "welcome" class
| would be loaded.
|
|	$route['404_override'] = 'errors/page_missing';
|
| This route will tell the Router which controller/method to use if those
| provided in the URL cannot be matched to a valid route.
|
|	$route['translate_uri_dashes'] = FALSE;
|
| This is not exactly a route, but allows you to automatically route
| controller and method names that contain dashes. '-' isn't a valid
| class or method name character, so it requires translation.
| When you set this option to TRUE, it will replace ALL dashes in the
| controller and method URI segments.
|
| Examples:	my-controller/index	-> my_controller/index
|		my-controller/my-method	-> my_controller/my_method
*/
$route['default_controller'] = 'welcome';
$route['404_override'] = '';
$route['translate_uri_dashes'] = FALSE;

/*
 * OPS API routes — see docs/API_CONTRACT.md. Paths match the contract
 * byte-for-byte (including trailing slashes, which CI3 ignores by default
 * but Android's Retrofit interface still sends) so this rewrite is a pure
 * implementation swap behind the same wire contract.
 */
$route['api/health'] = 'health/index';
$route['api/auth/register'] = 'auth/register';
$route['api/auth/login'] = 'auth/login';
$route['api/auth/refresh'] = 'auth/refresh';
$route['api/business/me'] = 'business/me';
$route['api/customers'] = 'customers/index';
$route['api/customers/(:any)'] = 'customers/index/$1';
$route['api/sync/push'] = 'sync/push';
$route['api/sync/pull'] = 'sync/pull';
