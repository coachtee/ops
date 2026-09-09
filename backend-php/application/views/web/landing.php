<!doctype html>
<html lang="en">
<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<meta name="color-scheme" content="light dark">
	<title>OPS — run your trade business from your phone</title>
	<meta name="description" content="Leads, quotes, jobs, invoices, payments and expenses for small South African trade and service businesses. Works offline on the phone, syncs to a dashboard you can read on any screen.">
	<link rel="icon" href="<?= base_url('assets/web/favicon.svg') ?>" type="image/svg+xml">
	<link rel="stylesheet" href="<?= base_url('assets/web/app.css') ?>">
	<link rel="stylesheet" href="<?= base_url('assets/web/landing.css') ?>">
	<script>(function(){try{var t=localStorage.getItem('ops-theme');if(t){document.documentElement.setAttribute('data-theme',t);}}catch(e){}})();</script>
</head>
<body>

<header class="lp-nav">
	<div class="lp-nav-inner">
		<a href="<?= site_url('/') ?>" class="lp-brand"><span class="logo-mark">O</span> OPS</a>
		<nav class="lp-nav-links">
			<a href="#features">Features</a>
			<a href="#how">How it works</a>
			<a href="#modules">What's inside</a>
			<a href="#faq">FAQ</a>
		</nav>
		<div class="lp-nav-actions">
			<button type="button" class="icon-btn" data-theme-toggle aria-label="Toggle dark mode">
				<span data-theme-icon-light><?= ops_icon('moon') ?></span>
				<span data-theme-icon-dark class="hide"><?= ops_icon('sun') ?></span>
			</button>
			<a class="btn btn-ghost" href="<?= site_url('login') ?>">Log in</a>
			<a class="btn btn-primary" href="<?= site_url('register') ?>">Get started</a>
		</div>
	</div>
</header>

