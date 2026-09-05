<!doctype html>
<html lang="en">
<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title><?= isset($page_title) ? html_escape($page_title).' · ' : '' ?>OPS</title>
	<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
	<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
	<link rel="stylesheet" href="<?= base_url('assets/web/style.css') ?>">
</head>
<body>

<nav class="ops-sidebar">
	<a href="<?= site_url('dashboard') ?>" class="ops-sidebar-brand">
		<span class="ops-logo-mark">O</span>
		<span>OPS</span>
	</a>
	<div class="ops-sidebar-nav">
		<div class="ops-sidebar-section">Overview</div>
		<a href="<?= site_url('dashboard') ?>" class="<?= $active_nav === 'dashboard' ? 'active' : '' ?>"><i class="fa-solid fa-gauge-high"></i> Dashboard</a>

		<div class="ops-sidebar-section">Sales</div>
		<a href="<?= site_url('leads') ?>" class="<?= $active_nav === 'leads' ? 'active' : '' ?>"><i class="fa-solid fa-bullseye"></i> Leads</a>
		<a href="<?= site_url('customers') ?>" class="<?= $active_nav === 'customers' ? 'active' : '' ?>"><i class="fa-solid fa-users"></i> Customers</a>
		<a href="<?= site_url('quotes') ?>" class="<?= $active_nav === 'quotes' ? 'active' : '' ?>"><i class="fa-solid fa-file-lines"></i> Quotes</a>

		<div class="ops-sidebar-section">Operations</div>
		<a href="<?= site_url('jobs') ?>" class="<?= $active_nav === 'jobs' ? 'active' : '' ?>"><i class="fa-solid fa-briefcase"></i> Jobs</a>
		<a href="<?= site_url('invoices') ?>" class="<?= $active_nav === 'invoices' ? 'active' : '' ?>"><i class="fa-solid fa-file-invoice-dollar"></i> Invoices</a>
	</div>
	<div class="ops-sidebar-nav" style="flex: 0;">
		<a href="<?= site_url('logout') ?>"><i class="fa-solid fa-right-from-bracket"></i> Log out</a>
	</div>
</nav>

<div class="ops-main">
	<div class="ops-topbar">
		<h1><?= isset($page_title) ? html_escape($page_title) : 'Dashboard' ?></h1>
		<div class="ops-user-chip">
			<span class="text-muted"><?= html_escape($business['name'] ?? '') ?></span>
			<span class="ops-avatar"><?= strtoupper(substr($business['name'] ?? '?', 0, 1)) ?></span>
		</div>
	</div>
	<div class="ops-content">
		<?= $content_html ?>
	</div>
</div>

</body>
</html>
