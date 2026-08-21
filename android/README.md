# OPS — Android

Mobile-first, offline-first Android client for OPS, a South African small-business operating
system. See `../docs/DISCOVERY.md` for the product/architecture rationale and
`../docs/API_CONTRACT.md` for the exact network contract this app implements against. This
module implements the V1 vertical slice — business setup, leads, customers, quotes, jobs,
invoices, payments, the home dashboard, and offline sync — plus the Expenses milestone
(capture, receipt photo attachment, VAT-inclusive extraction, categories, optional job/project
link) and the Suppliers milestone that followed it (a simple contact record — who the business
buys from — linked from Expense.supplier_id, with a picker on the expense form and each
supplier's own expense history shown read-only on its own screen). See DISCOVERY.md section 10
for what's still deliberately deferred (Employees/Payslips, Compliance, full Reports).

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
  contact person, phone, email, notes — not a vendor-management module. Schema history: `v2`
  added `ExpenseEntity`; `v3` added `SupplierEntity` and `ExpenseEntity.supplierId`. None of
  these has a migration path from the version before it — `fallbackToDestructiveMigration()` —
  since this app has never shipped; that stops being acceptable once it does.
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
  Job, Invoice+line items, Payment, Supplier, Expense, Business, Auth) plus
  `SyncStatusRepository` for the sync status screen's cross-model view.
  `ExpenseRepository.attachReceipt`/`retryReceipt` manage the receipt state machine;
  `save`/`delete` are the usual PENDING-then-sync pattern, same for `SupplierRepository`.
- `di/` — Hilt modules for Room, Retrofit/OkHttp, WorkManager. `AuthPreferences`
  (DataStore-backed) and every repository are constructor-injected directly (`@Inject
  constructor`), which is itself Hilt DI — no separate binding module is needed for concrete
  classes with no interface to bind against.
- `ui/` — one package per screen area (`splash`, `businesssetup`, `home`, `leads`,
  `customers`, `quotes`, `jobs`, `invoices`, `payments`, `expenses`, `suppliers`, `money`,
  `syncstatus`, `settings`), each with a `@HiltViewModel` + a Compose screen, plus
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
  app section.

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
BUILD SUCCESSFUL in 21s
4 actionable tasks: 1 executed, 3 up-to-date
```

36 tests, 36 passing, 0 failures, 0 errors — confirmed via both the console output and the
JUnit XML result files (`core-domain/build/test-results/test/*.xml`), broken down as:

| Test class                     | Tests | Covers |
|---------------------------------|:---:|---|
| `MoneyTest`                     |  9  | Line total rounding, VAT with/without, discount before VAT, discount > subtotal never negative, empty line items, half-up vs half-even, fractional quantities, the flat 15% rate itself |
| `VatInclusiveExtractionTest`    |  5  | Clean multiples of 115 extract exactly, unclean divisions round half-up, not-VAT-applicable and zero-amount both extract R0.00 — mirrors `backend/tests/test_money.py`'s `VatInclusiveExtractionTests` case-for-case |
| `SyncDecisionTest`              |  5  | No existing row, incoming strictly newer, existing newer (conflict), equal timestamps (conflict — this is also what makes a replayed push idempotent), sub-second precision |
| `IsoTimestampTest`              |  7  | `Z` suffix never `+00:00` (and never a raw `+` at all), zero-microsecond formatting, round-trip through format+parse, nanosecond truncation, the contract's own example value, no-fraction parsing, defensive offset-form parsing |
| `EnumsTest`                     | 10  | Every enum's (incl. `ExpenseCategory`, 14 values) wire values match the Django `choices` list byte-for-byte, `fromWire` round-trips every value, `fromWire` rejects an unknown value |

This is the one hard verification gate for this deliverable, and it's genuinely green — not
asserted, run. The Suppliers milestone added no `core-domain` code (no new enum, no new money
direction — a supplier is a plain contact record), so this table is unchanged from the Expenses
milestone; re-run above to confirm nothing regressed.

**Written but NOT compiled or run here — the `app` module:**

Every file under `app/src/main/kotlin` (112 Kotlin files as of the Suppliers milestone —
Room entities/DAOs, Retrofit service/DTOs, the sync engine, ten repositories, Hilt modules,
and 18 Compose screens with their ViewModels) was written carefully, by hand, cross-checking
every field name, wire enum value, and endpoint path against `API_CONTRACT.md` and the actual
Django serializers/models in `../backend/` — but **`app:compileDebugKotlin` was never
successfully run**, because it cannot succeed here: AGP needs `android.jar` from an installed
SDK to compile against, and this sandbox has neither the SDK nor network access to
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

All of the above are exactly the kind of thing a real `compileDebugKotlin` (or a runtime smoke
test) would catch immediately, which is why this section says "written, not verified" rather
than "done" — the same class of mistake could plausibly still be sitting somewhere in these 112
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
- **Employees/Payslips, Compliance reminders, and full Reports are not built** —
  DISCOVERY.md section 10 explicitly scopes these to later milestones; no screens or
  navigation for them exist.
- **Suppliers is a contact record, not a vendor-management module.** Name, contact person,
  phone, email, notes — that's the whole model, matching how a real small-business owner
  actually tracks "who I buy from" (a phone number to call, not a procurement workflow). "What
  have I bought from them" is answered by filtering that supplier's own `Expense` rows (shown
  read-only on the supplier's own screen), not a separate purchase-order/ledger concept.
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
