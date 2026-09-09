<?php
defined('BASEPATH') OR exit('No direct script access allowed');

use chriskacerguis\RestServer\RestController;

/**
 * Every authenticated endpoint extends this — mirrors BusinessScopedViewSet
 * in the Django backend this replaces (see backend/common/views.py): verify
 * the bearer token, resolve the caller's single business via Membership
 * (V1 assumption unchanged — see docs/DISCOVERY.md), and expose $this->
 * business_id/$this->user_id to the resource controller. A 401 is returned
 * (request never reaches the resource controller) for a missing, malformed,
 * expired, or wrong-type token — no endpoint below this class ever has to
 * re-check auth itself.
 */
class Api_Controller extends RestController {

	protected $business_id;
	protected $user_id;

	public function __construct()
	{
		parent::__construct();
		$this->load->library('Auth_lib');
		$this->load->model('Membership_model');

		$token = $this->auth_lib->bearer_token_from_request();
		$payload = $token ? $this->auth_lib->verify($token, 'access') : FALSE;

		if ($payload === FALSE)
		{
			// response() with its default $continue=false calls exit()
			// internally — execution genuinely stops here, the requested
			// controller method is never invoked.
			$this->response(array('detail' => 'Authentication credentials were not provided or are invalid.'), 401);
		}

		$membership = $this->Membership_model->for_user($payload->user_id);
		if ($membership === NULL)
		{
			$this->response(array('detail' => 'This user has no business membership.'), 401);
		}

		$this->user_id = $payload->user_id;
		$this->business_id = $membership['business_id'];
	}
}

/** register/login/refresh/health — the only endpoints reachable without a
 * token, per docs/API_CONTRACT.md's "Auth" section (unchanged by this
 * rewrite). */
class Public_Api_Controller extends RestController {

	public function __construct()
	{
		parent::__construct();
	}
}

/**
 * Base for the Perfex-CRM-style web admin panel (application/views/web/*,
 * application/controllers/Web_*.php) — a completely separate login/session
 * from the JWT the Android app uses (see Api_Controller above), same as how
 * Perfex CRM itself keeps its web panel login apart from any API/module
 * auth. A plain PHP session (CI3's built-in session library), not a token:
 * this is a server-rendered, cookie-based admin UI for the business owner
 * to view their data in a browser, not something a mobile client talks to.
 */
class Web_Controller extends CI_Controller {

	protected $business_id;
	protected $user_id;
	protected $business;

	public function __construct()
	{
		parent::__construct();
		$this->load->library('session');
		$this->load->helper('url');
		$this->load->helper('web');

		$business_id = $this->session->userdata('business_id');
		if (!$business_id)
		{
			redirect('login');
		}

		$this->load->model('Business_model');
		$business = $this->Business_model->find($business_id);
		if ($business === NULL)
		{
			$this->session->sess_destroy();
			redirect('login');
		}

		$this->business_id = $business_id;
		$this->user_id = $this->session->userdata('user_id');
		$this->business = $business;
	}

	/** Renders $view inside application/views/web/layout.php's shared
	 * sidebar/topbar chrome — every web page but the login/landing pages
	 * uses this. $data['crumbs'] is a list of ['label' => .., 'url' => ..]
	 * (last one has no url); $data['active_nav'] matches a key in
	 * config/web_nav.php. */
	protected function render($view, array $data = array())
	{
		$this->config->load('web_nav', TRUE);

		$data['business'] = $this->business;
		$data['nav'] = $this->config->item('web_nav', 'web_nav');
		$data['active_nav'] = $data['active_nav'] ?? '';
		$data['crumbs'] = $data['crumbs'] ?? array();
		$data['nav_counts'] = $data['nav_counts'] ?? $this->nav_counts();
		$data['search_q'] = $data['search_q'] ?? '';
		$data['content_html'] = $this->load->view('web/'.$view, $data, TRUE);
		$this->load->view('web/layout', $data);
	}

	/**
	 * Small badge counts in the sidebar — only for the two things that are
	 * genuinely "waiting on you": leads that haven't been contacted and
	 * invoices with money still outstanding. A count on every nav item
	 * would just be decoration.
	 */
	protected function nav_counts()
	{
		$new_leads = $this->db->where('business_id', $this->business_id)
			->where('deleted_at', NULL)
			->where_in('status', array('new', 'contacted'))
			->count_all_results('leads');

		$unpaid = $this->db->where('business_id', $this->business_id)
			->where('deleted_at', NULL)
			->where_not_in('status', array('draft', 'paid', 'cancelled'))
			->count_all_results('invoices');

		return array('leads' => $new_leads, 'invoices' => $unpaid);
	}

	/**
	 * id => name for a set of ids, in ONE query. Every list page shows a
	 * related record's name (an invoice's customer, a payslip's employee);
	 * looking each one up per row is the classic N+1 that makes a 25-row
	 * page fire 26 queries.
	 */
	protected function name_map($table, array $ids, $column = 'name')
	{
		$ids = array_values(array_unique(array_filter($ids)));
		if (empty($ids))
		{
			return array();
		}
		$rows = $this->db->select('id, '.$column.' AS label')
			->where('business_id', $this->business_id)
			->where_in('id', $ids)
			->get($table)->result_array();

		$map = array();
		foreach ($rows as $row)
		{
			$map[$row['id']] = $row['label'];
		}
		return $map;
	}
}

