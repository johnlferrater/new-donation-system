# Deployment Guide

This repository is a Spring Boot monolith. Pages are server-rendered with Thymeleaf (`src/main/resources/templates`) and static
assets are served from `src/main/resources/static` (`css/`, `js/`). The backend serves both the HTML pages and the REST APIs.
Netlify cannot run Thymeleaf, so the recommended setup is:

- Railway: Spring Boot backend + UI routes + MySQL
- Netlify: public front door. It serves the static CSS/JS files and proxies page/API routes to Railway, preserving
  the existing same-origin session/CSRF behavior without restructuring the app.

If you later convert the UI into a fully separate SPA, the backend already supports credentialed CORS for an exact
Netlify origin, but the login/registration/logout flows would still need to be rewritten as JavaScript fetches.

## 1. Railway setup (backend + MySQL)

1. Push the repo to GitHub.
2. In Railway, create a new project and add the GitHub repository as a service.
3. Add a MySQL service to the same project.
4. On the Spring Boot service, use the commands from `railway.toml` (also in `Procfile`):
   - Build: `./mvnw clean package -DskipTests`
   - Start: `java -jar target/new-donation-system-1.0.0.jar`
5. Railway provides `PORT`; the app already reads `server.port=${PORT:8080}`.
6. Generate a public domain for the Spring Boot service.
7. In service Settings, make sure private networking is enabled if you want the app and MySQL to communicate privately.

## 2. Railway MySQL setup

1. In the Railway project, add a MySQL service.
2. Keep the service in the same project/region as the app so `DB_HOST` can use the MySQL private host.
3. The app uses `spring.jpa.hibernate.ddl-auto=update`, so on an empty database Hibernate creates the schema on first
   startup. Do not delete or overwrite existing data during migration.

## 3. Environment variables

Set these on the Railway Spring Boot service. Names match `src/main/resources/application.properties` and `.env.example`.

| Variable | Value | Notes |
| --- | --- | --- |
| `DB_HOST` | MySQL private host | e.g. the MySQL service hostname from Railway |
| `DB_PORT` | `3306` | |
| `DB_NAME` | `donation_system` | Must match the MySQL database name |
| `DB_USERNAME` | MySQL user | Do not hardcode in Git |
| `DB_PASSWORD` | MySQL password | |
| `DB_USE_SSL` | `false` | Railway private networking normally does not need SSL |
| `SESSION_COOKIE_SECURE` | `true` | HTTPS in production |
| `SESSION_COOKIE_SAME_SITE` | `Lax` | Recommended proxy mode |
| `CSRF_COOKIE_SAME_SITE` | `Lax` | Recommended proxy mode |
| `CORS_ALLOWED_ORIGINS` | `https://your-site.netlify.app` | Exact Netlify origin; no wildcard with credentials |
| `APP_BOOTSTRAP_ADMIN_USERNAME` | admin username | Only if the DB is empty |
| `APP_BOOTSTRAP_ADMIN_EMAIL` | admin email | Only if the DB is empty |
| `APP_BOOTSTRAP_ADMIN_PASSWORD` | 12+ chars | Only if the DB is empty |
| `ADMIN_SIGNUP_CODE` | optional | Leave blank to disable admin self-registration |

Optional split-frontend/CORS mode (only if you later separate the UI):
`SESSION_COOKIE_SAME_SITE=None`, `CSRF_COOKIE_SAME_SITE=None`, and keep `SESSION_COOKIE_SECURE=true`. This requires
`SameSite=None; Secure` cookies for cross-site requests.

## 4. Backend build and start commands

Already configured in `railway.toml`:

```toml
[build]
builder = "NIXPACKS"
buildCommand = "./mvnw clean package -DskipTests"

[deploy]
startCommand = "java -jar target/new-donation-system-1.0.0.jar"
```

Verified JAR filename: `target/new-donation-system-1.0.0.jar` (from `pom.xml`: `com.donation:new-donation-system:1.0.0`).

## 5. Local MySQL migration to Railway

Do not overwrite the Railway database blindly. Use a dump/import so Hibernate's `update` mode only reconciles schema.

