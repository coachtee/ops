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

The JSON API Android talks to lives under `/api/*`. Visiting `/` in a browser goes to the web
admin panel (see "Web admin panel" below) — register a business via `POST /api/auth/register/`
first (there's no web-based sign-up yet), then log in at `/login` with that email/password.

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

Current coverage (49 tests): health check, register/login/refresh, auth-required rejection,
cross-tenant scoping, the sync protocol (push/pull/conflict/idempotency/last-write-wins), quote
+ job + invoice document numbering (sequential, per-business, assigned once on first successful
sync and never reassigned), quote/invoice totals recomputed from line items (including a
soft-deleted line item correctly dropping back out of the total), the invoice payment-state
machine (sent → partially_paid → paid, and back down on a reversed payment, with cancelled
invoices never touched), expense VAT-inclusive extraction + validation (amount > 0, date ≤
tomorrow), payslip net-pay + validation (deductions ≤ gross, period_end ≥ period_start), visit
photo upload, and all three Reports endpoints (including CSV export and the this_month/all_time
expense-category split).

## What's ported so far

**Everything in docs/API_CONTRACT.md**: Auth, Business, Customer, Lead, Quote + QuoteLineItem,
Job, Visit (+ photo upload), Invoice + InvoiceLineItem, Payment, Supplier, Expense (+ receipt
upload), Employee, Payslip, ComplianceItem, the full 14-model sync push/pull protocol with its
post-batch recompute (document numbering + quote/invoice totals + invoice payment state — see
`application/controllers/Sync.php`), and the three Reports endpoints. This mirrors
`../backend/` (Django)'s own resource set field-for-field — see each `application/models/*.php`
file's doc comment for the exact Django source it ports.

**Known simplifications versus the Django version** (acceptable for this rewrite's scope,
documented rather than silently done): the direct per-resource CRUD controllers (`/api/quotes/`,
`/api/payments/`, etc. — "convenience reads/writes... not the app's main sync path" per
API_CONTRACT.md) do not run the cross-record recompute cascade (parent quote/invoice totals,
payment-state) that `Sync::push_post()` runs after every batch; only the sync path does. Deep
cross-tenant foreign-key validation (e.g. "this expense's job_id must belong to the same
business") is not enforced beyond the existing per-resource tenant scoping. Neither gap is
exercised by the Android app, which only ever writes through sync.

**Django is not yet removed.** `../backend/` still exists, is still the backend the current
production Android app (if any is deployed) would need, and stays in place until this rewrite
reaches real parity and Android is repointed at it — see the top-level migration plan for the
cutover sequencing.

## Money and VAT

Implemented in `application/helpers/money_helper.php` (autoloaded), ported directly from
`../backend/common/money.py`. No `bcmath` extension is installed in this sandbox
(`php8.4-bcmath` ships from the `ondrej/php` PPA, which this environment's egress policy
blocks — confirmed, not retried), so this uses PHP's native `float` + `round()`
(`PHP_ROUND_HALF_UP`, PHP's default, matching Python's `Decimal` `ROUND_HALF_UP` exactly) —
safe here specifically because every function does at most one multiply/divide immediately
followed by one `round()` to cents, never a chain of unrounded float operations. Verified
against all 19 of `../backend/tests/test_money.py`'s own assertions, byte-for-byte, including
the float-precision edge case `compute_line_total("2", "950.005") == "1900.01"`.

## Production

See `docs/CPANEL_DEPLOY.md` for a full step-by-step guide to deploying this on shared cPanel
hosting with **no Terminal/SSH access** (File Manager + phpMyAdmin only) — includes a
ready-to-import `docs/CPANEL_SCHEMA.sql` schema dump (since `php index.php migrate` needs a
shell) and instructions for packaging a Composer-built `vendor/` for upload (since there's no
Composer on the host either).

Business logo upload (`PATCH /api/business/me/` with a multipart body — see
`application/helpers/multipart_helper.php`'s doc comment for why PHP needs this hand-parsed:
it only auto-parses multipart bodies for POST, not PATCH) is implemented and tested — see
`tests/BusinessLogoTest.php`.

`OPS_SECRET_KEY` (JWT signing secret, mirrors Django's `OPS_SECRET_KEY`) MUST be set to a real
random value for any shared/staging/production run. `application/config/config.php` now
**refuses to run at all** (a clear `FATAL:` 500 response, not a silent insecure default) if
`ENVIRONMENT !== 'development'` and this is still the placeholder — the same "loud failure, not
a silent insecure default" principle `../backend/ops/settings.py` established for Django,
verified working in both directions (refuses without a real key, runs normally with one).

Every `OPS_*` config value is read through `application/config/env.php`'s `ops_env()`, which
checks `getenv()` then falls back to `$_SERVER`/`$_ENV` — needed because a shared Apache host's
`.htaccess` `SetEnv` directive (the only way to set "environment variables" without shell
access) reliably populates `$_SERVER` but not always `getenv()`.

## Web admin panel

A server-rendered, Bootstrap-based admin panel in the same general genre as Perfex CRM's own
panel (dark icon sidebar, light card-based content, a slim topbar) — an **original layout**
built for this app from scratch, not copied from Perfex's actual theme assets/CSS/icons.
Session-cookie login (`/login`, `/logout`), completely separate from the JWT Android uses (see
`Web_Controller`'s doc comment in `application/core/MY_Controller.php`) — matching how Perfex
CRM itself keeps its web panel login apart from any API/module auth.

Pages: `/dashboard` (this month's revenue/expenses/profit, outstanding invoices, open quotes,
recent leads/invoices), and read-only list + detail views for `/customers`, `/leads`,
`/quotes`, `/jobs`, `/invoices` (a customer's detail page cross-links its quotes/jobs/invoices;
a quote/invoice's detail page shows its line items and computed totals; an invoice's detail page
also shows its payments). **Read-only by design for this pass** — the Android app remains the
one place that writes this data (via sync), matching how API_CONTRACT.md already frames the
per-resource CRUD endpoints as secondary to the sync protocol; the web panel is this app's admin
visibility/reporting layer, not a second write path. Suppliers/expenses/employees/payslips/
compliance items don't have web views yet — not yet ported to this layer.

Covered by `tests/WebUiTest.php` (login success/failure, CSRF, session redirect when
unauthenticated, tenant scoping, logout) and `tests/BusinessLogoTest.php`.

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
