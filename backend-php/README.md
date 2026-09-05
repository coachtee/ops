# OPS backend (PHP / CodeIgniter 3 / MySQL)

**Replacing** `../backend/` (Django/DRF/PostgreSQL) at the user's explicit direction: this
backend follows Perfex CRM's actual stack — PHP + CodeIgniter 3 + MySQL/MariaDB, not Laravel
(Perfex CRM is not a Laravel product). Same wire contract as before
(`../docs/API_CONTRACT.md`) — this is an implementation swap underneath the Android app, not a
new API design. **This is a work in progress, ported incrementally**: see "What's ported so far"
below before assuming anything beyond that exists.

## Run it

```bash
composer install
export OPS_DB_HOST=localhost OPS_DB_NAME=ops_ci OPS_DB_USER=ops OPS_DB_PASSWORD=ops
php index.php migrate
php -S 127.0.0.1:8080 router.php   # dev only — see "Production" below for real deployment
```

`router.php` exists only because PHP's built-in server has no mod_rewrite of its own; a real
deployment serves this through Apache/nginx with URL rewriting to `index.php`, same as any CI3
app (Perfex CRM included).

## Test

```bash
composer install   # installs phpunit/phpunit as a dev dependency
php -S 127.0.0.1:8080 router.php &
vendor/bin/phpunit
```

These are HTTP-level integration tests, not isolated unit tests of CI3 classes — CodeIgniter 3
(like Perfex CRM itself) was never built with dependency injection or unit-testability in mind,
so the reliable way to test it for real is to actually call it over HTTP, the same way Android
does. Each test registers its own throwaway business (unique email per call) for isolation
against the shared dev database — there's no separate ephemeral test database the way Django's
test runner gave us, since PHPUnit here doesn't manage schema/fixtures itself. See
`tests/ApiTestCase.php`.

Current coverage: health check, register (success + duplicate-email + short-password
rejection), login (success + wrong password), refresh (+ rejecting an access token used as a
refresh token), auth-required rejection (missing/garbage token), cross-tenant scoping (a
business cannot read, update, or list another business's customers), and the sync protocol
(push accepted, pull returns it, idempotent replay reports `conflict` not a duplicate, a
genuinely newer update wins, pull never leaks another business's rows).

## What's ported so far

Only **auth (register/login/refresh) + Business (read/update) + Customer (CRUD + sync)** — a
complete, proven vertical slice: JWT issue/verify, tenant scoping via Membership, the full sync
push/pull/conflict/idempotency protocol, all verified against a real MariaDB instance, not just
written. This mirrors exactly how `../backend/` (Django) itself started — see
`../docs/DISCOVERY.md`.

**Not yet ported** (still only in `../backend/`, the Django version): Lead, Quote + line items,
Job, Visit (+ photo), Invoice + line items, Payment, Supplier, Expense (+ receipt upload,
VAT-inclusive extraction), Employee, Payslip (net-pay computation), ComplianceItem, the three
Reports endpoints, and the remaining 13 of 14 sync-registry entries. Money/VAT computation for
those resources needs its own careful port — see "Money and VAT" below, since `bcmath` isn't
available in this environment (PHP built from a blocked PPA — see below) and the existing Django
logic uses Python's arbitrary-precision `Decimal`.

**Django is not yet removed.** `../backend/` still exists, is still the backend the current
production Android app (if any is deployed) would need, and stays in place until this rewrite
reaches real parity and Android is repointed at it — see the top-level migration plan for the
cutover sequencing.

## Money and VAT (once ported)

No `bcmath` extension is installed in this sandbox (`php8.4-bcmath` ships from the
`ondrej/php` PPA, which this environment's egress policy blocks — confirmed, not
retried). Plan: represent money as integer cents internally (a well-established, arguably more
robust pattern than decimal-string arithmetic — see e.g. how Stripe represents amounts) and use
PHP's native `round()` (default `PHP_ROUND_HALF_UP` mode, matching the Django backend's own
`ROUND_HALF_UP` rounding rule exactly) for the one place that needs a genuine division — VAT
extraction (`amount * 15 / 115`). Every other operation (line-item totals, discount, running
sums) is exact integer arithmetic on cents, never floating point. Document this decision inline
wherever it's implemented, the same way the Django backend documents its own `Decimal` usage.

## Production

Not deployed anywhere — see `../android/README.md`'s "No staging/production server exists yet"
section for what a real deployment needs (domain, DNS, TLS, a real web server in front of PHP).
`OPS_SECRET_KEY` (JWT signing secret, mirrors Django's `OPS_SECRET_KEY`) MUST be set to a real
random value for any shared/staging/production run — `application/config/config.php`'s
`encryption_key` falls back to an insecure placeholder for zero-config local dev only, same
"loud failure, not a silent insecure default" principle `../backend/ops/settings.py` established
(that file's own guard doesn't carry over automatically — this PHP backend needs its own
equivalent startup check before it's exposed anywhere real; not yet added).

## Why CodeIgniter 3 specifically, not CodeIgniter 4 or Laravel

Perfex CRM (the explicit stack reference) is built on CodeIgniter 3 — an older, pre-Composer-era
MVC framework with its own Active Record-style query builder (no Eloquent), file-based routing
config, and a migration system modeled after but simpler than Django's. CodeIgniter 3's last
release was 2019 and predates PHP 8.2's "creation of dynamic property" deprecation, which is why
`index.php`'s development error-reporting excludes `E_DEPRECATED`/`E_STRICT` — framework/PHP-
version mismatch noise, not an application bug (see the comment there). `chriskacerguis/
codeigniter-restserver` (a well-known third-party CI3 library, installed via Composer) supplies
HTTP-verb method dispatch (`index_get`/`index_post`/...) that plain CI3 controllers don't have
natively; its own built-in auth mechanisms are disabled (see `application/config/rest.php`) in
favor of custom JWT auth (`application/libraries/Auth_lib.php`,
`application/core/MY_Controller.php`).
