#!/usr/bin/env bash
#
# Builds the upload package described in docs/CPANEL_DEPLOY.md.
#
# The target host has File Manager and phpMyAdmin and nothing else — no shell,
# no Composer — so vendor/ has to travel inside the zip already built.
#
# vendor/ is built fresh in a temp directory with --no-dev rather than reusing
# the working tree's: that one carries PHPUnit and its 26 dev dependencies,
# which is gigabytes of test fixtures nobody should be uploading to a shared
# host, let alone serving from a web root. The two production packages come to
# about 6 MB.
#
# Also excluded: tests/ and phpunit.xml (dev-only, and they drive a live
# server), uploads/* (receipts and visit photos from local runs — the
# directory ships empty), application/logs/* and application/cache/*, docs/
# (the guide and the schema dump are delivered next to the zip, not inside the
# web root), and .git.
#
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
out="${1:-/tmp/ops-cpanel-backend.zip}"

staging="$(mktemp -d)"
trap 'rm -rf "$staging"' EXIT

cd "$here"

cp composer.json composer.lock "$staging/"
( cd "$staging" && composer install --no-dev --no-scripts --no-interaction --quiet )

cp index.php router.php .htaccess license.txt README.md "$staging/"
cp -r application assets system "$staging/"
rm -rf "$staging/application/cache/"* "$staging/application/logs/"*.php
mkdir -p "$staging/uploads"

rm -f "$out"
( cd "$staging" && zip -qr "$out" . -x '*.DS_Store' -x '*/.git/*' )

echo "built $out"
echo "  $(unzip -l "$out" | tail -1 | tr -s ' ')"
echo "  $(du -h "$out" | cut -f1) compressed"
