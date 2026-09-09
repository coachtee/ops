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

The JSON API Android talks to lives under `/api/*`. Visiting `/` in a browser gets the public
landing page; `/register` creates a business straight from the web and `/login` signs into the
admin panel (see "Web admin panel" below). A web sign-up and a `POST /api/auth/register/` create
the same account — either one can then log in on the phone or in the panel.

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

Current coverage (67 tests) — API: health check, register/login/refresh, auth-required rejection,
cross-tenant scoping, the sync protocol (push/pull/conflict/idempotency/last-write-wins), quote
+ job + invoice document numbering (sequential, per-business, assigned once on first successful
sync and never reassigned), quote/invoice totals recomputed from line items (including a
soft-deleted line item correctly dropping back out of the total), the invoice payment-state
machine (sent → partially_paid → paid, and back down on a reversed payment, with cancelled
invoices never touched), expense VAT-inclusive extraction + validation (amount > 0, date ≤
tomorrow), payslip net-pay + validation (deductions ≤ gross, period_end ≥ period_start), visit
photo upload, and all three Reports endpoints (including CSV export and the this_month/all_time
expense-category split). Web panel (`tests/WebPanelPagesTest.php`): the landing and register
pages served to a logged-out visitor, a web sign-up that then logs in against the JSON API, the
short-password and duplicate-email rejections, all sixteen module pages rendering without a PHP
or database error, filters composing with search, global search, the reports CSV export's
columns, a 404 for another business's record id, and an invoice detail's recomputed totals.

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

A server-rendered admin panel in the same general genre as Perfex CRM's own — dark sidebar rail
with grouped navigation, light card-and-table content, a slim topbar — an **original layout and
stylesheet** built for this app, not copied from Perfex's theme assets, CSS or icons. Session-
cookie login, completely separate from the JWT Android uses (see `Web_Controller`'s doc comment
in `application/core/MY_Controller.php`), matching how Perfex CRM itself keeps its panel login
apart from any API auth.

### Public pages

`/` is a landing page describing the product, `/register` creates a business and its first user
in one form, `/login` and `/logout` handle the session. A web sign-up and a `POST
/api/auth/register/` produce the same account, so an owner can sign up on the laptop and log
straight into the phone app with those credentials — `tests/WebPanelPagesTest.php` asserts
exactly that.

### Modules

`/dashboard`, then list + detail views for `/leads`, `/customers`, `/quotes`, `/invoices`,
`/payments`, `/jobs`, `/schedule` (visits), `/expenses`, `/suppliers`, `/employees`,
`/payslips` and `/compliance`, plus `/reports`, `/settings` and a global `/search`. Every list
shares one implementation — `Web_resource_controller` in `application/core/MY_Controller.php`
handles search, status filtering, sorting and pagination from a handful of declared properties,
so a module page is a subclass and two views rather than a twelfth copy of the same query code.
Related names (a lead's customer, an expense's supplier) resolve through `name_map()`, one
batched query per column instead of one per row.

`/reports` and `/dashboard` read from `Insights_model`, which is also what the JSON reports
endpoints use, so the screen, the CSV export and the API can't disagree about what a month's
revenue was. Revenue is cash-basis throughout (money actually received, dated by
`payments.paid_date`), which is the definition the CSV header and the page subtitle both state.

**Read-only by design.** The Android app remains the one place this data is written, via sync —
API_CONTRACT.md already frames the per-resource CRUD endpoints as secondary to the sync
protocol. The panel is the reporting and visibility layer, not a second write path. The two
exceptions are the ones that have to be: creating an account, and the reports date-range form.

### Design system

`assets/web/app.css` takes its palette verbatim from the Android app's Design System v3
(`android/app/src/main/kotlin/com/ops/app/ui/theme/Color.kt`), so the panel and the phone are
visibly one product: blue is interaction, green means success and only success, amber means
needs-attention, red means failed. Everything else is ink on neutral. Dark mode follows the
system by default and can be pinned either way from the topbar (stored in `localStorage`).

The two chart series carry their own `--chart-revenue` / `--chart-expense` tokens rather than
reusing `--primary` / `--warning`: the UI tokens are tuned for text and buttons, and in dark mode
`--primary` (#9FCBFF) is too light and too low-chroma to work as a fill — it reads grey. Both
pairs were run through a categorical-palette validator per mode and pass on lightness band,
chroma floor, colour-blind separation, normal-vision separation and contrast against the
surface. The comment above the tokens records the numbers.

**No CDN dependencies at all** — no Bootstrap, no Font Awesome, no web fonts. Icons are inline
SVG from `ops_icon()` in `application/helpers/web_helper.php`, and the type stack is the
system UI font. Shared cPanel hosting can sit behind an outbound firewall, and a panel that
loses its stylesheet when a CDN is unreachable is not acceptable for the one screen an owner
checks their money on.

### Verification

`tests/WebPanelPagesTest.php` drives every route over real HTTP with a real session cookie;
`tests/WebUiTest.php` covers the session mechanics underneath it (login success and failure,
CSRF, the redirect when unauthenticated, tenant scoping, logout) and `tests/BusinessLogoTest.php`
the logo upload.

Layout was checked by screenshotting each page through Chrome DevTools Protocol at 1440px and at
an emulated 390px phone, asserting `scrollWidth == clientWidth` on each — worth noting that
Chromium floors `--window-size` at 500px, so a naive headless "mobile" screenshot is a desktop
layout cropped to the requested width and will hide every responsive bug you have.

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
