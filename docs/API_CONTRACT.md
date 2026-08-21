# OPS API Contract — v1 (vertical slice)

Binding contract between the Django backend and the Android app for this slice. Both sides
implement exactly this. Base path: `/api/`. All responses JSON. All money fields are decimal
strings (e.g. `"1250.00"`), never floats. All timestamps UTC ISO-8601, always with a literal
`Z` suffix (`2026-08-21T10:15:00.123456Z`), never `+00:00` — an un-encoded `+` in a URL query
string decodes as a space under standard form-encoding rules, which would silently corrupt the
`since` cursor below. The server always emits `Z`; a client constructing a timestamp itself
(e.g. a record's `updated_at`) must do the same. All dates `YYYY-MM-DD`. Currency is always
ZAR — not sent per record.

## Auth

`djangorestframework-simplejwt`. Every endpoint below except `register`/`login`/`refresh`
requires `Authorization: Bearer <access>`. A user belongs to exactly one `Business` in V1 via
`Membership` (role `owner` or `staff`); the backend derives `business` from the authenticated
user on every request — the client never sends a business id.

### `POST /api/auth/register/`
Creates the User + Business + owner Membership in one step (the "let's set up your business"
first-run flow). Body:
```json
{
  "email": "thabo@thabosplumbing.co.za",
  "password": "string, min 8 chars",
  "first_name": "Thabo",
  "last_name": "Nkosi",
  "business": {
    "name": "Thabo's Plumbing & Maintenance",
    "trading_name": "",
    "registration_number": "",
    "tax_number": "",
    "vat_number": "",
    "is_vat_registered": false,
    "phone": "+27821234567",
    "email": "info@thabosplumbing.co.za",
    "address_line1": "12 Vygie Street",
    "address_line2": "",
    "suburb": "Delft",
    "city": "Cape Town",
    "province": "Western Cape",
    "postal_code": "7100",
    "industry": "plumbing"
  }
}
```
`201` → `{ "access", "refresh", "user": {...}, "business": {...} }`. `400` → field errors.

### `POST /api/auth/login/` → `{email, password}` → `201`/`200` `{access, refresh, user, business}`
### `POST /api/auth/refresh/` → `{refresh}` → `200` `{access}`
### `GET /api/business/me/` / `PATCH /api/business/me/` (multipart when sending `logo`)

## Standard CRUD resources

Each of the resources below has a `ModelViewSet` at `/api/<resource>/` (list/retrieve/create/
update/partial_update — no hard delete via REST, only soft delete via sync `deleted_at`).
These exist for direct reads (e.g. the initial full pull is really the sync pull below, but
these are used by the Django admin and are convenient for testing). **The Android app's main
read/write path is the sync endpoints in the next section** — Room is the source of truth on
device; these per-resource endpoints are not polled by the app during normal use.

Resources: `leads`, `customers`, `quotes`, `quote-line-items`, `jobs`, `invoices`,
`invoice-line-items`, `payments`, `expenses`. Field shapes are exactly the sync `fields`
payloads documented below, plus the DRF-standard `id`, `created_at`, `updated_at`,
`deleted_at`. `expenses` additionally has `POST /api/expenses/{id}/receipt/` — see "Expense
receipt attachments" at the end of this file.

## Sync

Nine syncable models in this slice, referenced by these `model` keys:
`lead`, `customer`, `quote`, `quote_line_item`, `job`, `invoice`, `invoice_line_item`,
`payment`, `expense`. All are scoped to the caller's business server-side; a client never
sends `business`. Note `expense`'s `receipt_image` field travels through `GET pull` (so other
devices learn a receipt was attached) but is never writable through `push` — see "Expense
receipt attachments" below for how the photo itself gets there.

### `POST /api/sync/push/`
```json
{
  "changes": [
    {
      "model": "lead",
      "id": "b3b2b6d0-...-uuid",
      "updated_at": "2026-08-21T09:00:00Z",
      "deleted_at": null,
      "fields": { "...": "model-specific, see below" }
    }
  ]
}
```
The client may list `changes` in any order — the server applies them within the batch in a
fixed dependency order (customer/lead → quote → quote line item → job → invoice → invoice
line item → payment → expense) so a line item (or an expense referencing a job) ahead of its
not-yet-applied parent in the list still resolves correctly. The one case this doesn't cover:
converting a lead to a customer where
the Customer.source_lead reference or the Lead.converted_customer reference points at a
record created earlier in the *same* batch on the *other* side of that pair — expected to
span two sync cycles in practice, since a lead is captured well before it's converted.

Applied as **one DB transaction**, per-record last-write-wins:
- No existing row, or incoming `updated_at` > current server `updated_at` → upsert, response
  `status: "accepted"` with the full canonical `server_record` (this is where `number` gets
  assigned for quotes/jobs/invoices on their first successful sync).
- Existing row's `updated_at` >= incoming `updated_at` (someone/something else already wrote
  a newer version) → **no write**, `status: "conflict"`, `server_record` = current server
  row. The app must surface this as an explicit conflict, not silently drop either side.
- Payload fails validation → `status: "error"`, `errors: {field: [msg]}`. Record stays
  `PENDING`/`FAILED` on device; nothing is dropped.

Response:
```json
{
  "results": [
    {"model": "lead", "id": "b3b2...", "status": "accepted", "server_record": {"...": "..."}}
  ]
}
```
Re-sending the exact same push (e.g. after a dropped connection) is a no-op the second time —
same id, same `updated_at` → falls into the "not newer" branch → `conflict` response carrying
back the row that was already accepted the first time, which the client recognises as
identical to what it has and simply marks `SYNCED`. This is the documented duplicate-sync
handling.

