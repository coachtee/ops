# OPS — Discovery & Architecture

**"Run your business from your phone."**

This document is the product/technical analysis required before any code was written. It
covers what OPS is, the V1 scope, the architecture, the domain model, and the plan for the
first vertical slice. Everything after this document (backend, Android app) implements the
decisions made here.

---

## 1. Understanding of OPS

OPS is a mobile-first, offline-first operating system for South African micro and small
businesses (1–100 people, V1 optimised for 1–10). It is not an ERP and must never feel like
one. It is organised around **what the owner is trying to do right now** (add a customer,
send a quote, see who owes me money), not around accounting/ERP modules.

The owner's mental model is a single loop:

> Someone gets in touch → I quote them → they say yes → I do the work → I get paid.

Everything else (leads, jobs, invoices, employees, compliance) is scaffolding around that
loop, and around the two questions every owner asks constantly: **"Who owes me money?"**
and **"How is my business doing?"**

OPS succeeds if a first-time owner, handed the phone, can act without training — and if the
app keeps working when their data drops, because that's the real operating environment for
this audience.

## 2. The core South African small-business lifecycle

Across the consulting, security, paving, and retail examples in the brief, one lifecycle
repeats:

```
ENQUIRY → LEAD → CUSTOMER → QUOTE → ACCEPTED → WORK/JOB → INVOICE → PAYMENT → DONE
```

Running alongside it, independent of any single job:

```
MONEY IN (payments)  +  MONEY OUT (expenses)  →  business financial picture
PEOPLE → EMPLOYEES → SHIFTS/PAY → PAYSLIP
COMPLIANCE: SARS / VAT / provisional tax / PAYE-UIF-SDL / CIPC annual return
```

V1 is built to make the first loop (enquiry → paid) effortless from a phone, offline. The
other two threads (money, people, compliance) are designed now so the domain model doesn't
need rework later, but only "money in / money out" gets real screens in V1; people and
compliance are scoped for the release immediately after.

## 3. Minimum V1 capability set

1. **Business setup** — name, trading name, CIPC reg no, tax no, VAT no (optional), address,
   phone, email, industry, logo. Becomes the letterhead on quotes/invoices.
2. **Leads** — fast capture (name, phone, source, enquiry, follow-up date), call/WhatsApp/
   email actions, convert to customer, "who must I call today".
3. **Customers** — one screen showing the whole relationship: quotes, jobs, invoices,
   outstanding balance, notes.
4. **Quotes** — create → preview → send, with business branding, VAT, line items, validity;
   accept/decline; accepted quote becomes a job.
5. **Work/Jobs** — the plain-language wrapper around "project" for whichever trade the owner
   is in; status, dates, linked quote, linked invoice.
6. **Invoices & payments** — invoice from a job or from scratch, record payments (full or
   partial), see outstanding.
7. **Home dashboard** — today's money in, outstanding total, leads needing follow-up, active
   jobs, quick actions. Answers "how is my business doing" in one glance.
8. **Offline-first sync** — everything above must be usable with zero connectivity, with a
   visible saved/syncing/synced/failed state per record.

Designed in the domain model, targeted for the releases immediately following V1:

9. Expenses & suppliers (the "money out" half of the financial picture).
10. Employees, shifts and payslips.
11. Compliance reminders (SARS/VAT/provisional tax/CIPC deadlines — track & remind, never
    "submit").
12. Reports as answers ("what did I make this month", "what are my biggest expenses").

## 4. Explicitly NOT in V1

- Multi-currency, multi-branch, multi-warehouse.
- Full accounting (general ledger, trial balance, chart of accounts, journal entries).
- POS / barcode retail checkout (Example D is future scope, not V1).
- Payroll tax computation or e-filing; any claim of submitting to SARS/CIPC.
- Complex RBAC/permission matrices — V1 has **Owner** and **Staff** only.
- Tendering/RFQ workflow automation, procurement approval chains.
- Configurable workflow builder, custom fields, custom statuses.
- Native iOS app, desktop app, and a marketing website — Android is the product for V1.
- AI features not explicitly requested (no "AI assistant" bolted on for its own sake).
- CRDT/operational-transform conflict resolution — V1 uses a documented, explicit
  last-write-wins protocol (see §6) that is honest about its limits rather than a
  complex merge engine nobody asked for.

## 5. Android information architecture

Bottom navigation, four destinations plus one FAB — nothing nested more than two levels deep,
because a phone screen and a nervous first-time user can't hold more than that.