1. Stop the local app to avoid writes during the dump.
2. Export local data (adjust user/password/host as needed):

   ```bash
   mysqldump -h localhost -u donation_app -p donation_system > donation_system.sql
   ```

3. In Railway, open the MySQL service and copy its host/user/password into a MySQL client
   (Railway CLI, MySQL Workbench, or `mysql` with the public proxy if private networking is not available).
4. Import the dump into the Railway database:

   ```bash
   mysql -h <railway-host> -P <railway-port> -u <railway-user> -p <railway-db> < donation_system.sql
   ```

5. Deploy the Spring Boot service. Hibernate `update` will add/adjust columns if needed.
6. Verify the row counts match between local and Railway before deleting local data.

## 6. Netlify setup (frontend/front door)

1. Push the repo to GitHub.
2. In Netlify, deploy from the same repository.
3. Use `netlify.toml`:
   - Build command: `echo 'Static frontend assets are served directly; dynamic pages and APIs are proxied to Railway'`
   - Publish directory: `src/main/resources/static`
4. After Railway gives you a public URL, replace every `YOUR-RAILWAY-BACKEND` in `netlify.toml` with the real
   Railway host, for example `https://api.example.up.railway.app`. Alternatively, add equivalent redirects in the Netlify UI.
5. `netlify.toml` already includes redirects for:
   - `/api/*`
   - `/login`, `/register`, `/logout`
   - `/dashboard`, `/donation-records`, `/donation-history`, `/goals`, `/announcements`, `/profile`, `/users`, `/admins`
   - catch-all `/*` to the backend (existing static files are still served by Netlify)
6. Deploy the Netlify site. There is no frontend build step; CSS/JS are plain files.

## 7. CORS configuration

Backend CORS is enabled in `SecurityConfig` and reads `CORS_ALLOWED_ORIGINS`.

- Exact origins only; no `*` with credentials.
- Allowed methods: `GET, POST, PUT, DELETE, OPTIONS`
- Allowed headers: `Accept, Authorization, Content-Type, Origin, X-Requested-With, X-XSRF-TOKEN`
- `AllowCredentials: true`

In the recommended Netlify proxy mode, browser requests are same-origin to Netlify, so CORS is not strictly required.
CORS is still useful if you access the Railway backend directly or later split the frontend.

## 8. Post-deployment testing

Test on the Netlify URL first, then the Railway URL:

- `/` loads.
- `/login` loads; submit regular and admin portal logins.
- Registration works, then login with the new account.
- `/dashboard`, `/donation-records`, `/donation-history`, `/goals`, `/announcements`, `/profile` load.
- Admin can open `/users` and use `/api/users`.
- Create/update/delete a donation record and history entry.
- Create/update/delete a goal and announcement as admin.
- Notifications bell loads and marks items read.
- Upload a JPEG/PNG/WebP profile picture (max 5 MB).
- Logout invalidates the session and returns to `/login?logout`.
- Anonymous `/api/notifications` returns JSON `401`; anonymous `/dashboard` redirects to `/login`.
- Confirm the app uses HTTPS and session cookies are marked `Secure`.

## 9. Security notes

- Do not commit real database passwords, API keys, or dumps. The local `backup-*.sql` files contain user data; keep them
  out of Git.
- Profile pictures are stored as BLOBs in MySQL, not on the Railway filesystem. The upload endpoint accepts only
  JPEG/PNG/WebP (content-type sniffed from magic bytes), max 5 MB, authenticated only, and serves with `nosniff`.
- Session and CSRF protection remain enabled. Keep `SESSION_COOKIE_SECURE=true` in production.
- The app validates `APP_BOOTSTRAP_ADMIN_*` only when the database is empty; remove those variables after the first admin exists.

## 10. Verification done before these instructions

- `.\mvnw.cmd clean test` → `Tests run: 104, Failures: 0, Errors: 0, Skipped: 0`
- `.\mvnw.cmd clean package` → `BUILD SUCCESS`
- Build output: `target/new-donation-system-1.0.0.jar`
