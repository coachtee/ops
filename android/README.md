# OPS — Android

Mobile-first, offline-first Android client for OPS, a South African small-business operating
system. See `../docs/DISCOVERY.md` for the product/architecture rationale and
`../docs/API_CONTRACT.md` for the exact network contract this app implements against. This
module implements the V1 vertical slice: business setup, leads, customers, quotes, jobs,
invoices, payments, the home dashboard, and offline sync — nothing more (see DISCOVERY.md
section 10 for what's deliberately deferred to later milestones).

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
  before VAT, taxable amount never negative, VAT is 0.00 when not applicable.
- `SyncDecision.kt` — `decideSyncOutcome(existingUpdatedAt, incomingUpdatedAt)`, the same
  last-write-wins comparison `backend/sync/services.py` makes server-side. The Android sync
  client (`app`'s `SyncManager`) calls this on every pulled row before letting it overwrite a
  clean local record.
- `IsoTimestamp.kt` — formats/parses the wire's `...Z`-suffixed UTC timestamps, deliberately
  never `+00:00` (an un-encoded `+` in a URL query string decodes as a space under
  form-encoding, corrupting the sync `since` cursor — see API_CONTRACT.md's opening section).
- `Enums.kt` — `LeadSource`, `LeadStatus`, `QuoteStatus`, `JobStatus`, `InvoiceStatus`,
  `PaymentMethod`, `CustomerType`, each carrying the exact wire string from the matching
  Django model's `choices`, checked against `backend/{crm,sales,work,finance}/models.py`.

### `app` (Android application, `com.ops.app`, Jetpack Compose, Material 3)

- `data/local/` — Room entities/DAOs/`OpsDatabase`. Every money field (quantity, unit_price,
  line_total, subtotal, vat_amount, total, discount_amount, amount_paid, amount) is a TEXT
  column holding the canonical decimal string — never REAL/float.
- `data/remote/` — Retrofit `OpsApiService` (every endpoint in API_CONTRACT.md),
  kotlinx.serialization DTOs, `AuthHeaderInterceptor` + `TokenAuthenticator` (401 → refresh
  once → retry).
- `data/sync/` — `SyncManager`, the offline-sync engine's client half (push → mark
  accepted/conflict/error → pull → upsert-if-safe → persist cursor), plus `SyncWorker`
  (WorkManager, ~15 min periodic heartbeat + expedited one-time trigger after local writes).
- `data/repository/` — one repository per aggregate (Lead, Customer, Quote+line items,
  Job, Invoice+line items, Payment, Business, Auth) plus `SyncStatusRepository` for the sync
  status screen's cross-model view.
- `di/` — Hilt modules for Room, Retrofit/OkHttp, WorkManager. `AuthPreferences`
  (DataStore-backed) and every repository are constructor-injected directly (`@Inject
  constructor`), which is itself Hilt DI — no separate binding module is needed for concrete
  classes with no interface to bind against.
- `ui/` — one package per screen area (`splash`, `businesssetup`, `home`, `leads`,
  `customers`, `quotes`, `jobs`, `invoices`, `payments`, `money`, `syncstatus`, `settings`),
  each with a `@HiltViewModel` + a Compose screen, plus `ui/navigation/OpsNavGraph.kt` wiring
  all of them together and `ui/components/` for shared pieces (money/date formatting, the
  sync status chip, the branded quote/invoice letterhead, a date picker field, dropdowns).

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
   jobs, invoices and payments down through the real `GET /api/sync/pull/` path — the same path
   any other data uses. Pull-to-refresh on Home forces this immediately rather than waiting.

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

```
$ ./gradlew :core-domain:test
...
BUILD SUCCESSFUL in 12s
4 actionable tasks: 1 executed, 3 up-to-date
```

30 tests, 30 passing, 0 failures, 0 errors — confirmed via both the console output and the
JUnit XML result files (`core-domain/build/test-results/test/*.xml`), broken down as:

| Test class            | Tests | Covers |
|------------------------|:---:|---|
| `MoneyTest`            |  9  | Line total rounding, VAT with/without, discount before VAT, discount > subtotal never negative, empty line items, half-up vs half-even, fractional quantities, the flat 15% rate itself |
| `SyncDecisionTest`     |  5  | No existing row, incoming strictly newer, existing newer (conflict), equal timestamps (conflict — this is also what makes a replayed push idempotent), sub-second precision |
| `IsoTimestampTest`     |  7  | `Z` suffix never `+00:00` (and never a raw `+` at all), zero-microsecond formatting, round-trip through format+parse, nanosecond truncation, the contract's own example value, no-fraction parsing, defensive offset-form parsing |
| `EnumsTest`            |  9  | Every enum's wire values match the Django `choices` list byte-for-byte, `fromWire` round-trips every value, `fromWire` rejects an unknown value |

This is the one hard verification gate for this deliverable, and it's genuinely green — not
asserted, run.

**Written but NOT compiled or run here — the `app` module:**

Every file under `app/src/main/kotlin` (99 Kotlin files: Room entities/DAOs, Retrofit
service/DTOs, the sync engine, seven repositories, Hilt modules, and 15 Compose screens with
their ViewModels) was written carefully, by hand, cross-checking every field name, wire enum
value, and endpoint path against `API_CONTRACT.md` and the actual Django serializers/models in
`../backend/` — but **`./gradlew :app:compileDebugKotlin` was never run**, because it cannot
succeed here: AGP needs `android.jar` from an installed SDK to compile against, and this sandbox
has neither the SDK nor network access to `dl.google.com` to fetch AGP's own plugin artifact in
the first place (confirmed by testing, see above) — the build fails at plugin resolution,
before it would even get to the point of missing the SDK. That is a sandbox limitation, not
something wrong with the `app` module's own build files, which are otherwise a normal,
standalone AGP/Compose/Hilt setup.

Two specific things were caught and fixed exactly because I went back and manually re-read the
code for compile-plausibility (not because a compiler caught them) — worth naming so it's clear
what "not compiled" actually risks:

- `RoomDatabase.clearAllTables()` (used on logout) asserts it isn't called on the main thread;
  the original `AuthRepository.logout()` called it directly from a `suspend fun` without
  dispatching to `Dispatchers.IO`, which would have crashed on first logout. Fixed.
- The picked-logo-image preview (Business Setup and Business Profile) originally tried to hand
  Coil's `AsyncImage` a raw `ByteArray` model, which Coil 2.x's default component registry
  doesn't know how to fetch. Fixed to decode via `BitmapFactory` and render with a plain
  Compose `Image` for the not-yet-uploaded preview, reserving Coil for the String-URL case
  (an already-uploaded logo).

Both are the kind of bug a real `compileDebugKotlin` (or a runtime smoke test) would have
caught immediately, which is exactly why this section says "written, not verified" rather than
"done" — the same class of mistake could plausibly still be sitting somewhere in the 99 files
this sandbox couldn't compile. **Confirming the `app` module actually builds and runs needs
Android Studio or CI with a real Android SDK** — that hasn't happened yet.

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
- **Expenses, Suppliers, Employees/Payslips, Compliance reminders, and full Reports are not
  built** — DISCOVERY.md section 10 explicitly scopes these to later milestones, and the task
  brief repeats that instruction; no screens or navigation for them exist in this slice.