```
┌───────────────────────────────────────────────────────┐
│  HOME   │  LEADS  │  CUSTOMERS  │  MONEY               │  ← bottom nav
└───────────────────────────────────────────────────────┘
                    (+) quick-add FAB on Home:
                    New lead · New customer · New quote ·
                    New invoice · Record payment

HOME
 └─ today snapshot, quick actions, follow-ups due, active jobs
LEADS
 └─ Lead list (filter: needs follow-up / all)
     └─ Lead detail → call/WhatsApp/email, note, follow-up date,
                       Convert to customer, Create quote
CUSTOMERS
 └─ Customer list (search)
     └─ Customer detail → quotes / jobs / invoices / outstanding / notes
         └─ Quote detail → edit (draft) / preview / send / accept / decline
             └─ (accepted) → Job (auto-created)
                 └─ Job detail → status, dates, → Create invoice
                     └─ Invoice detail → preview / send / Record payment
MONEY
 └─ Outstanding invoices · Payments received · (Expenses — next milestone)
```

Business Setup is a one-time flow shown before Home on first launch, and reachable later from
a lightweight Settings screen (not a full settings module in V1 — just business profile,
logo, and account/sync status).

## 6. Offline/sync architecture

**Principle:** Room (SQLite) on-device is the source of truth for the UI. The network is a
background concern the owner is never blocked on.

- **IDs:** every syncable record gets a client-generated UUID at creation time (`id`), so
  creation always works offline with zero collision risk — no round trip is needed to get an
  ID before the owner can keep working.
- **Change tracking:** each syncable table carries `updated_at` (UTC), `deleted_at`
  (soft delete, so deletions sync too), and a **local-only** `sync_state` enum
  (`PENDING` → `SYNCING` → `SYNCED` / `FAILED`) that drives the per-record sync badge the
  brief requires — the owner always knows if a record is only on their phone.
- **Push:** `POST /api/sync/push/` — batched, one transaction, upsert per record.
  - If no existing server row, or the incoming `updated_at` is newer → accept, echo back the
    canonical server row (this is where numbers get assigned, see below).
  - If the server's current `updated_at` is newer than the incoming one → **conflict**: the
    server does **not** overwrite. It returns its current row. The client keeps the user's
    pending edit visible as a flagged local conflict ("your change" vs "their change") rather
    than silently discarding either side — the owner resolves it explicitly. This is the one
    realistic conflict case at this scale (owner + 1–2 staff, occasionally two devices editing
    the same record while both offline); a full CRDT engine is not justified for that.
  - Because the record id is stable and the payload is idempotent, replaying the same push
    (e.g. after a dropped connection) is a no-op the second time — solves duplicate-sync
    safely without a separate dedup table.
- **Pull:** `GET /api/sync/pull/?since=<cursor>` — server returns every row touched after the
  cursor, scoped to the business, plus a new cursor (taken at query start, not query end, to
  avoid a race that would miss rows written mid-request). Client upserts into Room; a pull
  never overwrites a still-`PENDING` local row (that would be silent data loss) — it queues
  behind the next push/conflict check instead.
