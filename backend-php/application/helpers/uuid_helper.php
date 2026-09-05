<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * Server-generated UUIDv4 — only used where the client hasn't already
 * supplied one (e.g. the Membership row created during register). Every
 * syncable resource's id is client-generated (see docs/API_CONTRACT.md's
 * sync protocol, unchanged by this rewrite) — this helper is not used for
 * those.
 */
function uuid4()
{
	$data = random_bytes(16);
	$data[6] = chr((ord($data[6]) & 0x0f) | 0x40);
	$data[8] = chr((ord($data[8]) & 0x3f) | 0x80);
	return vsprintf('%s%s-%s-%s-%s-%s%s%s', str_split(bin2hex($data), 4));
}
