# OPS — Android

Mobile-first, offline-first Android client for OPS, a South African small-business operating
system. See `../docs/DISCOVERY.md` for the product/architecture rationale and
`../docs/API_CONTRACT.md` for the exact network contract this app implements against. This
module implements the V1 vertical slice — business setup, leads, customers, quotes, jobs,
invoices, payments, the home dashboard, and offline sync — plus the Expenses milestone
(capture, receipt photo attachment, VAT-inclusive extraction, categories, optional job/project
link), the Suppliers milestone (a simple contact record — who the business buys from — linked
from Expense.supplier_id), the Employees & Payslips milestone (a staff contact + agreed pay
rate, and one pay period's gross/deductions/computed-net-pay per employee — deliberately no
shift tracking, no leave management, no PAYE/UIF tax-table computation), the Compliance
milestone that followed it (a plain owner-managed deadline checklist — VAT return, PAYE/UIF/
SDL, provisional tax, CIPC annual return, or anything else the owner adds — with a due date, a
tick-off, and an on-screen reminder every time that nothing here files with SARS or CIPC), and
the Reports milestone (a fifth bottom-nav tab: profit by month, biggest expense categories this
month, VAT collected vs paid this month — all computed on demand from data already synced
locally, never a new stored model or an extra network call). See DISCOVERY.md section 10 for
what's still deliberately deferred (accountant-ready exports, hardening).

## Module layout

A two-module Gradle project, deliberately split so the parts that can be verified in a
sandbox with no Android SDK are physically separate from the parts that can't:

```
android/
├── core-domain/     pure Kotlin/JVM — VAT math, sync decision logic, ISO timestamps, wire enums
└── app/             Android application — Compose UI, Room, Hilt, Retrofit, WorkManager
```

### `core-domain` (`org.jetbrains.kotlin.jvm` plugin only, zero Android dependency)

- `Money.kt` — VAT_RATE = 0.15 (BigDecimal), `computeLineTotal`, `computeDocumentTotals`.
  Mirrors `backend/common/money.py` field-for-field: quantize to 2dp HALF_UP, discount applied
  before VAT, taxable amount never negative, VAT is 0.00 when not applicable. Also
  `extractVatFromInclusive` for expenses, which run VAT the *opposite* direction — the owner
  already knows the total paid, and this extracts the VAT portion already inside it
  (`amount * 15/115`) rather than adding VAT on top of a subtotal.