### `GET /api/sync/pull/?since=<iso8601, optional>`
Omit `since` for the first sync (full snapshot). Response:
```json
{
  "server_time": "2026-08-21T10:20:00Z",
  "changes": [
    {"model": "customer", "id": "...", "updated_at": "...", "deleted_at": null, "fields": {...}}
  ]
}
```
`server_time` is captured **before** the query runs and must be used as the next `since` —
this avoids missing a row written during the request. A client never lets a pulled row
overwrite a still-`PENDING` local row for the same id; it queues the pulled row and resolves
it through the same conflict path once the pending push completes.

## Model field payloads (`fields` in sync, and the CRUD body)

**lead**: `name*, phone*, email, source*(whatsapp|call|facebook|website|email|referral|walkin|tender|other), enquiry, notes, status*(new|contacted|quoted|converted|lost), follow_up_date, converted_customer_id`

**customer**: `name*, customer_type*(individual|company), phone*, email, address_line1, address_line2, suburb, city, province, postal_code, notes, source_lead_id`

**quote**: `customer_id*, lead_id, number(server-assigned, read-only), status*(draft|sent|accepted|declined|expired), issue_date*, valid_until, notes, terms, is_vat_applicable*, discount_amount, subtotal(computed), vat_amount(computed), total(computed), sent_at, accepted_at, declined_at`

**quote_line_item**: `quote_id*, description*, quantity*, unit_price*, line_total(computed), sort_order`

**job**: `customer_id*, quote_id, number(server-assigned, read-only), title*, description, status*(not_started|in_progress|completed|cancelled), start_date, due_date, completed_date`

**invoice**: `customer_id*, job_id, quote_id, number(server-assigned, read-only), status*(draft|sent|partially_paid|paid|overdue|cancelled), issue_date*, due_date, notes, terms, is_vat_applicable*, discount_amount, subtotal(computed), vat_amount(computed), total(computed), amount_paid(computed, read-only), sent_at`

**invoice_line_item**: `invoice_id*, description*, quantity*, unit_price*, line_total(computed), sort_order`

**payment**: `customer_id*, invoice_id(null = payment on account), amount*, method*(cash|eft|card|snapscan|other), reference, paid_date*, notes`

**expense**: `job_id, category*(materials_stock|fuel_travel|tools_equipment|rent|utilities|insurance|bank_charges|professional_fees|marketing|telephone_internet|vehicle|repairs_maintenance|wages_subcontractors|other), description, amount*, is_vat_applicable*, vat_amount(computed), date*, receipt_image(read-only URL or null — see addendum)`

`*` = required. `subtotal`/`vat_amount`/`total`/`line_total`/`amount_paid` are always
recomputed server-side from line items/payments on write — a client may compute them locally
for instant offline UI, but the server value on the `server_record` echoed back is
authoritative and the client overwrites its local value with it.

VAT, quotes/invoices: flat 15%, **added on top** of `subtotal - discount_amount` when
`is_vat_applicable` is true — the owner builds up a subtotal from line items and VAT is added.

VAT, expenses: the opposite direction. `amount` is the VAT-**inclusive** total the owner
already paid (what a receipt or bank statement shows); `vat_amount` is the portion of that
total that was already VAT, extracted as `amount * 15/115` when `is_vat_applicable` is true,
`0.00` otherwise (e.g. a non-VAT-registered supplier, or a bank charge). `amount` is never
added to; it's the number that matters for cash flow, and `vat_amount` is informational, for
the owner's SARS input-VAT records. `date` cannot be more than 1 day in the future (a hard
stop against a wrong year, not a strict same-day rule); `amount` must be greater than zero.

## Local-only Android fields (never sent to / received from the server)

`sync_state` (`PENDING|SYNCING|SYNCED|FAILED`) on every syncable Room entity — drives the
per-record sync badge in the UI. Set to `PENDING` on any local create/edit, `SYNCING` while a
push is in flight, `SYNCED` on `accepted`, `FAILED` on `error` (with the error message kept
for display), and left as the flagged-conflict state on `conflict` until the user resolves it.

## Expense receipt attachments

A receipt photo is binary, and the `changes` batch in `POST /api/sync/push/` is JSON — it
doesn't fit the sync protocol above, so it travels as a second, separate phase specific to
expenses:

### `POST /api/expenses/{id}/receipt/`
`multipart/form-data`, one field: `receipt` (image, ≤10MB — anything Pillow can decode as an
image; a non-image file is rejected with `400`). Requires the expense to already exist under
the caller's business (tenant-scoped lookup, same as every other endpoint) — `404` otherwise.
`200` → the full `ExpenseSerializer` representation, `receipt_image` now a URL. Uploading
bumps the expense's `updated_at`, so the new receipt reaches other devices through the normal
`GET /api/sync/pull/` path — no separate "attachment sync" endpoint is needed on the read
side, only on the write side.

**Why the expense must exist first, and what that means for an offline capture:** the receipt
is stored keyed by the expense's id, which the client already has (client-generated UUID) —
but the server-side row it attaches to only exists once that expense's own JSON `push` has
been `accepted`. A photo captured while offline is therefore held on the device (local file,
not yet uploaded) until its parent expense reports `SYNCED`; only then does the next sync
cycle attempt the multipart upload. This ordering is a client-side responsibility — the server
enforces it passively, by 404ing an upload for an id it doesn't have — not something the sync
protocol's dependency ordering (which only covers the JSON `changes` batch) handles for you.