<main>
	<section class="lp-section lp-hero">
		<div>
			<span class="lp-eyebrow"><?= ops_icon('wifi-off') ?> Works with no signal</span>
			<h1 class="lp-h1">The paperwork side of the job, handled.</h1>
			<p class="lp-lede">
				Capture the lead at the gate, price the job in the van, invoice before you drive off —
				all on your phone, whether there's signal or not. It syncs the moment there is, and
				lands on a dashboard you can actually read on a big screen.
			</p>
			<div class="lp-hero-cta">
				<a class="btn btn-primary btn-lg" href="<?= site_url('register') ?>">Set up your business <?= ops_icon('arrow-right') ?></a>
				<a class="btn btn-outline btn-lg" href="#how">See how it works</a>
			</div>
			<p class="lp-hero-note">Rands and 15% VAT throughout. No card needed to set up.</p>
		</div>

		<?php /* A styled mock of the panel's own dashboard, built from the same
		         components — an illustration of the real UI, not a screenshot. */ ?>
		<div class="lp-preview" aria-hidden="true">
			<div class="lp-preview-bar">
				<span class="lp-dot"></span><span class="lp-dot"></span><span class="lp-dot"></span>
				<span class="lp-preview-title">OPS · Dashboard</span>
			</div>
			<div class="lp-preview-body">
				<div class="lp-mini-stats">
					<div class="lp-mini-stat">
						<div class="lp-mini-label">Money in</div>
						<div class="lp-mini-value">R 84.2k</div>
					</div>
					<div class="lp-mini-stat">
						<div class="lp-mini-label">Money out</div>
						<div class="lp-mini-value">R 31.7k</div>
					</div>
					<div class="lp-mini-stat">
						<div class="lp-mini-label">Owed to you</div>
						<div class="lp-mini-value">R 19.4k</div>
					</div>
				</div>
				<div class="lp-mini-rows">
					<div class="lp-mini-row">
						<span class="badge badge-success">Paid</span>
						<span class="grow truncate">INV-0042 · Geyser replacement</span>
						<span class="amount">R 6 900</span>
					</div>
					<div class="lp-mini-row">
						<span class="badge badge-warning">Partially paid</span>
						<span class="grow truncate">INV-0041 · Bathroom re-pipe</span>
						<span class="amount">R 12 400</span>
					</div>
					<div class="lp-mini-row">
						<span class="badge badge-info">Sent</span>
						<span class="grow truncate">Q-0018 · Kitchen install</span>
						<span class="amount">R 24 150</span>
					</div>
					<div class="lp-mini-row">
						<span class="badge badge-danger">Overdue</span>
						<span class="grow truncate">INV-0037 · Callout + parts</span>
						<span class="amount">R 2 300</span>
					</div>
				</div>
			</div>
		</div>
	</section>

	<div class="lp-strip">
		<div class="lp-strip-inner">
			<div class="lp-strip-item"><?= ops_icon('cloud-off') ?><div><strong>Offline first</strong>Capture everything with no signal; it syncs later.</div></div>
			<div class="lp-strip-item"><?= ops_icon('receipt') ?><div><strong>VAT done right</strong>15% added on quotes, extracted from expenses.</div></div>
			<div class="lp-strip-item"><?= ops_icon('zap') ?><div><strong>Numbered for you</strong>Q-0001, J-0001, INV-0001 — automatically.</div></div>
			<div class="lp-strip-item"><?= ops_icon('lock') ?><div><strong>Your data, your server</strong>Self-hosted on hosting you control.</div></div>
		</div>
	</div>

	<section class="lp-section" id="features">
		<div class="lp-section-head">
			<h2 class="lp-h2">Everything between the enquiry and the money</h2>
			<p class="lp-lede">One thread from the first phone call to the payment landing — no re-typing the same customer into three different places.</p>
		</div>

		<div class="lp-grid">
			<div class="lp-card">
				<div class="lp-card-icon"><?= ops_icon('target') ?></div>
				<h3>Leads that don't get forgotten</h3>
				<p>Log the WhatsApp enquiry or the walk-in with a follow-up date. See at a glance who's new, contacted, quoted or lost.</p>
			</div>
			<div class="lp-card">
				<div class="lp-card-icon"><?= ops_icon('file-text') ?></div>
				<h3>Quotes priced on the spot</h3>
				<p>Build line items, apply a discount, let VAT work itself out. Each quote gets its own number the first time it syncs.</p>
			</div>
			<div class="lp-card">
				<div class="lp-card-icon"><?= ops_icon('briefcase') ?></div>
				<h3>Jobs and site visits</h3>
				<p>Turn an accepted quote into a job, schedule visits against it, assign staff and mark what still needs a follow-up.</p>
			</div>
			<div class="lp-card">
				<div class="lp-card-icon"><?= ops_icon('receipt') ?></div>
				<h3>Invoices that track themselves</h3>
				<p>Record a payment and the invoice moves itself to partially paid or paid — and back again if a payment is reversed.</p>
			</div>
			<div class="lp-card">
				<div class="lp-card-icon"><?= ops_icon('wallet') ?></div>
				<h3>Expenses with the VAT split out</h3>
				<p>Snap the receipt, enter what you actually paid. The input VAT portion is worked out for your SARS records, never added on top.</p>
			</div>
			<div class="lp-card">
				<div class="lp-card-icon"><?= ops_icon('shield') ?></div>
				<h3>Deadlines you set, reminders you keep</h3>
				<p>VAT returns, PAYE, provisional tax, CIPC annual returns — your own checklist, sorted by what's due next.</p>
			</div>
		</div>
	</section>

	<section class="lp-section lp-section-sm" id="how">
		<div class="lp-section-head">
			<h2 class="lp-h2">How it actually works</h2>
			<p class="lp-lede">The phone is where the work gets captured. This panel is where you read it back.</p>
		</div>

		<div class="lp-steps">
			<div class="lp-step">
				<h3>Capture on the phone</h3>
				<p>In the van, on the roof, in a basement with no bars — the app writes to the phone first, so nothing waits on a connection.</p>
			</div>
			<div class="lp-step">
				<h3>It syncs when it can</h3>
				<p>The moment there's signal, changes go up and anything new comes down. Two devices editing the same record surface the clash instead of quietly losing one side.</p>
			</div>
			<div class="lp-step">
				<h3>Read it on a real screen</h3>
				<p>Open this panel on a laptop for the wide view: what's owed, what's overdue, where the money went, what's due at SARS.</p>
			</div>
		</div>
	</section>

	<section class="lp-dark">
		<div class="lp-section" id="modules">
			<div class="lp-section-head">
				<span class="lp-eyebrow"><?= ops_icon('grid') ?> 14 record types, one thread</span>
				<h2 class="lp-h2">What's inside</h2>
				<p class="lp-lede">Everything a small trade or service business actually keeps track of — and nothing it doesn't.</p>
			</div>

			<div class="lp-modules">
				<span class="lp-module"><?= ops_icon('target') ?> Leads</span>
				<span class="lp-module"><?= ops_icon('users') ?> Customers</span>
				<span class="lp-module"><?= ops_icon('file-text') ?> Quotes</span>
				<span class="lp-module"><?= ops_icon('file-text') ?> Quote line items</span>
				<span class="lp-module"><?= ops_icon('briefcase') ?> Jobs</span>
				<span class="lp-module"><?= ops_icon('calendar') ?> Visits</span>
				<span class="lp-module"><?= ops_icon('receipt') ?> Invoices</span>
				<span class="lp-module"><?= ops_icon('receipt') ?> Invoice line items</span>
				<span class="lp-module"><?= ops_icon('credit-card') ?> Payments</span>
				<span class="lp-module"><?= ops_icon('wallet') ?> Expenses</span>
				<span class="lp-module"><?= ops_icon('truck') ?> Suppliers</span>
				<span class="lp-module"><?= ops_icon('badge') ?> Employees</span>
				<span class="lp-module"><?= ops_icon('banknote') ?> Payslips</span>
				<span class="lp-module"><?= ops_icon('shield') ?> Compliance items</span>
			</div>
		</div>
	</section>

	<section class="lp-section" id="faq">
		<div class="lp-section-head">
			<h2 class="lp-h2">Straight answers</h2>
		</div>

		<div class="lp-faq">
			<details open>
				<summary>Does it really work without signal?</summary>
				<p>
					Yes. The phone app stores everything locally first, so capturing a lead, building a quote
					or photographing a receipt never waits on a connection. When signal comes back it pushes
					what changed and pulls anything new. If the same record was edited in two places, the clash
					is shown to you rather than one version being silently dropped.
				</p>
			</details>
			<details>
				<summary>Does it file my VAT or submit anything to SARS?</summary>
				<p>
					No — and it never claims to. It works out 15% VAT on quotes and invoices, extracts the input
					VAT portion from expenses, and shows you a VAT position for a date range so you or your
					bookkeeper have the numbers to hand. Filing stays with you. The compliance list is your own
					reminder checklist; nothing is submitted anywhere, and an item is only marked done because
					you ticked it.
				</p>
			</details>
			<details>
				<summary>Does it calculate PAYE and UIF on payslips?</summary>
				<p>
					No. You enter gross pay and deductions — whatever you or your bookkeeper worked out — and it
					records the payslip and computes net pay as gross minus deductions. It does not know SARS's
					tax tables and makes no claim of payroll-tax accuracy.
				</p>
			</details>
			<details>
				<summary>Where does my data live?</summary>
				<p>
					On hosting you control. The server side is a standard PHP and MySQL application that runs on
					ordinary shared hosting — including cPanel with nothing more than File Manager and
					phpMyAdmin. There's no third-party account in the middle holding your customer list.
				</p>
			</details>
			<details>
				<summary>Can I use the web panel to create and edit records?</summary>
				<p>
					Not yet. Today the panel is the read side — dashboards, lists, detail views, reports and
					printable quotes and invoices. Records are created and edited in the phone app, which is the
					one place that works offline. Editing from the browser is the obvious next step, not a
					finished feature, and we'd rather say so than imply otherwise.
				</p>
			</details>
			<details>
				<summary>What does it cost?</summary>
				<p>
					There's no subscription being charged through this software. You run it on your own hosting,
					so your cost is whatever that hosting costs you.
				</p>
			</details>
		</div>
	</section>

	<section class="lp-section lp-section-sm">
		<div class="lp-cta">
			<h2 class="lp-h2">Set it up in a few minutes</h2>
			<p class="lp-lede">Create the business account here, then point the phone app at this server and log in with the same details.</p>
			<div class="lp-cta-actions">
				<a class="btn btn-primary btn-lg" href="<?= site_url('register') ?>">Set up your business <?= ops_icon('arrow-right') ?></a>
				<a class="btn btn-outline btn-lg" href="<?= site_url('login') ?>">I already have an account</a>
			</div>
		</div>
	</section>
</main>

<footer class="lp-footer">
	<div class="lp-footer-inner">
		<a href="<?= site_url('/') ?>" class="lp-brand"><span class="logo-mark">O</span> OPS</a>
		<span>Built for small South African trade and service businesses.</span>
		<div class="lp-footer-links">
			<a href="#features">Features</a>
			<a href="#faq">FAQ</a>
			<a href="<?= site_url('login') ?>">Log in</a>
		</div>
	</div>
</footer>

<script src="<?= base_url('assets/web/app.js') ?>"></script>
</body>
</html>