- `SyncDecision.kt` — `decideSyncOutcome(existingUpdatedAt, incomingUpdatedAt)`, the same
  last-write-wins comparison `backend/sync/services.py` makes server-side. The Android sync
  client (`app`'s `SyncManager`) calls this on every pulled row before letting it overwrite a
  clean local record.
- `IsoTimestamp.kt` — formats/parses the wire's `...Z`-suffixed UTC timestamps, deliberately
  never `+00:00` (an un-encoded `+` in a URL query string decodes as a space under
  form-encoding, corrupting the sync `since` cursor — see API_CONTRACT.md's opening section).
- `Enums.kt` — `LeadSource`, `LeadStatus`, `QuoteStatus`, `JobStatus`, `InvoiceStatus`,
  `PaymentMethod`, `CustomerType`, `ExpenseCategory`, each carrying the exact wire string from
  the matching Django model's `choices`, checked against
  `backend/{crm,sales,work,finance}/models.py`.

### `app` (Android application, `com.ops.app`, Jetpack Compose, Material 3)

- `data/local/` — Room entities/DAOs/`OpsDatabase`. Every money field (quantity, unit_price,
  line_total, subtotal, vat_amount, total, discount_amount, amount_paid, amount) is a TEXT
  column holding the canonical decimal string — never REAL/float. `ExpenseEntity` additionally
  carries local-only receipt state (`localReceiptPath`/`receiptSyncState`/`receiptSyncError`,
  see `ReceiptSyncState`) — a second state machine independent of the record's own
  `syncState`, since a receipt photo travels through a different sync path (see below), and a
  nullable `supplierId` (v3). `SupplierEntity` is a deliberately small contact record — name,
  contact person, phone, email, notes — not a vendor-management module. `EmployeeEntity`
  (v4) is the same idea for staff — name, role, contact details, agreed pay rate — and
  `PayslipEntity` (v4) carries `netPay` always derived from `grossPay - deductions` (never
  hand-entered), same pattern as `ExpenseEntity.vatAmount`. `ComplianceItemEntity` (v5) is a
  plain deadline row (category, title, dueDate, completedDate, isRecurring, notes) — no
  relations to any other entity. Schema history: `v2` added `ExpenseEntity`; `v3` added
  `SupplierEntity` and `ExpenseEntity.supplierId`; `v4` added `EmployeeEntity` and
  `PayslipEntity`; `v5` added `ComplianceItemEntity`. None of these has a migration path from
  the version before it — `fallbackToDestructiveMigration()` — since this app has never
  shipped; that stops being acceptable once it does.
- `data/remote/` — Retrofit `OpsApiService` (every endpoint in API_CONTRACT.md, including the
  multipart `POST /api/expenses/{id}/receipt/`), kotlinx.serialization DTOs,
  `AuthHeaderInterceptor` + `TokenAuthenticator` (401 → refresh once → retry).
- `data/sync/` — `SyncManager`, the offline-sync engine's client half: push → mark
  accepted/conflict/error → pull → upsert-if-safe → persist cursor, THEN a second, separate
  phase (`syncReceipts`) that uploads any expense's local receipt photo once — and only once —
  that expense's own JSON record has confirmed SYNCED (the upload 404s otherwise; see
  API_CONTRACT.md's "Expense receipt attachments"). Each receipt upload's failure is isolated
  per-record and never fails the overall sync outcome. Plus `SyncWorker` (WorkManager, ~15 min
  periodic heartbeat + expedited one-time trigger after local writes).
- `data/repository/` — one repository per aggregate (Lead, Customer, Quote+line items,
  Job, Invoice+line items, Payment, Supplier, Expense, Employee, Payslip, ComplianceItem,
  Business, Auth) plus `SyncStatusRepository` for the sync status screen's cross-model view.
  `ExpenseRepository.attachReceipt`/`retryReceipt` manage the receipt state machine;
  `save`/`delete` are the usual PENDING-then-sync pattern, same for `SupplierRepository`,
  `EmployeeRepository`, and `ComplianceItemRepository`. `PayslipRepository.save` computes
  `netPay` locally via the new `Money.computeNetPay` (core-domain) before writing, same
  instant-offline-UI reasoning as `ExpenseRepository.save`'s VAT extraction.
- `di/` — Hilt modules for Room, Retrofit/OkHttp, WorkManager. `AuthPreferences`
  (DataStore-backed) and every repository are constructor-injected directly (`@Inject
  constructor`), which is itself Hilt DI — no separate binding module is needed for concrete
  classes with no interface to bind against.
- `ui/` — one package per screen area (`splash`, `businesssetup`, `home`, `leads`,
  `customers`, `quotes`, `jobs`, `invoices`, `payments`, `expenses`, `suppliers`, `employees`,
  `compliance`, `reports`, `money`, `syncstatus`, `settings`), each with a `@HiltViewModel` + a
  Compose screen, plus
  `ui/navigation/OpsNavGraph.kt` wiring all of them together and `ui/components/` for shared
  pieces (money/date formatting, the sync status chip, the branded quote/invoice letterhead, a
  date picker field, dropdowns). `ui/expenses/ExpenseEditScreen` is one screen for create,
  edit, and view (delete + camera/gallery receipt capture live there too), following
  `JobDetailScreen`'s "always editable, no separate view/edit mode" spirit rather than the
  3-screen split (list/new-edit/detail) originally sketched — see Scope notes.
  `ui/suppliers/SupplierEditScreen` follows the same single-screen pattern (create, edit, view,
  delete, plus call/WhatsApp/email quick actions reusing `LeadDetailScreen`'s `Intent` pattern
  and a read-only list of that supplier's expenses below the form); `SupplierListScreen` is a
  plain alphabetical contact list, reached from the Money tab rather than its own bottom-nav
  tab — "Money in → Money out → Expenses → Suppliers" is one conceptual thread, not a separate
  app section. `ui/employees/EmployeeEditScreen` follows the identical single-screen pattern
  (plus a "Payslips" section listing that employee's payslip history, each row navigating to
  `PayslipEditScreen`); `EmployeeListScreen` is reached from Business Profile/Settings rather
  than the Money tab or a bottom-nav tab of its own — staff management reads as "part of
  running the business" to a real owner, not a daily transactional workflow like leads or
  invoices. `PayslipEditScreen` is its own single create/edit/view/delete screen (period dates,
  gross pay, deductions, a live computed net-pay line, "mark as paid today", and a plain-text
  "Share payslip" action via `Intent.ACTION_SEND` — no PDF, same as quote/invoice "Send").
  `ui/compliance/ComplianceEditScreen` is the same pattern again (category, title, due date, a
  "Repeats" toggle, mark-done), with one addition: marking a recurring item done offers an
  "Add the next reminder?" dialog pre-filled at a category-typical interval (computed purely
  client-side, see `ComplianceEditViewModel`'s `suggestedNextItem`) — the owner must tap "Add"
  for it to actually be created, nothing happens automatically. `ComplianceListScreen`, like
  `EmployeeListScreen`, is reached from Business Profile/Settings; both screens carry the
  on-screen reminder that OPS tracks deadlines but never files anything with SARS or CIPC.
  `ui/reports/ReportsScreen` is the app's fifth bottom-nav destination — one scrollable screen,
  no further navigation — showing profit by month (last six calendar months, oldest first),
  biggest expense categories this month, and VAT collected vs paid this month. Every figure is
  computed by `ReportsViewModel` directly from data three existing repositories
  (`PaymentRepository`, `ExpenseRepository`, `InvoiceRepository`) already have synced into Room
  — no new network call, no new stored entity, mirroring `backend/reports/views.py`'s
  aggregation logic (same cash-basis revenue definition, same draft/cancelled exclusion for VAT
  collected) closely enough that the two agree, without needing to be byte-exact the way
  VAT/net-pay math does — see "What was verified" below for why this stayed out of
  `core-domain`.

## Demo script

There is no separate local-only seed/demo mode in the app — by design, so every code path the
demo exercises is the same one real usage exercises. To see realistic data:

```bash
cd ../backend
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
export OPS_DB_HOST=localhost OPS_DB_NAME=ops OPS_DB_USER=ops OPS_DB_PASSWORD=ops
.venv/bin/python manage.py migrate
.venv/bin/python manage.py seed_demo      # Thabo's Plumbing & Maintenance
.venv/bin/python manage.py runserver 0.0.0.0:8000
```

Then, in the Android app (emulator or device on the same network as the backend):

1. First launch → Splash → Business Setup screen (no local business yet).
2. Tap **Sign in** (not Create business — that would register a brand-new, empty business).
3. `thabo@thabosplumbing.co.za` / `Demo12345`.
4. The app signs in, then the normal sync engine (WorkManager's post-write trigger fires once
   on first launch, plus the periodic heartbeat) pulls Thabo's seeded leads, customers, quotes,
   jobs, invoices, payments and expenses down through the real `GET /api/sync/pull/` path — the
   same path any other data uses. Pull-to-refresh on Home forces this immediately rather than
   waiting.
5. Money tab → **+** → record an expense (amount, VAT toggle, category, optional job link),
   Save, then **Take photo**/**Choose photo** to attach a receipt — the photo uploads on the
   next sync cycle once the expense record itself has synced. Home's "Money out" tile and the
   Money tab's Expenses section both update from the same local data immediately, offline or
   not.
6. Home → gear icon (top-right) → Business profile → **Manage employees** → open Bongani
   Sithole (Thabo's seeded plumber's assistant) → **+** under Payslips → gross pay and
   deductions, net pay works itself out → Save → **Mark as paid today** once the money's
   actually gone out.
7. Business profile → **Compliance reminders** → see the seeded PAYE/UIF/SDL and CIPC items →
   open the upcoming PAYE/UIF/SDL one → **Mark done today** → Save → the app offers to add
   next month's reminder; tap **Add** to confirm it (or **Not now** to skip — nothing is ever
   created without that explicit tap).
8. **Reports** tab (5th bottom-nav icon) → Thabo's actual August figures roll up immediately
   from the same already-synced local data: profit by month with this month's revenue/expenses/
   profit, biggest expense categories this month, and VAT collected vs paid (R0.00 collected —
   Thabo isn't VAT-registered — against whatever VAT was extracted from this month's expenses).
   No extra sync or network call happens when this tab opens.

`app/build.gradle.kts`'s debug `BASE_URL` is `http://10.0.2.2:8000/` — the Android emulator's
alias for the host machine's localhost, matching `runserver 0.0.0.0:8000` above. A physical
device on the same Wi-Fi needs that changed to the host's real LAN IP instead.

## Opening/building in Android Studio

1. Open the `android/` directory (this directory, not the repo root) as the project root.
2. Android Studio (Koala/2024.1+ recommended, for AGP 8.5.x support) syncs Gradle
   automatically. It needs an installed Android SDK with `compileSdk 34` / build-tools for 34
   available — the one thing this development sandbox does not have (see below).
3. Run the `app` configuration on an emulator (API 24+) or device.
4. `core-domain`'s tests also run from Android Studio's test runner (right-click
   `core-domain/src/test` → Run), or from the command line — see below.

**No `gradle/wrapper/` is committed.** Generating one (`gradle wrapper --gradle-version
8.14.3`) needs the `app` module's plugins to resolve first, which — same as everything else in
this section — needs Google's Maven repo, unreachable from this sandbox (confirmed: the
`wrapper` task itself failed here with the identical AGP-resolution error as everything else in
this README). This is not a hand-written project file that got missed, it's the same blocked
step. Android Studio generates the wrapper for you on first sync of a wrapper-less project, or
run that same `gradle wrapper` command yourself once you have real network access.

## What was verified in this sandbox, honestly

This sandbox has Java 21, Gradle 8.14.3, and Kotlin tooling, but **no Android SDK and no
emulator**, and the proxy that mediates this session's network access explicitly blocks
`dl.google.com` (confirmed: a direct `curl` to Google's Maven repo returned `403` from the
proxy), which is where AGP's Gradle plugin coordinates resolve from. Both are expected,
documented constraints of this environment, not something to work around.

**Actually run, real output — `core-domain`:**

No `gradlew` wrapper is committed (see below), so this is run with the sandbox's
system-installed Gradle 8.14.3 directly — `core-domain` is a pure-Kotlin/JVM module with no
Android Gradle Plugin dependency, so it needs neither the wrapper nor an SDK:

```
$ gradle :core-domain:test --rerun
...
BUILD SUCCESSFUL in 20s
4 actionable tasks: 4 executed
```

40 tests, 40 passing, 0 failures, 0 errors — confirmed via both the console output and the
JUnit XML result files (`core-domain/build/test-results/test/*.xml`), broken down as:

| Test class                     | Tests | Covers |
|---------------------------------|:---:|---|
| `MoneyTest`                     | 11  | Line total rounding, VAT with/without, discount before VAT, discount > subtotal never negative, empty line items, half-up vs half-even, fractional quantities, the flat 15% rate itself, net pay = gross - deductions, net pay with zero deductions |
| `VatInclusiveExtractionTest`    |  5  | Clean multiples of 115 extract exactly, unclean divisions round half-up, not-VAT-applicable and zero-amount both extract R0.00 — mirrors `backend/tests/test_money.py`'s `VatInclusiveExtractionTests` case-for-case |
| `SyncDecisionTest`              |  5  | No existing row, incoming strictly newer, existing newer (conflict), equal timestamps (conflict — this is also what makes a replayed push idempotent), sub-second precision |
| `IsoTimestampTest`              |  7  | `Z` suffix never `+00:00` (and never a raw `+` at all), zero-microsecond formatting, round-trip through format+parse, nanosecond truncation, the contract's own example value, no-fraction parsing, defensive offset-form parsing |
| `EnumsTest`                     | 12  | Every enum's (incl. `ExpenseCategory`, 14 values) wire values match the Django `choices` list byte-for-byte, `fromWire` round-trips every value, `fromWire` rejects an unknown value, `PayRateType`'s three values match `Employee.PAY_RATE_TYPE_CHOICES`, and (new this milestone) `ComplianceCategory`'s five values match `ComplianceItem.CATEGORY_CHOICES` |

This is the one hard verification gate for this deliverable, and it's genuinely green — not
asserted, run. The Compliance milestone added one `core-domain` enum (`ComplianceCategory`) and
no new money-math function — the "add the next reminder" interval suggestion is deliberately
*not* in core-domain (it isn't a value the server also computes and must match, unlike VAT or
net pay; it's a pure client-side UX nudge, see `ComplianceEditViewModel.suggestedNextItem`), so
it lives in the `app` module instead. **The Reports milestone added zero `core-domain` code** —
a deliberate decision, not an oversight: `ReportsViewModel`'s only arithmetic is plain
`BigDecimal` subtraction (`revenue - expenses`, `vatCollected - vatPaid`), which has no
rounding-mode ambiguity that could cause client/server drift the way VAT extraction or net-pay
computation do, so there is nothing here that *needs* to byte-match a server computation —
following the same precedent `HomeViewModel`'s own stat-card arithmetic already set (inline in
`app`, not routed through `core-domain`). Test count is unchanged at 40/40 as a result.

**Written but NOT compiled or run here — the `app` module:**

Every file under `app/src/main/kotlin` (133 Kotlin files as of the Reports milestone —
Room entities/DAOs, Retrofit service/DTOs, the sync engine, thirteen repositories,
Hilt modules, and 27 Compose screens with their ViewModels) was written carefully, by hand,
cross-checking every field name, wire enum value, and endpoint path against `API_CONTRACT.md`
and the actual Django serializers/models in `../backend/` — but **`app:compileDebugKotlin` was
never successfully run**, because it cannot succeed here: AGP needs `android.jar` from an
installed SDK to compile against, and this sandbox has neither the SDK nor network access to
`dl.google.com` to fetch AGP's own plugin artifact in the first place. Re-confirmed for this
milestone:

```
$ gradle :app:compileDebugKotlin
...
* What went wrong:
Plugin [id: 'com.android.application', version: '8.5.2'] was not found in any of the
following sources: ... could not resolve plugin artifact
'com.android.application:com.android.application.gradle.plugin:8.5.2' ...
BUILD FAILED in 3s
```

The build fails at plugin resolution, before it would even get to the point of missing the
SDK. That is a sandbox limitation, not something wrong with the `app` module's own build
files, which are otherwise a normal, standalone AGP/Compose/Hilt setup.

Several specific things were caught and fixed exactly because of manual re-reading and
cross-checking (not because a compiler caught them) — worth naming so it's clear what "not
compiled" actually risks. From the original vertical slice:

- `RoomDatabase.clearAllTables()` (used on logout) asserts it isn't called on the main thread;
  the original `AuthRepository.logout()` called it directly from a `suspend fun` without
  dispatching to `Dispatchers.IO`, which would have crashed on first logout. Fixed.
- The picked-logo-image preview (Business Setup and Business Profile) originally tried to hand
  Coil's `AsyncImage` a raw `ByteArray` model, which Coil 2.x's default component registry
  doesn't know how to fetch. Fixed to decode via `BitmapFactory` and render with a plain
  Compose `Image` for the not-yet-uploaded preview, reserving Coil for the String-URL case
  (an already-uploaded logo).

From the Expenses milestone:

- `ReceiptSyncState` was first written inside the `entities` package but imported from
  `com.ops.app.data.local` (matching where the pre-existing `SyncState` actually lives) —
  a straight package-path mismatch that would have failed to compile. Moved the file to match.
- `HomeViewModel`'s 6-flow combine initially used `kotlinx.coroutines.flow.combine`'s untyped
  vararg overload (`Array<Any?>` + unchecked casts by index) because the typed overloads only
  go up to 5 flows — correct at runtime, but fragile (a reordered cast silently reads the wrong
  field, with no compiler check). Rewritten as two nested 3-flow typed `combine` calls instead,
  which is both fully type-checked and closer to this codebase's existing style elsewhere.
- A missing `KeyboardOptions` import and one genuinely unused import
  (`RoundedCornerShape`, drafted for a receipt-thumbnail treatment that didn't end up needed)
  in the new `ExpenseEditScreen.kt`.

From the Suppliers milestone:

- Adding `SyncStatusItem.Supplier` as a new sealed-class subclass, while wiring
  `SyncStatusRepository`'s constructor and `observeItems()` flow list, initially left the
  `retry`/`keepMine`/`useTheirs` `when (item) { ... }` blocks without a `Supplier` branch —
  Kotlin's exhaustive-`when`-over-a-sealed-class check would have caught this immediately at
  compile time, but since compilation isn't available here, it had to be caught by re-reading
  the file against the (now six-subclass) sealed class directly. Fixed — all three blocks now
  handle every `SyncStatusItem` subtype.
- A misplaced KDoc comment: the "`receipt_image` is read-only on the wire" doc comment (which
  describes `ExpenseFieldsDto.receiptImage`) ended up sitting directly above the newly-inserted
  `SupplierFieldsDto` instead, in `ModelFieldsDto.kt` — correct code, wrong/misleading
  documentation. Fixed by moving it back above `ExpenseFieldsDto` and giving `SupplierFieldsDto`
  its own one-line comment.
- `SupplierEditViewModel.linkedExpenses` initially `flatMapLatest`'d directly off the full
  `_uiState` flow, which would have re-subscribed to `ExpenseRepository.observeBySupplierId`
  (cancelling and restarting the underlying Room query) on every keystroke in the name/notes
  fields, not just when the supplier's id actually changes. Fixed to `map { it.supplierId
  }.distinctUntilChanged()` first — correct either way, but the unfixed version would have been
  a real, if minor, performance defect (needless Room query churn while typing).
- An early draft of `SupplierEditScreen.kt` accepted an `onOpenExpense` callback parameter but
  never actually wired it to the linked-expense list's `ListItem`s (they weren't clickable), and
  had a leftover no-op `Modifier.let { m -> if (...) m else m }` block that did nothing.
  Fixed — the list is now clickable via `onOpenExpense`, and the dead conditional removed.

From the Employees & Payslips milestone:

- `OpsNavGraph`'s `EMPLOYEE_EDIT` composable originally read the `employeeId` it passed to
  `onOpenPayslip`/`onNewPayslip` straight off the static `NavBackStackEntry` route argument.
  That argument stays the `NONE` sentinel for the whole lifetime of that composable instance —
  it does not update just because `EmployeeEditViewModel.save()` internally resolves a real
  UUID after the first save. Concretely: create a new employee, save it, then immediately tap
  "+ New payslip" without leaving the screen, and the payslip would have been navigated to with
  `employeeId=NONE` instead of the just-created employee's real id — a payslip with no valid
  employee to attach to. Caught by re-tracing exactly when each id in that call chain is
  actually assigned, not by any test (there's no compiler or instrumented test to catch a
  same-composable-instance stale-arg bug like this). Fixed by changing
  `onOpenPayslip`/`onNewPayslip`'s signatures to take the employee id as a parameter supplied
  by `EmployeeEditScreen` itself at click-time (from its own live `uiState.employeeId`, which
  *is* correctly updated after save) rather than one closed over from the nav graph.
  `PayslipEditViewModel` also needed this: employee id is a required nav argument there, not
  optional, so this bug would otherwise have surfaced as `checkNotNull` throwing on a literal
  `"_"` string rather than a clean crash — worth naming since it's the kind of thing that would
  have been very confusing to debug from a stack trace alone.
- `SyncStatusRepository`'s three `when (item) { ... }` blocks (retry/keepMine/useTheirs) were
  extended with `EmployeeDao`/`PayslipDao`/`EmployeeRepository`/`PayslipRepository` constructor
  parameters and the two new `observeUnsynced()` flows in one pass this time — a direct
  consequence of the identical gap being caught (and having to be fixed as a follow-up edit)
  during the Suppliers milestone; re-reading the file confirmed all three blocks are exhaustive
  over the now-twelve-subclass `SyncStatusItem` sealed class.

From the Compliance milestone: no new functional defect turned up in this pass — a direct
result of applying the two lessons above proactively rather than reactively.
`SyncStatusRepository`'s constructor params, `observeUnsynced()` flow, and all three
`when (item) { ... }` branches for the new `ComplianceItem` subclass were written together in
one edit, then independently re-read against the (now thirteen-subclass) sealed class to
confirm exhaustiveness before moving on, rather than being caught as a follow-up fix. One
purely cosmetic slip was caught and fixed while writing, before it was ever "finished" code:
`ComplianceListScreen.kt`'s list content originally named its collected state `items` — the
exact same identifier as the imported `LazyListScope.items(...)` DSL function it's passed
into. `items(items, key = { it.id }) { ... }` is unambiguous and does compile (a `List` has no
`invoke` operator, so Kotlin's overload resolution can't confuse the two), but it reads as a
trap for a future reader and doesn't match this codebase's own convention of naming every
collected list semantically (`employees`, `suppliers`, `expenses`, never the generic `items`).
Renamed to `complianceItems` throughout the file.

From the Reports milestone: no functional defect found on manual re-read — every field
`ReportsViewModel` reads off `PaymentEntity`/`ExpenseEntity`/`InvoiceEntity` (`paidDate`,
`amount`, `date`, `category`, `vatAmount`, `issueDate`, `status`) and every shared component it
calls (`formatZar`, `labelFor`, `EmptyState`, `SectionHeader`, `EXPENSE_CATEGORY_CHOICES`,
`InvoiceStatus.DRAFT`/`.CANCELLED`) was cross-checked field-by-field, param-by-param against
their actual declarations, all matching. One proactive fix, caught while writing rather than as
a follow-up — the same category of mistake the Suppliers/Employees milestones' `when`-block
gaps were, just for a different Compose hazard: `ReportsScreen.kt`'s monthly-profit
`LazyColumn.items(...)` call initially used `key = { it.month }`, a raw `java.time.YearMonth`.
Compose requires list keys to be Bundle-saveable for state restoration across process death, and
`YearMonth` is not `Parcelable` — this would risk a runtime crash the moment Android needed to
restore this screen's scroll/recomposition state, with nothing catching it at compile time.
Fixed to `key = { it.month.toString() }` before it was ever "finished" code, matching the
String-keyed convention every other list screen in this app already uses. Also confirmed on
this pass (backend, not Android, but caught the same way — by re-reading and verifying rather
than trusting the first draft): `TruncMonth` on a Django `DateField` returns a native
`datetime.date`, not `datetime.datetime` — an initial `.date()` call on that result would have
raised `AttributeError` at request time; caught by a live Django shell check before it ever hit
a test. And the CSV-export query parameter was originally `?format=csv`, which DRF silently
intercepts for its own content-negotiation and 404s before the view's `get()` runs (no CSV
renderer is registered) — caught by an actual failing test (`test_csv_export`, `404 != 200`),
not by inspection; renamed to `?export=csv`, documented in both the view's docstring and
API_CONTRACT.md so nobody hits the same trap again.

All of the above are exactly the kind of thing a real `compileDebugKotlin` (or a runtime smoke
test) would catch immediately, which is why this section says "written, not verified" rather
than "done" — the same class of mistake could plausibly still be sitting somewhere in these 133
files this sandbox couldn't compile. **Confirming the `app` module actually builds and runs
needs Android Studio or CI with a real Android SDK** — that hasn't happened yet.

## Scope notes / deliberate simplifications

- **No PDF rendering.** Quote/invoice "preview" is a native Compose layout that mirrors the
  backend's letterhead (business logo/name/address, line items, VAT, totals) rather than a
  WebView loading the backend's actual HTML render. DISCOVERY.md's architecture section
  describes a shared HTML render as the long-term plan; reproducing that exactly was judged
  out of scope for this slice versus a native equivalent that shows the same information.
  "Send" shares a plain-text summary via `Intent.ACTION_SEND`, not a generated PDF file.
- **Customer-picker reuses the Customer list screen** (`OpsDestinations.CUSTOMERS_PICKABLE`,
  a `pickMode` route parameter) rather than adding a fourth new screen, for the Home
  quick-add actions (New quote / New invoice / Record payment) that need a customer chosen
  first but aren't listed as their own screen in DISCOVERY.md section 10.
- **Business Setup gained a "Sign in" mode** alongside "Create business". The screen list
  only names one first-run screen, but `POST /api/auth/login/` existing in the contract, and
  the demo script requiring it to log into the *already-seeded* Thabo's Plumbing account
  rather than registering a second, empty one, both make this a necessary addition, not
  scope creep — it is a second state of the same one screen, not a new one.
- **Compliance reminders and full Reports are not built** — DISCOVERY.md section 10
  explicitly scopes these to later milestones; no screens or navigation for them exist.
- **Suppliers is a contact record, not a vendor-management module.** Name, contact person,
  phone, email, notes — that's the whole model, matching how a real small-business owner
  actually tracks "who I buy from" (a phone number to call, not a procurement workflow). "What
  have I bought from them" is answered by filtering that supplier's own `Expense` rows (shown
  read-only on the supplier's own screen), not a separate purchase-order/ledger concept.
- **Employees & Payslips is deliberately not a workforce-management system.** No shift or
  hours tracking, no leave management, no org chart — `Employee` is a staff contact plus the
  agreed pay rate (shown back as a reminder, never used to auto-compute anything), and
  `Payslip` is one pay period's gross pay, deductions, and derived net pay. This app makes no
  claim of PAYE/UIF payroll-tax accuracy or e-filing — `deductions` is a plain number the owner
  types in (from their bookkeeper, or whatever they know to withhold), not a computed tax-table
  result. `EmployeeListScreen` is reached from Business Profile/Settings, not its own bottom-nav
  tab or nested under Money — staff management reads as an administrative task, not a daily
  transactional one, for a real SA trade business owner.
- **Payslip "sharing" is a plain-text summary**, same `Intent.ACTION_SEND` pattern as
  quote/invoice "Send" — no generated PDF, consistent with the "No PDF rendering" note above.
- **Compliance has no recurrence engine, anywhere.** `ComplianceItem` has no relations to any
  other model and no server-side scheduling logic at all — the app never creates a reminder on
  its own. Marking a recurring item done shows an "Add the next reminder?" dialog, pre-filled
  at a category-typical interval computed client-side (`ComplianceEditViewModel.suggestedNextItem`
  — VAT return +2 months, PAYE/UIF/SDL +1 month, provisional tax +6 months, CIPC annual return
  +1 year; "Other" gets no suggestion, since there's no sensible default), but the owner must
  tap "Add" for anything to actually be created; declining does nothing. Accountant-ready
  exports (mentioned in the original brief alongside compliance reminders) are Reports-milestone
  territory and not built here. `ComplianceListScreen`, like `EmployeeListScreen`, is reached
  from Business Profile/Settings, not a bottom-nav tab — and both the list and edit screens
  carry an explicit, permanent on-screen line that OPS tracks deadlines but files nothing with
  SARS or CIPC, per DISCOVERY.md's compliance-honesty note.
- **One Expense screen, not three.** The original screen list sketched list/new-edit/detail as
  separate screens; built instead as one `ExpenseEditScreen` (create, edit, view, delete, and
  receipt capture all in place) plus the list folded into the Money tab's existing feed
  pattern — matching how Job detail already works, and avoiding three near-identical screens
  for what's a flat, single-record form with no line items or preview/send step.
- **No live sync-status refresh while the Expense screen is open.** Its form fields are local
  mutable state (same reason `InvoiceEditViewModel` doesn't live-observe Room — avoiding
  Compose recomposition fighting the owner mid-type), so a receipt upload's
  PENDING→UPLOADING→UPLOADED progression, or the record's own SYNCING→SYNCED, only refreshes
  when the screen is re-opened. The top-bar sync chip still updates live regardless.
- **Receipt photos are a second, separate sync phase**, not part of the JSON `changes` batch —
  see API_CONTRACT.md's "Expense receipt attachments" and `SyncManager.syncReceipts`. A photo
  is written to permanent app-private storage (`filesDir/receipts/`) the moment it's
  captured/picked — before any network involvement — so it survives being attached fully
  offline; the temp camera-capture file (`cacheDir/receipts_tmp/`, exposed to the Camera app
  via a `FileProvider`) is a separate, disposable intermediate step.
- **Reports has no stored model of its own and no sync footprint at all.** Every figure is
  computed on demand — server-side by three `rest_framework.views.APIView`s over existing
  `Payment`/`Expense`/`Invoice` rows, client-side by `ReportsViewModel` over the same rows
  already synced into Room — so there is nothing to register in `sync/registry.py`, no new Room
  entity, and no `SyncStatusItem` subtype. Opening the Reports tab makes zero network calls.
- **Revenue is cash-basis (payments received), not accrued/invoiced amounts** — the same
  definition `HomeViewModel.moneyInThisMonth` already uses for its "Money in" stat card, kept
  identical here on purpose so the app has one financial vocabulary throughout, not two
  different numbers both plausibly called "revenue." This is explicitly not a general ledger,
  trial balance, or chart of accounts — see DISCOVERY.md's "Explicitly NOT in V1" list.
- **No CSV export wired into the Android UI.** The backend's `GET
  /api/reports/profit-summary/?export=csv` exists and is tested, for a future "email this to my
  bookkeeper" action, but no button calls it yet — out of scope for a first Reports slice on a
  phone screen, where the on-screen monthly table already answers the question.
- **VAT summary is informational only.** The Reports tab shows VAT collected vs paid for the
  owner's own SARS VAT201 prep with their bookkeeper — same honesty stance as the Compliance
  milestone's on-screen reminder — OPS does not calculate a filing figure or submit anything.
- **Reports is the one new module that earned a bottom-nav tab of its own**, unlike Suppliers/
  Employees/Compliance (each one tap deeper, from Money or Business Profile). Checking "how's
  the business doing" is a recurring check-in for a real owner, not an occasional administrative
  task — see DISCOVERY.md section 5 for the full placement rationale.
