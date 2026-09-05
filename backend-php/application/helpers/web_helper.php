<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * Renders a status string as a colored pill — one tone family per meaning
 * (neutral/info/warning/success/danger), the same status->tone grouping
 * idea as the Android app's StatusBadge component, so "paid"/"accepted"/
 * "completed" all read as success everywhere in this product, and
 * "declined"/"cancelled"/"lost" always read as danger.
 */
function ops_status_badge($status)
{
	$tones = array(
		'success' => array('paid', 'accepted', 'completed', 'converted'),
		'danger' => array('declined', 'cancelled', 'lost', 'expired'),
		'warning' => array('partially_paid', 'needs_follow_up', 'overdue', 'quoted'),
		'info' => array('sent', 'in_progress', 'contacted', 'en_route'),
	);
	$tone = 'neutral';
	foreach ($tones as $candidate => $statuses)
	{
		if (in_array($status, $statuses, TRUE))
		{
			$tone = $candidate;
			break;
		}
	}
	$label = ucwords(str_replace('_', ' ', (string) $status));
	return '<span class="ops-badge ops-badge-'.$tone.'">'.html_escape($label).'</span>';
}

/** "" -> "—" for empty detail-row values — a blank cell reads as "still
 * loading", an em dash reads as "deliberately empty". */
function ops_or_dash($value)
{
	return ($value === NULL || $value === '') ? '—' : html_escape($value);
}

function ops_money($value)
{
	return 'R '.number_format((float) $value, 2);
}

function ops_date($value)
{
	if (!$value)
	{
		return '—';
	}
	return date('d M Y', strtotime($value));
}
