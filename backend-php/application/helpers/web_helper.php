<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * View helpers for the web admin panel. Everything here is presentation
 * only — no query runs from a view.
 *
 * Icons are inline SVG rather than an icon-font CDN: the panel must render
 * correctly on shared hosting that may sit behind a firewall (and this
 * project's own sandbox egress policy blocks cdnjs outright), and an admin
 * screen whose every icon silently turns into an empty box is worse than
 * no icons at all.
 */

/** 24x24 stroke geometry, drawn to a single grid so mixed icons line up. */
function _ops_icon_paths()
{
	return array(
		'grid' => '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',
		'target' => '<circle cx="12" cy="12" r="8"/><circle cx="12" cy="12" r="4"/><circle cx="12" cy="12" r="1"/>',
		'users' => '<path d="M16 20v-1.5a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4V20"/><circle cx="9" cy="7" r="3.5"/><path d="M16 3.6a3.5 3.5 0 0 1 0 6.8"/><path d="M22 20v-1.5a4 4 0 0 0-3-3.85"/>',
		'user' => '<circle cx="12" cy="8" r="4"/><path d="M4 20v-1a5 5 0 0 1 5-5h6a5 5 0 0 1 5 5v1"/>',
		'file-text' => '<path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/><path d="M14 3v5h5"/><path d="M9 13h6"/><path d="M9 17h4"/>',
		'briefcase' => '<rect x="2.5" y="7" width="19" height="13" rx="2"/><path d="M9 7V5.5A1.5 1.5 0 0 1 10.5 4h3A1.5 1.5 0 0 1 15 5.5V7"/><path d="M2.5 12h19"/>',
		'calendar' => '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18"/><path d="M8 3v4"/><path d="M16 3v4"/>',
		'receipt' => '<path d="M5 3.5v17l2.5-1.5 2.5 1.5 2-1.5 2 1.5 2.5-1.5 2.5 1.5v-17z"/><path d="M9 8.5h6"/><path d="M9 12.5h6"/>',
		'credit-card' => '<rect x="2.5" y="5" width="19" height="14" rx="2.5"/><path d="M2.5 10h19"/><path d="M6.5 15h3"/>',
		'wallet' => '<path d="M3 7.5A2.5 2.5 0 0 1 5.5 5H18a2 2 0 0 1 2 2v1"/><path d="M3 7.5V17a2.5 2.5 0 0 0 2.5 2.5H19a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2H5.5"/><circle cx="16.5" cy="13.5" r="1.2"/>',
		'truck' => '<path d="M2.5 6.5h10.5v10H2.5z"/><path d="M13 10h4l3.5 3.5v3H13z"/><circle cx="7" cy="18" r="1.8"/><circle cx="17" cy="18" r="1.8"/>',
		'badge' => '<rect x="3.5" y="4" width="17" height="16" rx="2.5"/><circle cx="12" cy="10.5" r="2.5"/><path d="M8 16.5a4.2 4.2 0 0 1 8 0"/>',
		'banknote' => '<rect x="2.5" y="6" width="19" height="12" rx="2"/><circle cx="12" cy="12" r="2.5"/><path d="M6 9.5v5"/><path d="M18 9.5v5"/>',
		'shield' => '<path d="M12 3l7.5 3v5.5c0 4.4-3 8.2-7.5 9.5-4.5-1.3-7.5-5.1-7.5-9.5V6z"/><path d="M9 12l2 2 4-4"/>',
		'bar-chart' => '<path d="M4 20V10"/><path d="M10 20V4"/><path d="M16 20v-7"/><path d="M21 20H3"/>',
		'trending-up' => '<path d="M3 17l6-6 4 4 8-8"/><path d="M15 7h6v6"/>',
		'trending-down' => '<path d="M3 7l6 6 4-4 8 8"/><path d="M15 17h6v-6"/>',
		'settings' => '<circle cx="12" cy="12" r="3"/><path d="M19.4 14.5a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-2.7 1.1V21a2 2 0 1 1-4 0v-.1a1.6 1.6 0 0 0-2.7-1.1l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0-1.1-2.7H3a2 2 0 1 1 0-4h.1a1.6 1.6 0 0 0 1.1-2.7l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.6 1.6 0 0 0 1.8.3H9a1.6 1.6 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.6 1.6 0 0 0 2.7 1.1l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.8V9a1.6 1.6 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1z"/>',
		'search' => '<circle cx="11" cy="11" r="7"/><path d="M20 20l-3.6-3.6"/>',
		'plus' => '<path d="M12 5v14"/><path d="M5 12h14"/>',
		'check' => '<path d="M4.5 12.5l5 5 10-11"/>',
		'check-circle' => '<circle cx="12" cy="12" r="9"/><path d="M8 12.5l2.5 2.5L16 9.5"/>',
		'x' => '<path d="M6 6l12 12"/><path d="M18 6L6 18"/>',
		'menu' => '<path d="M3.5 6.5h17"/><path d="M3.5 12h17"/><path d="M3.5 17.5h17"/>',
		'chevron-right' => '<path d="M9 5l7 7-7 7"/>',
		'chevron-left' => '<path d="M15 5l-7 7 7 7"/>',
		'chevron-down' => '<path d="M5 9l7 7 7-7"/>',
		'chevron-up' => '<path d="M5 15l7-7 7 7"/>',
		'arrow-left' => '<path d="M19 12H5"/><path d="M11 6l-6 6 6 6"/>',
		'arrow-right' => '<path d="M5 12h14"/><path d="M13 6l6 6-6 6"/>',
		'arrow-up-right' => '<path d="M7 17L17 7"/><path d="M8 7h9v9"/>',
		'log-out' => '<path d="M15 17v2a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h7a2 2 0 0 1 2 2v2"/><path d="M20 12H9"/><path d="M17 8l4 4-4 4"/>',
		'sun' => '<circle cx="12" cy="12" r="4.2"/><path d="M12 2.5v2"/><path d="M12 19.5v2"/><path d="M4.2 4.2l1.4 1.4"/><path d="M18.4 18.4l1.4 1.4"/><path d="M2.5 12h2"/><path d="M19.5 12h2"/><path d="M4.2 19.8l1.4-1.4"/><path d="M18.4 5.6l1.4-1.4"/>',
		'moon' => '<path d="M20.5 14.5A8.5 8.5 0 0 1 9.5 3.5a8.5 8.5 0 1 0 11 11z"/>',
		'alert-triangle' => '<path d="M12 4l9 15.5H3z"/><path d="M12 10v4"/><path d="M12 17h.01"/>',
		'info' => '<circle cx="12" cy="12" r="9"/><path d="M12 11v5"/><path d="M12 8h.01"/>',
		'printer' => '<path d="M7 9V3.5h10V9"/><rect x="4" y="9" width="16" height="7" rx="2"/><path d="M7 14h10v6.5H7z"/>',
		'phone' => '<path d="M6.5 3.5h3l1.5 4-2 1.5a12 12 0 0 0 6 6l1.5-2 4 1.5v3a2 2 0 0 1-2.2 2A17 17 0 0 1 4.5 5.7 2 2 0 0 1 6.5 3.5z"/>',
		'mail' => '<rect x="3" y="5" width="18" height="14" rx="2.5"/><path d="M3.5 7l8.5 6 8.5-6"/>',
		'map-pin' => '<path d="M12 21s7-6.2 7-11a7 7 0 1 0-14 0c0 4.8 7 11 7 11z"/><circle cx="12" cy="10" r="2.6"/>',
		'clock' => '<circle cx="12" cy="12" r="9"/><path d="M12 7v5.2l3.2 2"/>',
		'building' => '<path d="M4 21V5.5A1.5 1.5 0 0 1 5.5 4h9A1.5 1.5 0 0 1 16 5.5V21"/><path d="M16 10h3.5A1.5 1.5 0 0 1 21 11.5V21"/><path d="M2.5 21h19"/><path d="M7.5 8h2"/><path d="M7.5 12h2"/><path d="M7.5 16h2"/>',
		'inbox' => '<path d="M3.5 13H8l1.5 3h5L16 13h4.5"/><path d="M5.5 5h13l2 8v4.5a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2V13z"/>',
		'download' => '<path d="M12 4v10"/><path d="M8 11l4 4 4-4"/><path d="M4.5 19h15"/>',
		'filter' => '<path d="M3.5 5.5h17l-6.5 8V20l-4-2v-4.5z"/>',
		'refresh' => '<path d="M20 11a8 8 0 0 0-13.7-4.9L3.5 8.5"/><path d="M4 13a8 8 0 0 0 13.7 4.9l2.8-2.4"/><path d="M3.5 4v4.5H8"/><path d="M20.5 20v-4.5H16"/>',
		'smartphone' => '<rect x="6.5" y="2.5" width="11" height="19" rx="2.5"/><path d="M10.5 18.5h3"/>',
		'cloud-off' => '<path d="M17.5 17.5H7a4 4 0 0 1-.7-7.94"/><path d="M8.8 5.6A6 6 0 0 1 18 10.5a3.5 3.5 0 0 1 2.3 5.9"/><path d="M3 3l18 18"/>',
		'zap' => '<path d="M13 2.5L4.5 13.5H11l-.5 8L19.5 10H13z"/>',
		'lock' => '<rect x="4.5" y="10" width="15" height="10.5" rx="2.5"/><path d="M8 10V7.5a4 4 0 0 1 8 0V10"/>',
		'eye' => '<path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z"/><circle cx="12" cy="12" r="3"/>',
		'external-link' => '<path d="M14 4h6v6"/><path d="M20 4l-9 9"/><path d="M18 14v5a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h5"/>',
		'sort' => '<path d="M8 4v16"/><path d="M4.5 7.5L8 4l3.5 3.5"/><path d="M16 20V4"/><path d="M12.5 16.5L16 20l3.5-3.5"/>',
		'sparkles' => '<path d="M12 3l1.8 4.7L18.5 9.5 13.8 11.3 12 16l-1.8-4.7L5.5 9.5l4.7-1.8z"/><path d="M18.5 15.5l.8 2 2 .8-2 .8-.8 2-.8-2-2-.8 2-.8z"/>',
		'wifi-off' => '<path d="M3 3l18 18"/><path d="M9 17.5a4 4 0 0 1 6 0"/><path d="M6 13.8a8.5 8.5 0 0 1 3-2.1"/><path d="M15 11.7a8.5 8.5 0 0 1 3 2.1"/><path d="M3.5 10a13 13 0 0 1 4-2.6"/><path d="M17 7.7a13 13 0 0 1 3.5 2.3"/>',
	);
}

/**
 * Inline SVG icon. Returns '' for an unknown name rather than throwing —
 * a missing decorative icon must never take a page down.
 */
function ops_icon($name, $class = '')
{
	$paths = _ops_icon_paths();
	if (!isset($paths[$name]))
	{
		return '';
	}
	$class_attr = $class !== '' ? ' class="'.html_escape($class).'"' : '';
	return '<svg'.$class_attr.' viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" '
		.'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">'
		.$paths[$name].'</svg>';
}

/**
 * Renders a status string as a coloured pill — one tone family per meaning,
 * the same status->tone grouping as the Android app's StatusBadge, so
 * "paid"/"accepted"/"completed" read as success everywhere in this product
 * and "declined"/"cancelled"/"lost" always read as danger.
 */
function ops_status_tone($status)
{
	$tones = array(
		'success' => array('paid', 'accepted', 'completed', 'converted'),
		'danger' => array('declined', 'cancelled', 'lost', 'expired', 'overdue'),
		'warning' => array('partially_paid', 'needs_follow_up', 'quoted'),
		'info' => array('sent', 'in_progress', 'contacted', 'en_route'),
	);
	foreach ($tones as $tone => $statuses)
	{
		if (in_array($status, $statuses, TRUE))
		{
			return $tone;
		}
	}
	return 'neutral';
}

function ops_status_badge($status)
{
	if ($status === NULL || $status === '')
	{
		return '';
	}
	$label = ucwords(str_replace('_', ' ', (string) $status));
	return '<span class="badge badge-'.ops_status_tone($status).'">'.html_escape($label).'</span>';
}

/** "" -> "—" — a blank cell reads as "still loading", an em dash reads as
 * "deliberately empty". */
function ops_or_dash($value)
{
	return ($value === NULL || $value === '') ? '<span class="subtle">&mdash;</span>' : html_escape($value);
}

function ops_money($value)
{
	return 'R&nbsp;'.number_format((float) $value, 2);
}

/** R 1.2m / R 45.6k / R 980 — for stat tiles where the exact cents don't
 * help and the full number would wrap. */
function ops_money_compact($value)
{
	$amount = (float) $value;
	$sign = $amount < 0 ? '-' : '';
	$amount = abs($amount);
	if ($amount >= 1000000)
	{
		return $sign.'R&nbsp;'.rtrim(rtrim(number_format($amount / 1000000, 1), '0'), '.').'m';
	}
	if ($amount >= 10000)
	{
		return $sign.'R&nbsp;'.rtrim(rtrim(number_format($amount / 1000, 1), '0'), '.').'k';
	}
	return $sign.'R&nbsp;'.number_format($amount, 2);
}

function ops_date($value)
{
	if (!$value)
	{
		return '<span class="subtle">&mdash;</span>';
	}
	return date('d M Y', strtotime($value));
}

function ops_datetime($value)
{
	if (!$value)
	{
		return '<span class="subtle">&mdash;</span>';
	}
	return date('d M Y, H:i', strtotime($value));
}

/** "Today" / "Yesterday" / "in 3 days" / "12 days ago" — for due dates and
 * follow-ups, where the distance matters more than the calendar date. */
function ops_relative_day($value)
{
	if (!$value)
	{
		return '';
	}
	$then = (int) floor(strtotime(date('Y-m-d', strtotime($value))) / 86400);
	$today = (int) floor(strtotime(date('Y-m-d')) / 86400);
	$days = $then - $today;
	if ($days === 0) return 'Today';
	if ($days === 1) return 'Tomorrow';
	if ($days === -1) return 'Yesterday';
	if ($days > 0) return 'in '.$days.' days';
	return abs($days).' days ago';
}

function ops_initials($name)
{
	$name = trim((string) $name);
	if ($name === '')
	{
		return '?';
	}
	$parts = preg_split('/\s+/', $name);
	if (count($parts) === 1)
	{
		return strtoupper(substr($parts[0], 0, 2));
	}
	return strtoupper(substr($parts[0], 0, 1).substr($parts[count($parts) - 1], 0, 1));
}

/** Builds a URL for the current page with $params merged over the current
 * query string — used by every filter chip, sort header and pager link so
 * they compose instead of clobbering each other. */
function ops_query_url($base_path, array $params, array $current = NULL)
{
	$current = $current === NULL ? $_GET : $current;
	$merged = array_merge($current, $params);
	foreach ($merged as $key => $value)
	{
		if ($value === NULL || $value === '')
		{
			unset($merged[$key]);
		}
	}
	$query = http_build_query($merged);
	return site_url($base_path).($query !== '' ? '?'.$query : '');
}

/**
 * SVG path for a bar with rounded top corners and a square bottom, so the
 * mark stays visually anchored to the baseline (a fully rounded rect
 * floats and misreads its own zero point). $r is clamped so a very short
 * bar doesn't turn into a lozenge.
 */
function ops_bar_path($x, $y, $w, $h, $r = 4)
{
	$r = min($r, $w / 2, max($h, 0));
	if ($h <= 0.5)
	{
		return '';
	}
	$x2 = $x + $w;
	$y2 = $y + $h;
	return 'M'.round($x, 2).' '.round($y2, 2)
		.'V'.round($y + $r, 2)
		.'A'.round($r, 2).' '.round($r, 2).' 0 0 1 '.round($x + $r, 2).' '.round($y, 2)
		.'H'.round($x2 - $r, 2)
		.'A'.round($r, 2).' '.round($r, 2).' 0 0 1 '.round($x2, 2).' '.round($y + $r, 2)
		.'V'.round($y2, 2).'Z';
}

/** Rounds an axis maximum up to a readable gridline value (1/2/5 x 10^n)
 * so the y-axis reads "R 40 000", not "R 38 417.63". */
function ops_nice_max($value)
{
	$value = (float) $value;
	if ($value <= 0)
	{
		return 1.0;
	}
	$exp = floor(log10($value));
	$pow = pow(10, $exp);
	$frac = $value / $pow;
	// Fine-grained steps: with only 1/2/5/10 available, a peak of 50 350
	// rounds all the way to 100 000 and every bar ends up squashed into the
	// bottom half of the plot.
	foreach (array(1, 1.2, 1.5, 2, 2.5, 3, 4, 5, 6, 8, 10) as $step)
	{
		if ($frac <= $step)
		{
			return $step * $pow;
		}
	}
	return 10 * $pow;
}

/** "Sep 25" from "2025-09" — x-axis labels stay short enough not to collide. */
function ops_month_label($ym)
{
	$ts = strtotime($ym.'-01');
	return $ts ? date('M y', $ts) : $ym;
}

/** Percentage of $part out of $whole, clamped to 0-100, for progress bars. */
function ops_percent($part, $whole)
{
	$whole = (float) $whole;
	if ($whole <= 0)
	{
		return 0;
	}
	return max(0, min(100, ((float) $part / $whole) * 100));
}
