# OPS

**Run your business from your phone.**

OPS is a mobile-first, offline-first operating system for South African small businesses —
built around the real lifecycle of a small business (enquiry → lead → customer → quote → job →
invoice → payment), not around ERP/accounting modules. See `docs/DISCOVERY.md` for the full
product analysis and architecture, and `docs/API_CONTRACT.md` for the binding contract between
the two halves of this repo.

## Repo layout

- **`docs/`** — start here. `DISCOVERY.md` is the product/technical analysis (what OPS is, the
  V1 scope, the domain model, the offline-sync design, the screen list, the build sequence).
  `API_CONTRACT.md` is the exact API the Android app implements against.
- **`backend/`** — Django + DRF + PostgreSQL. The sync authority: auth, the business domain
  model, VAT/totals calculation, document numbering, and the offline-sync push/pull engine.
  See `backend/README.md` to run it and its 43 automated tests.
- **`android/`** — the Android app (Kotlin, Jetpack Compose, Room, Hilt, WorkManager). This is
  the product — the primary and, for V1, only user-facing surface. See `android/README.md`.

## The vertical slice

The first working deliverable, per the brief, is the full loop end to end: business setup →
customer → lead → quote → work/job → invoice → payment, offline-first, using a realistic South
African demo business (Thabo's Plumbing & Maintenance, seeded by
`backend/manage.py seed_demo`). Expenses, suppliers, employees/payslips, and compliance
reminders are modelled in the backend's domain but intentionally not built into this slice —
see `docs/DISCOVERY.md` sections 3–4 and 10–11 for what's in V1 versus deferred, and why.

## Quickstart

```bash
# Backend
cd backend
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
export OPS_DB_HOST=localhost OPS_DB_NAME=ops OPS_DB_USER=ops OPS_DB_PASSWORD=ops
.venv/bin/python manage.py migrate
.venv/bin/python manage.py seed_demo
.venv/bin/python manage.py runserver 0.0.0.0:8000

# Android — open android/ in Android Studio, point it at the backend above,
# log in with thabo@thabosplumbing.co.za / Demo12345. See android/README.md.
```