- **Human-readable numbers** (quote/invoice/job numbers): assigning these offline on
  multiple devices risks collisions or gaps that confuse an owner ("why do I have two Quote
  #14s"). V1's explicit decision: numbers are assigned **by the server on first successful
  sync**, atomically per business. Until then the record shows "Draft — not yet synced" in
  the UI instead of a number. This is a deliberate, documented trade-off, not an oversight.
- **Trigger:** WorkManager runs sync opportunistically (connectivity regained, app
  foregrounded, periodic ~15 min backoff) and on explicit pull-to-refresh. Sync is always a
  background job — never a blocking spinner on the owner's typing.
- **Failure:** a push that errors (validation, auth, server 5xx) leaves the record `FAILED`
  with a visible retry action; it never gets silently dropped from the outbox.

## 7. Django backend architecture

- **Django 5 + Django REST Framework + PostgreSQL.** Modular monolith — one deployable, apps
  are internal boundaries, not services. No microservices; nothing here needs them at this
  scale, and they'd slow the team down for no benefit.
- **Multi-tenant by row:** every business-owned table has a `business` FK; a
  `TenantScopedManager`/permission class enforces that every query and write is scoped to the
  authenticated user's business. Simple, proven, avoids schema-per-tenant operational cost.
- **Auth:** JWT (access + refresh) via `djangorestframework-simplejwt`, since the Android app
  is a long-lived offline client, not a browser session.
- **Apps:**
  - `accounts` — User, Business, Membership (role: owner/staff).
  - `crm` — Lead, Customer.
  - `sales` — Quote, QuoteLineItem.
  - `work` — Job.
  - `finance` — Invoice, InvoiceLineItem, Payment (Expense/Supplier modelled, not exposed via
    API yet).
  - `sync` — the generic push/pull machinery in §6, model-registry driven so new syncable
    models opt in with one line, not a bespoke endpoint each.
- **Money:** `DecimalField`, never float. VAT is a flat 15% (current SA rate) computed
  server-side as the source of truth, with the same calculation duplicated intentionally (not
  copy-pasted blindly) in the Android domain module so quote/invoice totals are correct
  offline before any sync happens.
- **PDF/preview:** quotes/invoices render to HTML server-side (and the same HTML is what the
  Android WebView-based preview shows before "Send") so there is exactly one layout to
  maintain, not two.

## 8. Core domain model

```
Business ─┬─< Membership >─ User
          ├─< Lead ─────────────── (converted_to → Customer)
          ├─< Customer ─┬─< Quote ─┬─< QuoteLineItem
          │             │          └── (accepted →) Job
          │             ├─< Job ───┬── (from Quote, optional)
          │             │          └── (→) Invoice
          │             ├─< Invoice ─┬─< InvoiceLineItem
          │             │            └─< Payment
          │             └─< Payment (also linkable directly to a Customer, on-account)
          ├─< Expense (→ Supplier, → Job)         [modelled, V1.1]
          ├─< Supplier                             [modelled, V1.1]
          └─< Employee ─< Payslip                  [modelled, V1.2]
```

Every business-owned entity: `id (UUID)`, `business`, `created_at`, `updated_at`,
`deleted_at`. Financial documents (Quote/Invoice) additionally carry `status`, `subtotal`,
`vat_amount`, `discount_amount`, `total` — all computed from line items, never entered by
hand, so the numbers can't drift from what's itemised.

## 9. Main user journeys (V1)

1. **First run:** install → "Let's set up your business" (name, address, phone, logo, VAT
   status) → Home.
2. **New enquiry, no signal:** owner taps + → New lead → fills name/phone/source/enquiry →
   saved instantly, badge shows "Saved on this phone" → syncs when back online.
3. **Quote to job to invoice to paid:** Lead → Convert to customer → Create quote → add line
   items → Preview → Send (WhatsApp/email share sheet) → mark Accepted → Job auto-created →
   owner updates job status as work happens → Create invoice from job → Send → Record
   payment → invoice shows Paid, job can be marked Completed.
4. **"Who owes me money":** Money tab → Outstanding, sorted oldest-first, tap → call the
   customer straight from the invoice.
5. **"How's my business doing":** Home tab, no navigation required.

## 10. Screen list (V1 vertical slice, built now)

1. Splash / auth check
2. Business Setup (multi-step-lite: details → address → logo)
3. Home dashboard
4. Leads list
5. Lead detail / edit
6. New lead
7. Customer list
8. Customer detail
9. New/edit quote (line items)
10. Quote preview (branded)
11. Job detail
12. New/edit invoice (from job, with line items pre-filled from job/quote)
13. Invoice preview (branded)
14. Record payment
15. Sync status sheet (what's pending, retry)

Not built this slice, designed for the next: Expenses, Suppliers, Employees, Payslips,
Compliance calendar, full Reports tab — these are additive screens on the same architecture,
not a redesign.

## 11. MVP development sequence

1. **This deliverable — vertical slice:** Business setup → Customer → Lead → Quote → Job →
   Invoice → Payment, offline-first on Android, syncing to the Django backend. (What's built
   below.)
2. Money out: Expenses + Suppliers, Home dashboard gains "money spent" and a real profit
   figure.
3. Reports tab: the question-shaped reports in §18 of the brief, built on data that already
   exists by then.
4. People: Employees, shifts, payslips (starts simple — three fields and a payslip PDF, not a
   workforce-management system).
5. Compliance: SARS/VAT/PAYE/CIPC deadline tracker with reminders and accountant-ready
   exports — explicitly "helps you prepare", never "submits for you".
6. Hardening: conflict-resolution UX polish, backup/restore, multi-device QA, performance on
   low-end Android hardware and 2G/3G networks.

## 12. Risks and assumptions

- **Assumption:** VAT is modelled at the current flat 15% SA rate, server-configurable per
  business (vat-registered vs not) but not multi-rate — reasonable for V1's target segment.
- **Assumption:** "Staff" role in V1 can see and edit business data but not business
  setup/billing; a finer permission matrix is deferred until real usage shows it's needed.
- **Assumption:** one business per install for V1 (an owner running two businesses uses two
  installs or accounts) — multi-business switching is a plausible fast-follow, not V1.
- **Risk:** last-write-wins sync is the right complexity level for 1–10 people, but will need
  revisiting if OPS grows into the 50–100 person segment mentioned as a "eventually" — flagged
  now so the `sync_state`/conflict fields aren't a dead end.
- **Risk:** low-end Android hardware and patchy connectivity are the primary usage
  environment, not the exception — every screen must render instantly from Room and never
  block on network; this is a standing constraint on all future screens, not just this slice.
- **Environment constraint (this session):** the build sandbox used to produce this slice has
  no Android SDK/emulator installed, so the Android module's compile/runtime behaviour could
  not be executed and verified here the way the Django backend's tests could. This is called
  out explicitly rather than claiming a UI test that didn't happen — see the Android
  module's own README/NOTES for exactly what was and wasn't verified, and run it in Android
  Studio / CI to confirm the build.
- **Compliance honesty:** nothing in OPS claims to file with SARS or CIPC. Compliance
  features (from milestone 5 onward) are tracking, reminders, and accountant-ready exports
  only, and must say so on-screen every time, per the brief's explicit instruction not to
  fabricate regulatory certainty.