/**
 * Every module list page wants the same four things — text search, a status
 * filter, pagination and a tenant-scoped detail lookup — so they live here
 * once. A subclass declares what it is (table, columns to search, status
 * vocabulary) and gets the whole list page; it only writes code for what's
 * genuinely specific to it (extra columns to decorate, its own detail view).
 */
abstract class Web_resource_controller extends Web_Controller {

	/** @var string database table this module lists */
	protected $table;
	/** @var string views/web/<dir>/index.php */
	protected $view_dir;
	/** @var string site_url() path, also the base for filter/pager links */
	protected $base_path;
	/** @var string key in config/web_nav.php */
	protected $nav_key;
	protected $title = '';
	protected $subtitle = '';
	/** @var string[] columns a search term is matched against */
	protected $search_fields = array('name');
	/**
	 * Lets a search term also match a related record's name — typing
	 * "Acme" on the Invoices page should find Acme's invoices, which is
	 * what an owner actually types, not the invoice number they'd have to
	 * look up first. Shape: ['column' => 'customer_id', 'table' =>
	 * 'customers', 'field' => 'name'].
	 */
	protected $search_related = NULL;
	protected $search_placeholder = 'Search…';
	/** @var array value => label; drives the filter chips */
	protected $status_options = array();
	/** Column the filter chips filter on — 'status' for most modules, but
	 * expenses filter by category and customers by type. */
	protected $status_field = 'status';
	protected $order_by = 'created_at';
	protected $order_dir = 'DESC';
	protected $per_page = 25;

	public function index()
	{
		$q = trim((string) $this->input->get('q'));
		$status = (string) $this->input->get('status');
		$page = max(1, (int) $this->input->get('page'));

		if (!array_key_exists($status, $this->status_options))
		{
			$status = '';
		}

		$this->apply_scope($q, $status);
		// FALSE keeps the builder state so the same WHERE clauses feed the
		// row query below instead of being rebuilt (and drifting) by hand.
		$total = $this->db->count_all_results($this->table, FALSE);

		$pages = max(1, (int) ceil($total / $this->per_page));
		$page = min($page, $pages);

		// get() takes no table here on purpose: count_all_results(…, FALSE)
		// left the FROM clause in place, and naming the table again would
		// produce "FROM leads, leads".
		$rows = $this->db->order_by($this->order_by, $this->order_dir)
			->limit($this->per_page, ($page - 1) * $this->per_page)
			->get()->result_array();

		$this->render($this->view_dir.'/index', array_merge(array(
			'page_title' => $this->title,
			'active_nav' => $this->nav_key,
			'crumbs' => array(array('label' => $this->title)),
			'title' => $this->title,
			'subtitle' => $this->subtitle,
			'rows' => $this->decorate($rows),
			'total' => $total,
			'page' => $page,
			'pages' => $pages,
			'q' => $q,
			'status' => $status,
			'status_options' => $this->status_options,
			'search_placeholder' => $this->search_placeholder,
			'base_path' => $this->base_path,
			'carry' => array_filter(array('status' => $status)),
		), $this->extra_index_data()));
	}

	/** Tenant scope + soft-delete + search + status, shared by the count and
	 * the row query. */
	protected function apply_scope($q, $status)
	{
		$this->db->where('business_id', $this->business_id)->where('deleted_at', NULL);

		if ($q !== '' && (!empty($this->search_fields) || $this->search_related))
		{
			$this->db->group_start();
			$first = TRUE;
			foreach ($this->search_fields as $field)
			{
				$first ? $this->db->like($field, $q) : $this->db->or_like($field, $q);
				$first = FALSE;
			}
			if ($this->search_related)
			{
				$r = $this->search_related;
				$sub = $r['column'].' IN (SELECT id FROM '.$r['table']
					.' WHERE business_id = '.$this->db->escape($this->business_id)
					.' AND '.$r['field'].' LIKE '.$this->db->escape('%'.$q.'%').')';
				$first ? $this->db->where($sub, NULL, FALSE) : $this->db->or_where($sub, NULL, FALSE);
				$first = FALSE;
			}
			$this->db->group_end();
		}

		if ($status !== '')
		{
			$this->db->where($this->status_field, $status);
		}
	}

	/** Hook: attach related names/derived values to the rows about to be
	 * rendered. Default is a no-op. */
	protected function decorate(array $rows)
	{
		return $rows;
	}

	/** Hook: extra view data for the index page (summary totals, etc.). */
	protected function extra_index_data()
	{
		return array();
	}

	/** Tenant-scoped single record, or a 404 — never leak another business's
	 * row by id. */
	protected function find_or_404($id)
	{
		$row = $this->db->where('id', $id)
			->where('business_id', $this->business_id)
			->where('deleted_at', NULL)
			->get($this->table)->row_array();

		if ($row === NULL)
		{
			show_404();
		}
		return $row;
	}
}
