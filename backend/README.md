# OPS backend

Django + Django REST Framework + PostgreSQL. See `../docs/DISCOVERY.md` for why it's built
this way and `../docs/API_CONTRACT.md` for the exact API the Android app consumes.

## Run it

```bash
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt

# Postgres (default) — create a database first, e.g.:
#   createuser ops --createdb; createdb -O ops ops
export OPS_DB_HOST=localhost OPS_DB_NAME=ops OPS_DB_USER=ops OPS_DB_PASSWORD=ops
.venv/bin/python manage.py migrate
.venv/bin/python manage.py seed_demo      # Thabo's Plumbing & Maintenance demo data
.venv/bin/python manage.py createsuperuser  # optional, for /admin/
.venv/bin/python manage.py runserver 0.0.0.0:8000
```

For a quick local run without Postgres, set `OPS_USE_SQLITE=1` instead of the `OPS_DB_*`
vars.

Demo login (after `seed_demo`): `thabo@thabosplumbing.co.za` / `Demo12345`.

## Test

```bash
export OPS_DB_HOST=localhost OPS_DB_NAME=ops OPS_DB_USER=ops OPS_DB_PASSWORD=ops
.venv/bin/python manage.py test tests
```

42 tests: money/VAT math, auth + registration, quote/invoice line-item totals recomputation,
job/quote/invoice numbering, invoice payment-state transitions (including reversing a
payment), cross-tenant IDOR guards, and the sync engine — accept/conflict/error, idempotent
replay of a dropped-connection retry, out-of-order batch application, and a full offline
session (customer + invoice + line item + payment) synced in a single push.

## What's deliberately not production-hardened yet

This is the vertical slice's backend, not a deployed instance. `manage.py check --deploy`
will flag: `DEBUG=True` by default, a placeholder `SECRET_KEY`, and no HTTPS/HSTS/secure-cookie
settings — all controlled by `OPS_DEBUG`/`OPS_SECRET_KEY`/env vars, and all things to set
explicitly before any real deployment, not oversights in this slice.

## Layout

- `accounts/` — User, Business, Membership; registration/login/business-profile endpoints.
- `crm/` — Lead, Customer.
- `sales/` — Quote, QuoteLineItem.
- `work/` — Job ("work" in the product UI).
- `finance/` — Invoice, InvoiceLineItem, Payment (+ Supplier/Expense, modelled for V1.1, not
  exposed via API yet).
- `sync/` — the offline-sync push/pull engine; `sync/registry.py` is where new syncable
  models get wired in.
- `common/` — the shared `BusinessOwnedModel` base, VAT/money math, tenant-scoping helpers.
- `tests/` — see above.
