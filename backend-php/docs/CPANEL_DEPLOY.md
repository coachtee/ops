# Deploying to cPanel (File Manager + phpMyAdmin only, no Terminal)

This guide assumes exactly what most shared cPanel hosting gives you: **File Manager**,
**phpMyAdmin**, and the standard cPanel dashboard tools — no SSH, no Terminal, no way to run
`composer` or `php index.php migrate` on the server itself. Everything below is designed around
that.

You'll need two things from me before you start, both already prepared:

- **`ops-cpanel-backend.zip`** — the whole app, including `vendor/` already built (since there's
  no Composer on the host to build it for you).
- **`CPANEL_SCHEMA.sql`** — the full database schema, ready to import via phpMyAdmin (since
  there's no shell to run the migration command).

## 1. Pick where it lives

You said you don't have a domain/subdomain picked yet — you'll need one before the Android app
or anything else can reach this over the internet, and it **must support HTTPS** (a free
Let's Encrypt/AutoSSL certificate, which cPanel usually provisions automatically once DNS is
pointed correctly — look for "SSL/TLS Status" or "AutoSSL" in cPanel).

Recommended: a **subdomain** dedicated to the API, e.g. `api.yourdomain.com`, kept separate
from anything else on the main domain.

1. In cPanel, open **Domains** → **Subdomains** (or **Domains** → **Create A New Domain**,
   depending on your cPanel theme).
2. Create `api` as the subdomain of your domain. cPanel will ask for a document root — accept
   the default it suggests (usually `public_html/api`) or pick your own; just remember it, you'll
   upload files there in step 3.
3. Wait for DNS/SSL to catch up (usually a few minutes; can be up to 24 hours on a brand new
   domain). You can proceed with the steps below in the meantime.

## 2. Create the database

1. In cPanel, open **Databases** → **MySQL Databases**.
2. Create a new database — call it something like `yourcpaneluser_ops`. cPanel prefixes
   database/user names with your cPanel username automatically; note the **full** prefixed name.
3. Under "MySQL Users", create a new user with a strong password. **Save this password
   somewhere** — you'll need it in step 4.
4. Under "Add User to Database", add that user to the database you just created, and grant
   **ALL PRIVILEGES**.
5. Note down three things: the **full database name**, the **full username**, and the
   **password**. The database host is almost always `localhost` on shared cPanel hosting.

## 3. Import the schema

1. In cPanel, open **Databases** → **phpMyAdmin**.
2. Select your new database in the left sidebar (it should be empty).
3. Click the **Import** tab.
4. Choose the `CPANEL_SCHEMA.sql` file, leave the format as SQL, and click **Go**.
5. You should see 19 tables appear in the sidebar (`businesses`, `customers`, `quotes`,
   `invoices`, … down to `migrations`). If it fails partway through, the database wasn't empty —
   drop all tables and re-import into a genuinely fresh database.

## 4. Upload the application

1. In cPanel, open **Files** → **File Manager**, and navigate to the document root you set up
   in step 1 (e.g. `public_html/api`).
2. Click **Upload**, select `ops-cpanel-backend.zip`, and wait for it to finish.
3. Back in File Manager (not the upload dialog), right-click the uploaded zip and choose
   **Extract**. This unpacks everything (`index.php`, `.htaccess`, `application/`, `system/`,
   `vendor/`, `assets/`, `uploads/`) directly into that folder.
4. Delete the zip file itself once extraction succeeds (no need to keep it there).
5. Confirm `application/`, `system/`, `vendor/`, `index.php`, and `.htaccess` are all now
   sitting directly inside your document root (not nested one level deeper inside an extra
   folder — if `unzip` created a subfolder, move everything up one level and delete the empty
   folder).

## 5. Configure it — edit `.htaccess`

This is the one file you edit by hand. Open `.htaccess` in File Manager's code editor (right-click
→ Edit) and add these lines at the very top, **before** the existing `<IfModule mod_rewrite.c>`
block — don't remove or change anything that's already there:

