<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * Sidebar structure for the web admin panel, grouped the way the business
 * actually thinks about the work rather than by database table: money
 * coming in (Sales), work being done (Operations), money going out
 * (Spending), the people doing it (Team), and the deadlines that bite
 * (Compliance).
 *
 * `key` matches the $active_nav a controller passes to render(), `route`
 * is a site_url() path, `icon` a name from web_helper.php's icon set.
 */
$config['web_nav'] = array(
	array(
		'section' => NULL,
		'items' => array(
			array('key' => 'dashboard', 'label' => 'Dashboard', 'route' => 'dashboard', 'icon' => 'grid'),
		),
	),
	array(
		'section' => 'Sales',
		'items' => array(
			array('key' => 'leads', 'label' => 'Leads', 'route' => 'leads', 'icon' => 'target'),
			array('key' => 'customers', 'label' => 'Customers', 'route' => 'customers', 'icon' => 'users'),
			array('key' => 'quotes', 'label' => 'Quotes', 'route' => 'quotes', 'icon' => 'file-text'),
			array('key' => 'invoices', 'label' => 'Invoices', 'route' => 'invoices', 'icon' => 'receipt'),
			array('key' => 'payments', 'label' => 'Payments', 'route' => 'payments', 'icon' => 'credit-card'),
		),
	),
	array(
		'section' => 'Operations',
		'items' => array(
			array('key' => 'jobs', 'label' => 'Jobs', 'route' => 'jobs', 'icon' => 'briefcase'),
			array('key' => 'visits', 'label' => 'Schedule', 'route' => 'schedule', 'icon' => 'calendar'),
		),
	),
	array(
		'section' => 'Spending',
		'items' => array(
			array('key' => 'expenses', 'label' => 'Expenses', 'route' => 'expenses', 'icon' => 'wallet'),
			array('key' => 'suppliers', 'label' => 'Suppliers', 'route' => 'suppliers', 'icon' => 'truck'),
		),
	),
	array(
		'section' => 'Team',
		'items' => array(
			array('key' => 'employees', 'label' => 'Employees', 'route' => 'employees', 'icon' => 'badge'),
			array('key' => 'payslips', 'label' => 'Payslips', 'route' => 'payslips', 'icon' => 'banknote'),
		),
	),
	array(
		'section' => 'Insight',
		'items' => array(
			array('key' => 'reports', 'label' => 'Reports', 'route' => 'reports', 'icon' => 'bar-chart'),
			array('key' => 'compliance', 'label' => 'Compliance', 'route' => 'compliance', 'icon' => 'shield'),
		),
	),
);
