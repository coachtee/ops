<!doctype html>
<html lang="en">
<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<meta name="color-scheme" content="light dark">
	<title>Set up your business · OPS</title>
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

			<h1>Set up your business</h1>
			<p class="auth-sub">Four details now — the rest you can fill in from the app.</p>

			<?php if (!empty($errors['form'])): ?>
				<div class="alert alert-danger mb-3"><?= ops_icon('alert-triangle') ?><div><?= html_escape($errors['form']) ?></div></div>
			<?php endif; ?>

			<form method="post" action="<?= site_url('register') ?>">
				<input type="hidden" name="csrf_token" value="<?= html_escape($csrf_token) ?>">

				<div class="field">
					<label class="field-label" for="business_name">Business name</label>
					<input class="input<?= isset($errors['business_name']) ? ' is-error' : '' ?>" type="text"
					       id="business_name" name="business_name" value="<?= html_escape($values['business_name']) ?>"
					       placeholder="e.g. Thabo's Plumbing" required autofocus>
					<?php if (isset($errors['business_name'])): ?>
						<div class="field-error"><?= html_escape($errors['business_name']) ?></div>
					<?php endif; ?>
				</div>

				<div class="form-grid">
					<div class="field">
						<label class="field-label" for="first_name">First name</label>
						<input class="input<?= isset($errors['first_name']) ? ' is-error' : '' ?>" type="text"
						       id="first_name" name="first_name" value="<?= html_escape($values['first_name']) ?>"
						       autocomplete="given-name" required>
						<?php if (isset($errors['first_name'])): ?>
							<div class="field-error"><?= html_escape($errors['first_name']) ?></div>
						<?php endif; ?>
					</div>
					<div class="field">
						<label class="field-label" for="last_name">Last name</label>
						<input class="input" type="text" id="last_name" name="last_name"
						       value="<?= html_escape($values['last_name']) ?>" autocomplete="family-name">
					</div>
				</div>

				<div class="field">
					<label class="field-label" for="email">Email</label>
					<input class="input<?= isset($errors['email']) ? ' is-error' : '' ?>" type="email"
					       id="email" name="email" value="<?= html_escape($values['email']) ?>"
					       autocomplete="username" required>
					<?php if (isset($errors['email'])): ?>
						<div class="field-error"><?= html_escape($errors['email']) ?></div>
					<?php else: ?>
						<div class="field-hint">You'll use this to log in here and on the phone.</div>
					<?php endif; ?>
				</div>

				<div class="field">
					<label class="field-label" for="password">Password</label>
					<input class="input<?= isset($errors['password']) ? ' is-error' : '' ?>" type="password"
					       id="password" name="password" autocomplete="new-password" required minlength="8">
					<?php if (isset($errors['password'])): ?>
						<div class="field-error"><?= html_escape($errors['password']) ?></div>
					<?php else: ?>
						<div class="field-hint">At least 8 characters.</div>
					<?php endif; ?>
				</div>

				<button type="submit" class="btn btn-primary btn-lg btn-block mt-2">Create business account</button>
			</form>

			<p class="auth-alt">
				Already set up? <a href="<?= site_url('login') ?>">Log in</a>
			</p>
		</div>
	</main>
</div>
</body>
</html>