```apache
SetEnv CI_ENV production
SetEnv OPS_BASE_URL https://api.yourdomain.com
SetEnv OPS_DB_HOST localhost
SetEnv OPS_DB_NAME yourcpaneluser_ops
SetEnv OPS_DB_USER yourcpaneluser_opsuser
SetEnv OPS_DB_PASSWORD the-database-password-from-step-2
SetEnv OPS_SECRET_KEY 83359075c9dd10e93dd8525362d010ebc2867b3f42d3df92c4d982877b65835b
```

Replace every value on the right with your own — **especially** `OPS_BASE_URL` (your actual
subdomain, `https://`, no trailing slash) and the three `OPS_DB_*` values (from step 2).

The `OPS_SECRET_KEY` value above is a real, randomly-generated 64-character key — safe to use
as-is, or generate your own 64-hex-character replacement if you'd rather (any long random
string works; it just signs the login tokens the Android app uses, so it needs to be unguessable
and must never be shared or reused elsewhere). **Do not skip this** — the app will show a clear
`FATAL: OPS_SECRET_KEY is not set` error page instead of running at all if you forget it, on
purpose (see `application/config/config.php`), rather than silently running insecurely.

Save the file.

## 6. Verify it's alive

Visit `https://api.yourdomain.com/api/health/` in a browser (adjust to your actual subdomain).
You should see exactly:

```json
{"status":"ok","service":"ops-api","database":"ok"}
```

- **A 500 error page saying `FATAL: OPS_SECRET_KEY is not set`** → the `SetEnv` block didn't
  save, or you're editing the wrong `.htaccess` (make sure you edited the one in the same folder
  as `index.php`, not a different one).
- **`"database": "error: ..."`** → one of the `OPS_DB_*` values is wrong; double-check them
  against what you created in step 2 (host is almost always `localhost`).
- **A raw PHP error or blank page** → check your hosting's PHP version is 8.1 or newer (cPanel's
  **MultiPHP Manager** tool lets you pick the PHP version per domain).
- **404 on everything** → `mod_rewrite` isn't enabled for your account, or `.htaccess` overrides
  are disabled — contact your host; this is rare on cPanel but not unheard of on some
  budget plans.

## 7. Point the Android app at it, and create your first business

Install the debug APK (see the main README/whatever channel you got it through), then:

1. Open the app → go to **Business Profile** → **Developer options** → **Connection
   Diagnostics**.
2. Enter `https://api.yourdomain.com` as the server URL override and save.
3. Run "Test connection" (hits `/api/health/`) and "Test authentication" — the second one will
   fail until you've registered a business, which is expected.
4. Go through the app's own sign-up/business setup flow (the same first-run screen it shows a
   brand new install) — this calls `POST /api/auth/register/` and creates your business, owner
   user, and login in one step. There's no separate web-based sign-up; this is the one place a
   business gets created.
5. Once registered, log in at `https://api.yourdomain.com/login` with that same email/password
   to see the web admin panel (dashboard, customers, leads, quotes, jobs, invoices).

## What you get vs. what's still missing

- The API (everything the Android app uses: auth, sync, receipt/photo uploads) and the read-only
  web admin panel both work identically to how they were verified in this sandbox — same code,
  same tests, no cPanel-specific shortcuts.
- **No automated backups are configured by this deployment** — set up cPanel's own database/file
  backup schedule (**Backup** in cPanel) yourself; this guide doesn't do that for you.
- **No email is configured** — nothing in this app currently sends email (no password-reset
  flow exists yet), so nothing to set up here, but worth knowing it's not there.
- If you ever get Terminal/SSH access later (an upgrade, or a different host), you can switch to
  running `php index.php migrate` for future schema changes instead of hand-applying new SQL —
  the `migrations` table is already correctly marked up to date by `CPANEL_SCHEMA.sql`'s last
  line, so it'll pick up cleanly from there.
