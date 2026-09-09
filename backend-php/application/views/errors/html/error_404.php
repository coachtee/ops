<?php
defined('BASEPATH') OR exit('No direct script access allowed');
// Rendered by show_404(), including for a record id that isn't in the
// caller's own business — so the copy deliberately doesn't distinguish
// "no such record" from "not yours".
$base = defined('BASEPATH') && function_exists('base_url') ? base_url() : '/';
?><!doctype html>
<html lang="en">
<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<meta name="color-scheme" content="light dark">
	<title>Page not found · OPS</title>
	<link rel="stylesheet" href="<?= $base ?>assets/web/app.css">
	<script>(function(){try{var t=localStorage.getItem('ops-theme');if(t){document.documentElement.setAttribute('data-theme',t);}}catch(e){}})();</script>
	<style>
		.nf { min-height: 100vh; display: grid; place-items: center; padding: 24px; }
		.nf-card { max-width: 460px; text-align: center; }
		.nf-code { font-size: 62px; font-weight: 800; letter-spacing: -2px; color: var(--primary); line-height: 1; }
		.nf h1 { font-size: 22px; font-weight: 700; margin: 14px 0 8px; }
		.nf p { color: var(--text-muted); margin: 0 0 22px; line-height: 22px; }
	</style>
</head>
<body>
	<div class="nf">
		<div class="nf-card">
			<div class="nf-code">404</div>
			<h1><?= isset($heading) ? html_escape($heading) : 'Page not found' ?></h1>
			<p>That page doesn't exist, or the record isn't one this business can see.</p>
			<a class="btn btn-primary btn-lg" href="<?= $base ?>dashboard">Back to dashboard</a>
		</div>
	</div>
</body>
</html>
