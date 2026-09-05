<!doctype html>
<html lang="en">
<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Log in · OPS</title>
	<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
	<link rel="stylesheet" href="<?= base_url('assets/web/style.css') ?>">
</head>
<body>
<div class="ops-login-wrap">
	<div class="ops-login-card">
		<div class="ops-login-brand">
			<span class="ops-logo-mark">O</span>
			<span>OPS</span>
		</div>
		<p class="text-muted mb-4">Sign in to your business dashboard.</p>

		<?php if (!empty($error)): ?>
			<div class="alert alert-danger py-2"><?= html_escape($error) ?></div>
		<?php endif; ?>

		<form method="post" action="<?= site_url('login') ?>">
			<input type="hidden" name="csrf_token" value="<?= html_escape($csrf_token) ?>">
			<div class="mb-3">
				<label class="form-label">Email</label>
				<input type="email" name="email" class="form-control" required autofocus>
			</div>
			<div class="mb-3">
				<label class="form-label">Password</label>
				<input type="password" name="password" class="form-control" required>
			</div>
			<button type="submit" class="btn btn-primary w-100">Log in</button>
		</form>
	</div>
</div>
</body>
</html>
