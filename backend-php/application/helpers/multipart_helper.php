<?php
defined('BASEPATH') OR exit('No direct script access allowed');

/**
 * PHP only auto-populates $_POST/$_FILES for a multipart/form-data body on
 * a POST request — the identical body on a PATCH (or PUT) request, which is
 * exactly what Android's updateBusinessWithLogo() sends (see
 * OpsApiService.kt and BusinessRepository.updateProfileWithLogo()), is left
 * entirely unparsed in php://input. This hand-parses it.
 *
 * @return array{0: array<string,string>, 1: array<string,array{name:string,type:string,tmp_name:string,size:int}>}
 *   [text fields, files] — a file's 'tmp_name' is a plain temp file (created
 *   via tempnam(), not a real HTTP upload), so move it with rename(), never
 *   move_uploaded_file() (which only accepts genuine $_FILES entries).
 */
function parse_multipart_form_data($raw_body, $content_type)
{
	$fields = array();
	$files = array();

	if (!preg_match('/boundary=(?:"([^"]+)"|([^;]+))/', $content_type, $matches))
	{
		return array($fields, $files);
	}
	$boundary = $matches[1] !== '' ? $matches[1] : rtrim($matches[2]);

	$parts = preg_split('/--'.preg_quote($boundary, '/').'(?:--)?\r?\n/', $raw_body);
	foreach ($parts as $part)
	{
		if (trim($part) === '')
		{
			continue;
		}
		$split = preg_split('/\r?\n\r?\n/', $part, 2);
		if (count($split) !== 2)
		{
			continue;
		}
		list($raw_headers, $body) = $split;
		if (!preg_match('/name="([^"]+)"/', $raw_headers, $name_match))
		{
			continue;
		}
		$name = $name_match[1];
		$body = preg_replace('/\r?\n$/', '', $body);

		if (preg_match('/filename="([^"]*)"/', $raw_headers, $filename_match) && $filename_match[1] !== '')
		{
			preg_match('/Content-Type:\s*([^\r\n]+)/i', $raw_headers, $type_match);
			$tmp_path = tempnam(sys_get_temp_dir(), 'ops_upload_');
			file_put_contents($tmp_path, $body);
			$files[$name] = array(
				'name' => $filename_match[1],
				'type' => $type_match[1] ?? 'application/octet-stream',
				'tmp_name' => $tmp_path,
				'size' => filesize($tmp_path),
			);
		}
		else
		{
			$fields[$name] = $body;
		}
	}

	return array($fields, $files);
}
