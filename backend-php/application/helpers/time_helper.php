<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * All timestamps are UTC, microsecond precision, ISO-8601 with a literal
 * `Z` suffix — never `+00:00` (see docs/API_CONTRACT.md's opening
 * paragraph, unchanged by this rewrite: an un-encoded `+` in a URL query
 * string decodes as a space, which would silently corrupt the sync `since`
 * cursor). MySQL's DATETIME(6) columns store naive UTC values (the app
 * never sets a session timezone), so no timezone conversion happens here —
 * only reformatting.
 */

/** MySQL DATETIME(6) string ('Y-m-d H:i:s.u') -> wire ISO-8601 ('Z' suffix). */
function iso8601($mysql_datetime)
{
	// A DATETIME(6) column always yields a 6-digit fractional part from
	// this app's own writes, but format defensively (matching IsoTimestamp's
	// own "accepts no-fraction values too" leniency on the Android side)
	// rather than assuming.
	if (strpos($mysql_datetime, '.') === FALSE)
	{
		$mysql_datetime .= '.000000';
	}
	list($date_part, $frac) = explode('.', $mysql_datetime, 2);
	$frac = str_pad(substr($frac, 0, 6), 6, '0');
	return str_replace(' ', 'T', $date_part).'.'.$frac.'Z';
}

/** Current UTC instant as a MySQL DATETIME(6) string, for writing. */
function mysql_now()
{
	$microtime = microtime(TRUE);
	$dt = DateTimeImmutable::createFromFormat('U.u', sprintf('%.6F', $microtime), new DateTimeZone('UTC'));
	return $dt->format('Y-m-d H:i:s.u');
}

/** Wire ISO-8601 ('...Z' or '...+00:00') -> MySQL DATETIME(6) string.
 * Accepts a bare 'Z' or an explicit offset, matching IsoTimestamp's own
 * parse-side leniency on the Android client. */
function mysql_datetime_from_iso($iso)
{
	$dt = new DateTimeImmutable($iso);
	$dt = $dt->setTimezone(new DateTimeZone('UTC'));
	return $dt->format('Y-m-d H:i:s.u');
}
