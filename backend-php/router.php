<?php
/**
 * Router for `php -S` (PHP's built-in server has no mod_rewrite of its
 * own) — dev/test use only, mirrors what .htaccess/nginx would do in a
 * real deployment: serve an existing file as-is, otherwise hand every
 * request to index.php so CI3's own URI routing takes over.
 */
$uri = urldecode(parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH));
if ($uri !== '/' && file_exists(__DIR__.$uri) && !is_dir(__DIR__.$uri))
{
	return FALSE;
}
require __DIR__.'/index.php';
