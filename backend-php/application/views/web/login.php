<!doctype html>
<html lang="en">
<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<meta name="color-scheme" content="light dark">
	<title>Log in · OPS</title>
	<link rel="icon" href="<?= base_url('assets/web/favicon.svg') ?>" type="image/svg+xml">
	<link rel="stylesheet" href="<?= base_url('assets/web/app.css') ?>">
	<script>(function(){try{var t=localStorage.getItem('ops-theme');if(t){document.documentElement.setAttribute('data-theme',t);}}catch(e){}})();</script>
</head>
<body>
<div class="auth">
	<?php $this->load->view('web/partials/auth_aside'); ?>

	<main class="auth-main">
		<div class="auth-card">
			<div class="auth-mobile-brand"><span class="logo-mark">O</span> OPS</div>

			<h1>Welcome back</h1>
			<p class="auth-sub">Log in to your business dashboard.</p>

			<?php if (!empty($error)): ?>
				<div class="alert alert-danger mb-3"><?= ops_icon('alert-triangle') ?><div><?= html_escape($error) ?></div></div>
			<?php endif; ?>

			<form method="post" action="<?= site_url('login') ?>">
				<input type="hidden" name="csrf_token" value="<?= html_escape($csrf_token) ?>">

				<div class="field">
					<label class="field-label" for="email">Email</label>
					<input class="input" type="email" id="email" name="email" value="<?= html_escape($email ?? '') ?>"
					       autocomplete="username" required autofocus>
				</div>

				<div class="field">
					<label class="field-label" for="password">Password</label>
					<input class="input" type="password" id="password" name="password"
					       autocomplete="current-password" required>
				</div>

				<button type="submit" class="btn btn-primary btn-lg btn-block mt-2">Log in</button>
			</form>

			<p class="auth-alt">
				No account yet? <a href="<?= site_url('register') ?>">Set up your business</a>
			</p>
		</div>
	</main>
</div>
</body>
</html>
