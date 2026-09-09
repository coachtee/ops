<!doctype html>
<html lang="en">
<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<meta name="color-scheme" content="light dark">
	<title><?= isset($page_title) ? html_escape($page_title).' · ' : '' ?>OPS</title>
	<link rel="icon" href="<?= base_url('assets/web/favicon.svg') ?>" type="image/svg+xml">
	<link rel="stylesheet" href="<?= base_url('assets/web/app.css') ?>">
	<!-- Applied before first paint so a dark-mode user never sees a white flash. -->
	<script>(function(){try{var t=localStorage.getItem('ops-theme');if(t){document.documentElement.setAttribute('data-theme',t);}}catch(e){}})();</script>
</head>
<body>

<div class="shell">
	<div class="nav-scrim" data-nav-close></div>

	<nav class="sidebar" aria-label="Main">
		<a href="<?= site_url('dashboard') ?>" class="sidebar-brand">
			<span class="logo-mark">O</span>
			<span>OPS</span>
		</a>

		<div class="sidebar-scroll">
			<?php foreach ($nav as $group): ?>
				<?php if (!empty($group['section'])): ?>
					<div class="nav-section"><?= html_escape($group['section']) ?></div>
				<?php endif; ?>
				<?php foreach ($group['items'] as $item): ?>
					<a href="<?= site_url($item['route']) ?>" class="nav-item<?= $active_nav === $item['key'] ? ' active' : '' ?>">
						<?= ops_icon($item['icon']) ?>
						<span><?= html_escape($item['label']) ?></span>
						<?php if (!empty($nav_counts[$item['key']])): ?>
							<span class="nav-count"><?= (int) $nav_counts[$item['key']] ?></span>
						<?php endif; ?>
					</a>
				<?php endforeach; ?>
			<?php endforeach; ?>
		</div>

		<div class="sidebar-foot">
			<a href="<?= site_url('settings') ?>" class="nav-item<?= $active_nav === 'settings' ? ' active' : '' ?>">
				<?= ops_icon('settings') ?><span>Settings</span>
			</a>
			<a href="<?= site_url('logout') ?>" class="nav-item">
				<?= ops_icon('log-out') ?><span>Log out</span>
			</a>
		</div>
	</nav>

	<div class="main">
		<header class="topbar">
			<button type="button" class="icon-btn nav-toggle" data-nav-open aria-label="Open menu"><?= ops_icon('menu') ?></button>

			<nav class="crumbs" aria-label="Breadcrumb">
				<a href="<?= site_url('dashboard') ?>">Home</a>
				<?php foreach (($crumbs ?? array()) as $crumb): ?>
					<span class="sep"><?= ops_icon('chevron-right') ?></span>
					<?php if (!empty($crumb['url'])): ?>
						<a href="<?= site_url($crumb['url']) ?>"><?= html_escape($crumb['label']) ?></a>
					<?php else: ?>
						<span class="current truncate"><?= html_escape($crumb['label']) ?></span>
					<?php endif; ?>
				<?php endforeach; ?>
			</nav>

			<div class="topbar-spacer"></div>

			<form class="search" method="get" action="<?= site_url('search') ?>" role="search">
				<?= ops_icon('search') ?>
				<input type="search" name="q" placeholder="Search customers, quotes, invoices…"
				       value="<?= html_escape($search_q ?? '') ?>" aria-label="Search">
			</form>

			<!-- Below 640px the input is hidden (it squeezes to ~90px between the
			     hamburger and the avatar); this takes its place. -->
			<a class="icon-btn search-link" href="<?= site_url('search') ?>"
			   aria-label="Search" title="Search"><?= ops_icon('search') ?></a>

			<button type="button" class="icon-btn" data-theme-toggle aria-label="Toggle dark mode" title="Toggle dark mode">
				<span data-theme-icon-light><?= ops_icon('moon') ?></span>
				<span data-theme-icon-dark class="hide"><?= ops_icon('sun') ?></span>
			</button>

			<div class="menu" data-menu>
				<button type="button" class="avatar" data-menu-toggle aria-haspopup="true" aria-expanded="false"
				        aria-label="Account menu"><?= html_escape(ops_initials($business['name'] ?? '?')) ?></button>
				<div class="menu-pop" role="menu">
					<div class="menu-head">
						<div class="t-title-sm truncate"><?= html_escape($business['name'] ?? '') ?></div>
						<div class="t-body-sm subtle"><?= html_escape($business['email'] ?: 'No email on file') ?></div>
					</div>
					<a href="<?= site_url('settings') ?>" role="menuitem"><?= ops_icon('building') ?> Business profile</a>
					<a href="<?= site_url('reports') ?>" role="menuitem"><?= ops_icon('bar-chart') ?> Reports</a>
					<div class="menu-sep"></div>
					<a href="<?= site_url('logout') ?>" role="menuitem" class="menu-danger"><?= ops_icon('log-out') ?> Log out</a>
				</div>
			</div>
		</header>

		<main class="content" id="main">
			<?= $content_html ?>
		</main>
	</div>
</div>

<script src="<?= base_url('assets/web/app.js') ?>"></script>
</body>
</html>
