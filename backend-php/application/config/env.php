<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * cPanel-style shared Apache hosts commonly only offer `SetEnv FOO bar` in
 * .htaccess for setting "environment variables" for PHP (there's no shell
 * to `export` anything, and no per-app process manager to hand env vars to
 * the way a real server deployment would) — SetEnv reliably populates
 * $_SERVER (and often $_ENV), but NOT always getenv(), depending on the
 * SAPI and php.ini's variables_order. Every OPS_* config value in this app
 * is read through this so either mechanism works, so the same
 * database.php/config.php work unchanged whether OPS_DB_HOST etc. come
 * from a real exported env var (this repo's own dev/CI setup) or from an
 * .htaccess SetEnv directive (see docs/CPANEL_DEPLOY.md).
 */
function ops_env($name, $default = NULL)
{
	$value = getenv($name);
	if ($value !== FALSE && $value !== '')
	{
		return $value;
	}
	if (!empty($_SERVER[$name]))
	{
		return $_SERVER[$name];
	}
	if (!empty($_ENV[$name]))
	{
		return $_ENV[$name];
	}
	return $default;
}
