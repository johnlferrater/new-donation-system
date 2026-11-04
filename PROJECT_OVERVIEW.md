# Donation System - Project Overview

## Description

A web-based donation management system built with Spring Boot. It allows users to track donations, manage goals, record donor information, and post announcements. This document is based on the current source code in `src/main/java` and `src/main/resources`.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2.5 |
| Security | Spring Security 6 (form login, BCrypt, roles, CSRF, optional in-memory tab tokens) |
| Database | MySQL 8 (`donation_system`); H2 in-memory only for tests |
| Frontend | Thymeleaf, Bootstrap 5, Thymeleaf Spring Security extras |
| Rate limiting | Bucket4j 8.10.1 |
| Build | Maven (wrapper included) |

## Project Structure

```
new-donation-system/
├── src/main/java/project2/
│   ├── Project2Application.java
│   ├── config/
│   │   ├── DataSeeder.java                 (optional environment-configured first admin)
│   │   ├── DatabaseCredentialValidator.java
│   │   ├── ProfilePictureMigration.java    (migrates legacy file-based avatars to the DB)
│   │   └── RateLimitFilter.java            (Bucket4j filter for /api/rate-limit-test)
│   ├── security/
│   │   ├── SecurityConfig.java             (filter chain, roles, CSRF, BCrypt, login portals)
│   │   ├── DatabaseUserDetailsService.java (loads users for form login)
│   │   ├── ApiSecurityErrorHandler.java    (JSON 401/403 for /api/**)
│   │   ├── TabAuthFilter.java              (X-Tab-Auth-Token authentication in TOKEN mode)
│   │   ├── TabAuthToken.java
│   │   └── TabAuthTokenService.java        (in-memory opaque tokens, TTL + cleanup)
│   ├── validation/
│   │   ├── OnCreate.java                   (create-only validation group)
│   │   └── OnUpdate.java                   (update validation group)
│   ├── exception/
│   │   ├── ResourceNotFoundException.java  (unknown id -> 404)
│   │   ├── ApiError.java                   (standard JSON error body)
│   │   └── GlobalExceptionHandler.java     (maps exceptions to JSON errors)
│   ├── service/
│   │   ├── DonationSyncService.java        (records <-> history sync + donation notification)
│   │   └── NotificationService.java        (notification feed, read state, retention cleanup)
│   ├── controller/
│   │   ├── PageController.java             (HTML page routes)
│   │   ├── RegisterController.java         (HTML registration form)
│   │   ├── UserController.java             (REST API: /api/users and profile)
│   │   ├── AdminController.java            (legacy REST API: /api/admins; not used for login)
│   │   ├── DonationGoalController.java     (REST API: /api/goals)
│   │   ├── DonationRecordController.java   (REST API: /api/donation-records)
│   │   ├── DonationHistoryController.java  (REST API: /api/donation-history)
│   │   ├── DonationOptionsController.java  (REST API: /api/donation-options)
│   │   ├── AnnouncementController.java     (REST API: /api/announcements)
│   │   ├── NotificationController.java     (REST API: /api/notifications)
│   │   ├── TabAuthController.java          (REST API: /api/auth)
│   │   └── RateLimitController.java        (REST API: /api/rate-limit-test)
│   ├── entity/
│   │   ├── User.java
│   │   ├── ProfilePicture.java
│   │   ├── Admin.java
│   │   ├── DonationGoal.java
│   │   ├── DonationRecord.java
│   │   ├── DonationHistory.java
│   │   ├── DonationType.java              (enum + display labels)
│   │   ├── PaymentMethod.java             (shared enum)
│   │   ├── Announcement.java
│   │   ├── Notification.java
│   │   ├── NotificationRead.java
│   │   └── NotificationType.java
│   └── repository/
│       ├── UserRepository.java
│       ├── ProfilePictureRepository.java
│       ├── AdminRepository.java
│       ├── DonationGoalRepository.java
│       ├── DonationRecordRepository.java
│       ├── DonationHistoryRepository.java
│       ├── AnnouncementRepository.java
│       ├── NotificationRepository.java
│       └── NotificationReadRepository.java
├── src/main/resources/
│   ├── application.properties
│   ├── static/
│   │   ├── css/styles.css
│   │   ├── js/app.js           (shared fetch helper with CSRF support)
│   │   └── js/notifications.js (navbar bell, polling, read state)
│   └── templates/
│       ├── index.html          (Home page + public community impact counts)
│       ├── dashboard.html      (Stats overview + admin growth reports)
│       ├── users.html          (User management)
│       ├── goals.html          (Donation goals with progress)
│       ├── donation-records.html(Donation records table)
│       ├── donation-history.html(Donation history table)
│       ├── announcements.html  (Announcement board)
│       ├── profile.html        (Profile, avatar upload, password change)
│       ├── register.html       (Registration form)
│       ├── login.html          (Login page)
│       └── fragments/layout.html
├── src/test/resources/          (H2 in-memory test configuration)
├── pom.xml
├── mvnw                          (Maven wrapper - Unix)
├── mvnw.cmd                      (Maven wrapper - Windows)
└── PROJECT_OVERVIEW.md           (This file)
```

## URL Map

### Page Routes (HTML)

| URL | Page | Access |
|---|---|---|
| `/` | Home | Public |
| `/login` | Login page | Public |
| `/register` | Registration page | Public |
| `/dashboard` | Dashboard with stats | Authenticated |
| `/users` | User management | Admin |
| `/admins` | Redirects to `/users` | Admin |
| `/goals` | Donation goals | Public |
| `/donation-records` | Donation records | Authenticated (both roles can add records; the records table and edit actions are Admin-only; anonymous redirected to `/login`) |
| `/donation-history` | Donation history | Authenticated |
| `/announcements` | Announcements | Public |
| `/profile` | Profile and password page | Authenticated |

### API Conventions

- All API endpoints are under `/api/**` and return JSON unless noted (profile-picture GET returns image bytes).
- Successful reads and writes return HTTP `200`; this project does not use `201`.
- `DELETE` handlers return `200` with an empty body.
- Authentication and authorization failures on `/api/**` return JSON from `ApiSecurityErrorHandler`.
- All state-changing requests (POST/PUT/DELETE and form login/logout) require a valid CSRF token; a missing or invalid token returns `403`.

### Authentication & CSRF

The application has two authentication modes, selected by `app.auth.mode` (`SESSION` is the default in `application.properties`).

**SESSION mode (default)**

1. Submit the login form at `POST /login` with `username`, `password`, and optional `loginPortal` (`user` or `admin`).
2. Spring Security creates an HTTP session (`JSESSIONID`), configured HttpOnly, SameSite=Lax, and `Secure` when `SESSION_COOKIE_SECURE=true`.
3. Call the API with the session cookie.

The login success handler redirects to `/dashboard`. If `loginPortal=admin` is supplied for a non-admin, or `loginPortal=user` for an admin, login is rejected with `/login?error=portal`. Other failures redirect to `/login?error`.

**TOKEN mode (`app.auth.mode=TOKEN`)**

Server-side token machinery only: no browser code requests a token, stores it in `sessionStorage`, or sends the `X-Tab-Auth-Token` header, so per-tab login is not available to users yet. All browser requests still use the session cookie in both modes (full page loads always depend on the session cookie).

- Obtain a token with `POST /api/auth/token` while already authenticated (for example after a form login). The endpoint returns an opaque random token and its username.
- Send the token on later requests in the `X-Tab-Auth-Token` header. `TabAuthFilter` validates it and populates the security context.
- Tokens are stored in memory only, expire after `app.tabauth.token-ttl-minutes` (default 30) with sliding renewal on use, and expired tokens are cleaned up every `app.tabauth.cleanup-interval-ms` (default 60000 ms).
- A valid token wins over a session cookie belonging to a different user. A request carrying an invalid or expired token is rejected with `401` (fail-closed) and does **not** fall back to the session identity, except for `POST /api/auth/logout`, which still returns `200` with a bad or missing token.
- In SESSION mode the filter ignores the token header completely.
- `POST /api/auth/logout` revokes the token supplied in `X-Tab-Auth-Token`.
- `POST /api/auth/logout-all` revokes every active token.
- In SESSION mode, `/api/auth/token` and `/api/auth/logout-all` return `403 {"error":"Token auth disabled"}`.
- `/api/auth/logout` returns `200` even with an invalid or missing token; it revokes a token only in TOKEN mode and does **not** invalidate an HTTP session. Use `POST /logout` for session logout.

**CSRF**

- CSRF is enabled in both modes. Spring Security uses `CookieCsrfTokenRepository` with the header name `X-XSRF-TOKEN` and an `XSRF-TOKEN` cookie (SameSite=Lax, Secure when session cookies are secure).
- Templates expose the token in `<meta name="_csrf">` and the header name in `<meta name="_csrf_header">`; `static/js/app.js` reads those meta tags and sends the header automatically.
- The `X-Tab-Auth-Token` header is an authentication mechanism, not a CSRF bypass. TOKEN-mode calls that change state still need a CSRF token.
- The login, registration, and logout forms include the `_csrf` hidden field.

### Standard error format

Handled errors use this JSON shape (`ApiError`), with `details` present only for validation failures:

```json
{
  "timestamp": "2026-11-03T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/goals",
  "details": ["title: Title is required"]
}
```

| Status | When it is returned |
|---|---|
| `400 Bad Request` | Bean-validation failure, malformed/unreadable JSON, invalid enum value, or an `IllegalArgumentException` such as duplicate username, wrong password, forbidden role change, self-deletion, or last-admin protection. |
| `401 Unauthorized` | No authenticated user for a protected `/api/**` endpoint. Body: `{"status":401,"error":"Unauthorized","message":"Authentication required","path":"..."}`. |
| `403 Forbidden` | Authenticated but lacking the required role, or missing/invalid CSRF token. API authorization failures use the same `ApiError` shape. |
| `404 Not Found` | Unknown id or endpoint, or a non-owner requesting a donation-history entry or unpublished announcement. |
| `409 Conflict` | A database constraint conflict (`DataIntegrityViolationException`), e.g. duplicate key or invalid reference. |

### Rate limiting

Bucket4j is used by `config/RateLimitFilter`, which runs before Spring Security. It applies only to:

- `GET /api/rate-limit-test`

Limits: capacity 5 requests per client IP, greedy refill of 5 requests per minute. Responses include `X-RateLimit-Limit` and `X-RateLimit-Remaining`. When exhausted, the API returns `429 Too Many Requests`, a `Retry-After` header, and the standard `ApiError` body with message `"Rate limit exceeded. Please try again later."`. The client IP is the direct connection address; forwarded headers are intentionally not trusted.

No other endpoint is rate limited.

## API Reference

### Users — `/api/users`

| Method | URL | Description | Access | Request body | Success | Errors |
|---|---|---|---|---|---|---|
| GET | `/api/users` | List all user accounts. | Admin | — | `200`, array of `User` | `401`, `403` |
| POST | `/api/users` | Create a user account; password is BCrypt-hashed. | Admin | `User` create JSON (fields below) | `200`, `User` | `400`, `401`, `403`, `409` |
| GET | `/api/users/{username}` | Read one account. | Authenticated (self or Admin) | — | `200`, `User` | `401`, `403`, `404` |
| PUT | `/api/users/{username}` | Update email, full name, role, and/or password. | Authenticated (self or Admin; role changes Admin only) | `User` update JSON or an object with the same fields (fields below) | `200`, `User` | `400`, `401`, `403`, `404`, `409` |
| DELETE | `/api/users/{username}` | Delete an account and its stored profile picture. | Admin | — | `200`, empty body | `400`, `401`, `403`, `404` |

Request body fields (create uses the `OnCreate` validation group; update is **not** annotated with `@Valid`):

| Field | Type | Required | Rules / notes |
|---|---|---|---|
| `username` | string | Create: yes | `@NotBlank` on create; must not already exist. Only used in the create body; on update the path variable is authoritative. No API-level pattern or length check. |
| `email` | string | Create: yes | `@NotBlank` and `@Email` on create. On update it is applied only when it is a non-blank string; the update path does not run bean validation. |
| `fullName` | string | No | `@Size(max = 150)` on create. Optional on update. |
| `password` | string | Create: yes | `@NotBlank` on create; write-only, never returned. Controller requires at least 6 characters for `USER`, at least 12 for `ADMIN`. On update it is optional and the same role-based minimum applies. |
| `role` | string | No | `@Pattern(regexp = "ADMIN|USER")` on create; defaults to `USER`. On update only an Admin may change it, it is subject to self-demotion/last-admin checks, and the pattern is not re-validated because update has no `@Valid`. |
| `dateCreated` | timestamp | No | Accepted in the body but overwritten by `@PrePersist`. Treated as read-only in practice. |
| `profilePicture` | string | No | Optional marker; the avatar upload sets it to `"db"`. No validation. |

Create example:

```json
{
  "username": "jdoe",
  "email": "jdoe@example.org",
  "fullName": "Jane Doe",
  "password": "secret123",
  "role": "USER"
}
```

Response (`200`, password omitted):

```json
{
  "username": "jdoe",
  "email": "jdoe@example.org",
  "fullName": "Jane Doe",
  "role": "USER",
  "dateCreated": "2026-11-03T12:00:00.000+00:00",
  "profilePicture": null
}
```

Account rules:

- A user can read and update their own account but cannot change their own role.
- An Admin can read/update any account, change roles, and delete accounts.
- No one can delete their own account.
- The last administrator cannot be deleted or demoted; an administrator cannot remove their own administrator access.

### Profile — `/api/users/me`

| Method | URL | Description | Access | Request body | Success | Errors |
|---|---|---|---|---|---|---|
| GET | `/api/users/me` | Return the authenticated user's account. | Authenticated | — | `200`, `User` | `401`, `404` |
| POST | `/api/users/me/change-password` | Change the authenticated user's password after checking the current one. | Authenticated | `currentPassword`, `newPassword` | `200`, empty body | `400`, `401`, `403`, `404` |
| POST | `/api/users/me/profile-picture` | Upload/replace the authenticated user's avatar. | Authenticated | `multipart/form-data` with field `file` | `200`, `{"profilePicture":"db","url":"/api/users/me/profile-picture"}` | `400`, `401`, `403`, `404` |
| GET | `/api/users/{username}/profile-picture` | Return an avatar as image bytes; the literal `me` means the current user. | Authenticated | — | `200`, image bytes (`image/jpeg`, `image/png`, or `image/webp`) | `401`, `404` |

| Field | Type | Required | Rules / notes |
|---|---|---|---|
| `currentPassword` | string | Yes | Must match the stored BCrypt hash; blank is rejected. |
| `newPassword` | string | Yes | At least 6 characters for `USER`, at least 12 for `ADMIN`. |
| `file` (multipart) | binary | Yes | Maximum 5 MB (also `spring.servlet.multipart.max-file-size` and `max-request-size` = 5 MB). Content is detected by magic bytes: JPEG, PNG, or WebP only. |

Change-password example:

```json
{
  "currentPassword": "secret123",
  "newPassword": "newSecret123"
}
```

Profile-picture response:

```json
{
  "profilePicture": "db",
  "url": "/api/users/me/profile-picture"
}
```

Notes:

- `GET /api/users/{username}/profile-picture` only requires authentication; it does not restrict a user to their own picture.
- **UNCLEAR:** the exact HTTP status when an upload exceeds the container multipart limit before the controller runs. The controller returns `400` for its own 5 MB/type checks, but `GlobalExceptionHandler` has no `MaxUploadSizeExceededException` mapping (`src/main/resources/application.properties:31-33`, `src/main/java/project2/exception/GlobalExceptionHandler.java`).

### Goals — `/api/goals`

| Method | URL | Description | Access | Request body | Success | Errors |
|---|---|---|---|---|---|---|
| GET | `/api/goals` | List all donation goals. | Public | — | `200`, array of `DonationGoal` | — |
| GET | `/api/goals/{id}` | Read one goal. | Authenticated | — | `200`, `DonationGoal` | `401`, `404` |
| POST | `/api/goals` | Create a goal. | Admin | `DonationGoal` JSON (fields below) | `200`, `DonationGoal` | `400`, `401`, `403`, `409` |
| PUT | `/api/goals/{id}` | Update a goal. | Admin | `DonationGoal` JSON (fields below) | `200`, `DonationGoal` | `400`, `401`, `403`, `404`, `409` |
| DELETE | `/api/goals/{id}` | Delete a goal. | Admin | — | `200`, empty body | `401`, `403`, `404` |

Request body fields (`@Valid` on create and update):

| Field | Type | Required | Rules / notes |
|---|---|---|---|
| `id` | number | No | Read-only; ignored on create and taken from the path on update. |
| `title` | string | Yes | `@NotBlank`. |
| `description` | string | No | Free text (`TEXT` column). |
| `targetAmount` | number / BigDecimal | No | `@DecimalMin("0.00")`; defaults to `0` when omitted. |
| `currentAmount` | number / BigDecimal | No | `@DecimalMin("0.00")`; defaults to `0`. On update a supplied negative value is ignored. |
| `startDate` | date (`YYYY-MM-DD`) | No | No server-side ordering check. |
| `endDate` | date (`YYYY-MM-DD`) | No | No server-side ordering check; the frontend checks `endDate >= startDate`. |
| `status` | string | No | Free string. The UI sends `Active`, `Paused`, or `Completed`, but the server accepts any value. |

Create example:

```json
{
  "title": "Community Food Drive",
  "description": "Monthly food packs",
  "targetAmount": 10000.00,
  "currentAmount": 0,
  "startDate": "2026-11-01",
  "endDate": "2026-12-31",
  "status": "Active"
}
```

Response (`200`):

```json
{
  "id": 1,
  "title": "Community Food Drive",
  "description": "Monthly food packs",
  "targetAmount": 10000.00,
  "currentAmount": 0,
  "startDate": "2026-11-01",
  "endDate": "2026-12-31",
  "status": "Active"
}
```

### Donation Records — `/api/donation-records`

| Method | URL | Description | Access | Request body | Success | Errors |
|---|---|---|---|---|---|---|
| GET | `/api/donation-records` | List all donation records. | Admin | — | `200`, array of `DonationRecord` | `401`, `403` |
| GET | `/api/donation-records/{id}` | Read one record. | Admin | — | `200`, `DonationRecord` | `401`, `403`, `404` |
| POST | `/api/donation-records` | Create a record and a matching history entry. | Authenticated | `DonationRecord` JSON (fields below) | `200`, `DonationRecord` | `400`, `401`, `403`, `409` |
| PUT | `/api/donation-records/{id}` | Update a record (does not update the linked history entry). | Admin | `DonationRecord` JSON (fields below) | `200`, `DonationRecord` | `400`, `401`, `403`, `404`, `409` |
| DELETE | `/api/donation-records/{id}` | Delete a record (does not delete the linked history entry). | Admin | — | `200`, empty body | `401`, `403`, `404` |

Request body fields (`@Valid` on create and update):

| Field | Type | Required | Rules / notes |
|---|---|---|---|
| `recordId` | number | No | Read-only; generated by the database. |
| `donorName` | string | Yes | `@NotBlank`. |
| `dateDonated` | date (`YYYY-MM-DD`) | Yes | `@NotNull`, `@PastOrPresent`. |
| `donorAddress` | string | Yes | `@NotBlank`. |
| `donationType` | string | Yes | `@NotBlank`. Free string; the UI offers `Cash`, `Check`, `Online`, `In-Kind`, `Other` from `DonationType.labels()`, but the server accepts any non-blank value. |
| `amount` | number / BigDecimal | No | `@DecimalMin("0.00")`; defaults to `0` when omitted; stored as `DECIMAL(12,2)`. |
| `paymentMethod` | enum string | Yes | `@NotNull`; one of `Cash`, `GCash`, `PayMaya`, `Maya`, `PalawanPawnshop`, `CebuanaLhuillier`, `Check`, `Personal`. |

Create example:

```json
{
  "donorName": "Jane Doe",
  "dateDonated": "2026-11-01",
  "donorAddress": "123 Main St",
  "donationType": "Cash",
  "amount": 500.00,
  "paymentMethod": "Cash"
}
```

Response (`200`):

```json
{
  "recordId": 1,
  "donorName": "Jane Doe",
  "dateDonated": "2026-11-01",
  "donorAddress": "123 Main St",
  "donationType": "Cash",
  "amount": 500.00,
  "paymentMethod": "Cash"
}
```

Create behavior:

- When a non-admin creates a record, the matching history entry uses the authenticated username instead of the submitted `donorName`.
- When an Admin creates a record, the matching history entry uses the submitted `donorName`.
- The history entry copies `donationType` to `item`, plus `amount`, `paymentMethod`, and `dateDonated`.
- Each successful create writes one notification for the other users; the actor is excluded from their own notification feed.

### Donation History — `/api/donation-history`

| Method | URL | Description | Access | Request body | Success | Errors |
|---|---|---|---|---|---|---|
| GET | `/api/donation-history` | List history entries. Admins see all; other users see only their own, newest first. | Authenticated | — | `200`, array of `DonationHistory` | `401` |
| GET | `/api/donation-history/{id}` | Read one history entry. Admins may read any; other users only their own. | Authenticated | — | `200`, `DonationHistory` | `401`, `404` |
| POST | `/api/donation-history` | Create a history entry and a matching record. | Authenticated | `DonationHistory` JSON (fields below) | `200`, `DonationHistory` | `400`, `401`, `403`, `409` |
| PUT | `/api/donation-history/{id}` | Update a history entry (does not update the linked record). | Admin | `DonationHistory` JSON (fields below) | `200`, `DonationHistory` | `400`, `401`, `403`, `404`, `409` |
| DELETE | `/api/donation-history/{id}` | Delete a history entry (does not delete the linked record). | Admin | — | `200`, empty body | `401`, `403`, `404` |

Request body fields (`@Valid` on create and update):

| Field | Type | Required | Rules / notes |
|---|---|---|---|
| `id` | number | No | Read-only; generated by the database. |
| `username` | string | Yes | `@NotBlank`; non-admin creates are overwritten with the authenticated username. Admins may supply another username. |
| `item` | string | Yes | `@NotBlank`. |
| `amount` | number / BigDecimal | No | `@DecimalMin("0.00")`; defaults to `0`; stored as `DECIMAL(12,2)`. |
| `paymentMethod` | enum string | Yes | `@NotNull`; same values as donation records. |
| `dateDonated` | timestamp | No | Defaults to the current time on insert if omitted. On update this field is not copied from the request body. |

Create example:

```json
{
  "username": "jdoe",
  "item": "Cash",
  "amount": 500.00,
  "paymentMethod": "Cash"
}
```

Response (`200`):

```json
{
  "id": 1,
  "username": "jdoe",
  "item": "Cash",
  "amount": 500.00,
  "paymentMethod": "Cash",
  "dateDonated": "2026-11-03T12:00:00.000+00:00"
}
```

Create behavior:

- A non-admin's submitted `username` is replaced by the authenticated username.
- The matching record uses `username` as `donorName`, `item` as `donationType`, the same `amount` and `paymentMethod`, and `donorAddress = "N/A"` (history has no address).

### Announcements — `/api/announcements`

| Method | URL | Description | Access | Request body | Success | Errors |
|---|---|---|---|---|---|---|
| GET | `/api/announcements` | List announcements. Non-admins see published announcements only; admins see all. | Public | — | `200`, array of `Announcement` | — |
| GET | `/api/announcements/categories` | List default plus stored announcement categories. | Admin | — | `200`, array of strings | `401`, `403` |
| GET | `/api/announcements/{id}` | Read one announcement. Non-admins cannot read an unpublished one. | Authenticated | — | `200`, `Announcement` | `401`, `404` |
| POST | `/api/announcements` | Create an announcement. | Admin | `Announcement` JSON (fields below) | `200`, `Announcement` | `400`, `401`, `403`, `409` |
| PUT | `/api/announcements/{id}` | Update an announcement. | Admin | `Announcement` JSON (fields below) | `200`, `Announcement` | `400`, `401`, `403`, `404`, `409` |
| POST | `/api/announcements/{id}/publish` | Mark an announcement published. | Admin | — | `200`, `Announcement` | `401`, `403`, `404` |
| POST | `/api/announcements/{id}/unpublish` | Mark an announcement unpublished. | Admin | — | `200`, `Announcement` | `401`, `403`, `404` |
| DELETE | `/api/announcements/{id}` | Delete an announcement. | Admin | — | `200`, empty body | `401`, `403`, `404` |

Request body fields (`@Valid` on create and update):

| Field | Type | Required | Rules / notes |
|---|---|---|---|
| `id` | number | No | Read-only; ignored on create, taken from the path on update. |
| `title` | string | Yes | `@NotBlank`, `@Size(max = 200)`. A server-side title fallback exists, but `@NotBlank` runs first, so a title must be supplied in API requests. |
| `message` | string | Yes | `@NotBlank`; `TEXT` column. `message` and `announcementContent` are kept in sync by the entity setters. Because `message` is required by validation, supplying only `announcementContent` is not enough on create. |
| `announcementContent` | string | No | Optional alias/content field; `TEXT` column. |
| `category` | string | Yes | `@NotBlank`, `@Size(max = 100)`. |
| `publicationDate` | date (`YYYY-MM-DD`) | No | No cross-field validation against expiration date. |
| `expirationDate` | date (`YYYY-MM-DD`) | No | No cross-field validation. |
| `status` | string | No | `"Published"` sets `published=true`; `"Draft"` sets it to `false` (case-insensitive). Other values leave `published` unchanged/default. If omitted, it is derived from `published`. |
| `published` | boolean | No | Defaults to `true`. |
| `datePosted` | timestamp | No | Defaults to now if omitted. |
| `publishedAt` | timestamp | No | Defaults to now when published and null; cleared on unpublish. |
| `createdAt` | timestamp | No | Defaults to now if omitted. |
| `updatedAt` | timestamp | No | Set to now on create/update. |
| `createdBy` | string | No | Defaults to the authenticated username on create if omitted. |

Create example:

```json
{
  "title": "Holiday Drive",
  "message": "We are collecting gifts until December 20.",
  "announcementContent": "We are collecting gifts until December 20.",
  "category": "Campaigns",
  "publicationDate": "2026-11-15",
  "expirationDate": "2026-12-20",
  "status": "Published",
  "published": true
}
```

Response (`200`, abbreviated):

```json
{
  "id": 1,
  "title": "Holiday Drive",
  "message": "We are collecting gifts until December 20.",
  "announcementContent": "We are collecting gifts until December 20.",
  "category": "Campaigns",
  "publicationDate": "2026-11-15",
  "expirationDate": "2026-12-20",
  "status": "Published",
  "published": true,
  "publishedAt": "2026-11-03T12:00:00.000+00:00",
  "createdBy": "admin"
}
```

### Notifications - `/api/notifications`

Notifications are stored once per event and read state is stored per user in `notification_reads`, so one event does not create one row per account. The notification feed is newest first and capped at 20 items. The actor who triggered an event is excluded from their own notification feed. Notifications created before a user's `dateCreated` are shown as read and do not count toward the unread badge.

| Method | URL | Description | Access | Request body | Success | Errors |
|---|---|---|---|---|---|---|
| GET | `/api/notifications` | List the latest 20 notifications for the current user with a per-item `read` flag. | Authenticated | — | `200`, array of notification views | `401` |
| GET | `/api/notifications/unread-count` | Count unread notifications for the current user. | Authenticated | — | `200`, `{"count":0}` | `401` |
| POST | `/api/notifications/{id}/read` | Mark one notification read for the current user. | Authenticated | — | `204`, empty body | `401`, `403`, `404` |
| POST | `/api/notifications/read-all` | Mark every visible notification read for the current user. | Authenticated | — | `204`, empty body | `401`, `403` |

Notification view fields:

| Field | Type | Notes |
|---|---|---|
| `id` | number | Notification identifier. |
| `type` | enum string | `DONATION_CREATED` or `ANNOUNCEMENT_PUBLISHED`. |
| `title` | string | Plain text title. |
| `message` | string | Generic plain text message with no donor address, payment method, or exact amount. |
| `link` | string | Internal path such as `/donation-records` or `/announcements`. |
| `createdAt` | timestamp | Creation time. |
| `read` | boolean | Read state for the current user. |

Triggers and retention:

- Creating a donation record writes one `New donation recorded` notification. Creating a history entry writes one as well.
- Creating a published announcement writes one `New announcement: <title>` notification. Drafts do not notify; publishing a draft notifies once; re-publishing or editing an already published announcement does not notify again.
- The bell in the shared navbar polls the unread count every 30 seconds and pauses while the tab is hidden.
- A scheduled cleanup deletes notifications older than `app.notifications.retention-days` (default `30` days). The schedule interval is `app.notifications.cleanup-interval-ms` (default one day).

### Auth — `/api/auth` and form endpoints

| Method | URL | Description | Access | Request body | Success | Errors |
|---|---|---|---|---|---|---|
| POST | `/login` | Form login (session). | Public | `application/x-www-form-urlencoded`: `username`, `password`, optional `loginPortal` (`user`/`admin`) | `302` to `/dashboard`; failures `302` to `/login?error` or `/login?error=portal` | Missing/invalid CSRF -> `403`; bad credentials -> redirect |
| POST | `/logout` | Spring Security session logout. | Public | — | `302` to `/login?logout`; session invalidated | Missing/invalid CSRF -> `403` |
| POST | `/register` | Create an account from the registration form; role is `USER` unless Admin is chosen with a valid admin code. | Public | form fields: `username`, `email`, `password`, `confirmPassword`, optional `role`, `adminCode` | `302` to `/login?registered`; validation error re-renders the form | `200` form with error message |
| POST | `/api/auth/token` | Issue an in-memory tab token. | Public filter-level; controller requires `TOKEN` mode and an authenticated caller | — | `200`, `{"token":"...","username":"..."}` | `401` not authenticated; `403` when `app.auth.mode` is not `TOKEN` |
| POST | `/api/auth/logout` | Revoke the token in `X-Tab-Auth-Token` when in TOKEN mode. | Public | Optional header `X-Tab-Auth-Token` | `200`, `{"status":"logged_out"}` | — |
| POST | `/api/auth/logout-all` | Revoke all active tab tokens. | Public filter-level; controller requires `TOKEN` mode and an authenticated caller | — | `200`, `{"status":"logged_out_all"}` | `401` not authenticated; `403` when not in `TOKEN` mode |

Registration form rules (`RegisterController`):

| Field | Type | Required | Rules |
|---|---|---|---|
| `username` | string | Yes | Trimmed; `^[A-Za-z0-9_.-]{3,50}$`; must not already exist. |
| `email` | string | Yes | Trimmed; simple pattern requiring `local@domain.tld`; must not already be used (`existsByEmail`). |
| `password` | string | Yes | At least 6 characters; at least 12 characters when `role` is ADMIN. |
| `confirmPassword` | string | Yes | Must equal `password`. |
| `role` | string | No | User/Admin, server decides; defaults to `USER`. Any missing, blank, or unrecognized value becomes `USER`. |
| `adminCode` | string | Only for Admin | Required only when `role` is Admin; compared in constant time (`MessageDigest.isEqual`) against `app.admin-signup-code` (env var `ADMIN_SIGNUP_CODE`). Blank config disables admin sign-up. |

The server decides the role: `ADMIN` is granted only when `app.admin-signup-code` is configured and the submitted code matches; a wrong or missing code returns an "Invalid admin code" validation error and creates no account.

Token example:

```json
{
  "token": "3xY...base64url...",
  "username": "jdoe"
}
```

### Public / Stats

There are **no dedicated stats, community-impact, or growth-report endpoints** in the controllers.

- The public home page (`templates/index.html`) calculates “Community Impact” counts in the browser from the public `GET /api/goals` and `GET /api/announcements` responses.
- The dashboard growth report (`templates/dashboard.html`) is calculated in the browser from `GET /api/donation-records` (Admin) and `GET /api/donation-history` (Authenticated). No server-side aggregation endpoint is called.
- The only public diagnostic endpoint is `GET /api/rate-limit-test`.

| Method | URL | Description | Access | Request body | Success | Errors |
|---|---|---|---|---|---|---|
| GET | `/api/rate-limit-test` | Verify that the Bucket4j filter is active; returns the request timestamp. | Public, rate limited | — | `200`, `{"message":"Request accepted","timestamp":"..."}` | `429` when the limit is exceeded |

### Donation Options — `/api/donation-options`

| Method | URL | Description | Access | Request body | Success | Errors |
|---|---|---|---|---|---|---|
| GET | `/api/donation-options/types` | Return donation type display labels: `Cash`, `Check`, `Online`, `In-Kind`, `Other`. | Authenticated | — | `200`, array of strings | `401` |
| GET | `/api/donation-options/payment-methods` | Return `PaymentMethod` enum names. | Authenticated | — | `200`, array of strings | `401` |

No `/api/donation-options/**` path is in the public list, so both endpoints require authentication even though the pages that use them may be public.

### Legacy Admins — `/api/admins`

The `admins` table is a legacy directory. `DatabaseUserDetailsService` authenticates only from the `users` table, so records created here **do not grant login access**. Real administrator access is controlled by `User.role = "ADMIN"` on `/api/users`. The `/admins` page simply redirects to `/users`.

| Method | URL | Description | Access | Request body | Success | Errors |
|---|---|---|---|---|---|---|
| GET | `/api/admins` | List legacy admin-directory records. | Admin | — | `200`, array of `Admin` | `401`, `403` |
| GET | `/api/admins/{name}` | Read one legacy record. | Admin | — | `200`, `Admin` | `401`, `403`, `404` |
| POST | `/api/admins` | Create a legacy directory record; does not grant login access. | Admin | `Admin` create JSON (fields below) | `200`, `Admin` | `400`, `401`, `403`, `409` |
| PUT | `/api/admins/{name}` | Update a legacy record's password. | Admin | `Admin` update JSON (fields below) | `200`, `Admin` | `400`, `401`, `403`, `404`, `409` |
| DELETE | `/api/admins/{name}` | Delete a legacy record. | Admin | — | `200`, empty body | `401`, `403`, `404` |

Request body fields:

| Field | Type | Required | Rules / notes |
|---|---|---|---|
| `admin` | string | Create: yes | `@NotBlank` under `OnCreate`; must not already exist. On update the path variable is authoritative. |
| `password` | string | Create: yes | `@NotBlank` under `OnCreate`; write-only. At least 12 characters. On update it is optional; when supplied it must be at least 12 characters. |

Create example:

```json
{
  "admin": "legacy-admin",
  "password": "a-very-long-password"
}
```

Response (`200`, password omitted):

```json
{
  "admin": "legacy-admin"
}
```

## Record ↔ History sync

Donation records and donation history are kept in sync automatically by `service/DonationSyncService`:

- Adding a **record** also writes a matching **history** entry.
- Adding a **history** entry also writes a matching **record**.
- The sync is one hop only (the internal save writes the counterpart directly via its repository), so it never loops or creates duplicates.
- Updating or deleting one side does **not** update or delete the counterpart.

Field mapping:

| Record | → | History |
|---|---|---|
| donorName (or authenticated username for non-admin record creates) | | username |
| donationType | | item |
| dateDonated | | dateDonated |
| amount | | amount |
| paymentMethod | | paymentMethod |

| History | → | Record |
|---|---|---|
| username | | donorName |
| item | | donationType |
| dateDonated | | dateDonated |
| amount | | amount |
| paymentMethod | | paymentMethod |
| — | | donorAddress = `"N/A"` (history has no address) |

## Libraries & external services

| Dependency (from `pom.xml`) | Used for |
|---|---|
| `spring-boot-starter-web` | REST controllers, embedded Tomcat, Jackson JSON |
| `spring-boot-starter-validation` | Jakarta Bean Validation (`@NotBlank`, `@Size`, `@Email`, etc.) |
| `spring-boot-starter-data-jpa` | Hibernate/JPA entities and Spring Data repositories |
| `spring-boot-starter-thymeleaf` | Server-side HTML templates |
| `spring-boot-starter-security` | Authentication, role authorization, BCrypt, CSRF, sessions |
| `thymeleaf-extras-springsecurity6` | `sec:authorize` support in Thymeleaf templates |
| `bucket4j-core` 8.10.1 | Token-bucket rate limiting for `GET /api/rate-limit-test` |
| `mysql-connector-j` (runtime) | MySQL JDBC driver used by the runtime datasource |
| `h2` (runtime) | In-memory database for tests (`src/test/resources/application.properties`); not the production datasource |
| `spring-boot-starter-test` (test) | JUnit, Spring Test, Mockito, AssertJ |
| `spring-security-test` (test) | MockMvc security helpers such as `@WithMockUser` and `csrf()` |
| `spring-boot-devtools` (runtime, optional) | Development-time restart and tooling |

The Spring Boot application itself calls **no external APIs**. Browser pages load third-party static assets from CDNs: Bootstrap 5.3.3 CSS/JS, Bootstrap Icons 1.11.3, and Google Fonts (Inter). Those are browser-side requests, not server-side API integrations.

## Security

- **Authentication:** form login at `/login` backed by `DatabaseUserDetailsService`; optional opaque token authentication via `X-Tab-Auth-Token` when `app.auth.mode=TOKEN`.
- **Passwords:** hashed with BCrypt; never returned in JSON responses (`@JsonProperty(access = WRITE_ONLY)`).
- **Roles:** `ADMIN` and `USER`. Admin-only pages and endpoints are enforced server-side with both URL rules and `@PreAuthorize`.
- **Administrator access:** real login accounts are managed in `/users` by changing their `ADMIN` or `USER` role. The separate `/api/admins` directory is legacy and does not grant login access.
- **CSRF:** enabled with a cookie repository and the `X-XSRF-TOKEN` header. Failed/absent tokens return HTTP `403`.
- **Session cookies:** HttpOnly and SameSite=Lax are enabled. Set `SESSION_COOKIE_SECURE=true` when serving over HTTPS.
- **Rate limiting:** only `GET /api/rate-limit-test` is limited (5 requests/minute per IP).
- **Initial administrator:** no default accounts are created. On an empty database, configure `APP_BOOTSTRAP_ADMIN_USERNAME`, `APP_BOOTSTRAP_ADMIN_EMAIL`, and `APP_BOOTSTRAP_ADMIN_PASSWORD` before the first start. The password must be at least 12 characters. Store these values in the runtime's secret manager; never commit them.
- **Admin self-registration:** `app.admin-signup-code` (environment variable `ADMIN_SIGNUP_CODE`, blank by default) enables the Admin role choice on the registration form. While blank, registering as Admin is disabled server-side.
- **Account controls:** the administrator screen supports create, edit, password reset, role changes, and deletion. The last administrator cannot be deleted or demoted, and an administrator cannot remove their own access or delete their own account.
- **Browser output:** user-submitted text is escaped before rendering.

## How to Run

Before starting on an empty database, set the three `APP_BOOTSTRAP_ADMIN_*` environment variables above, plus `DB_PASSWORD` for MySQL. For local development, keep `SESSION_COOKIE_SECURE=false` while using plain HTTP; use `true` behind HTTPS. Do not put real passwords in shell history or source files.

```sh
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Then open: `http://localhost:8080/`

Tests use an in-memory H2 database via `src/test/resources/application.properties`, so they do not need local MySQL credentials.

## Database

The application uses **MySQL** by default (`src/main/resources/application.properties`):

- URL: `jdbc:mysql://localhost:3306/donation_system`
- Username: `donation_app`
- Password: `${DB_PASSWORD}` (environment variable)
- Hibernate: `spring.jpa.hibernate.ddl-auto=update`

The H2 console is disabled by default and can be enabled with `H2_CONSOLE_ENABLED=true`; keep it disabled outside local debugging. Tests use an in-memory H2 database and `create-drop`.

Earlier versions used a file-based H2 database. The `data/` directory contains leftover H2 files from those versions; the current runtime configuration does not use them.

This change does not rewrite password hashes already stored in an existing database, and removing credentials from the current files does not remove them from older Git commits. Before using an existing demo database, change any starter account passwords in the Users screen.

## Features

- Spring Security authentication with BCrypt-hashed passwords and ADMIN/USER roles
- Session login plus optional in-memory tab-token authentication
- CSRF-protected REST API and forms
- Full REST CRUD (create, read, update, delete) for every entity
- Automatic record ↔ history sync (add one, the other is created too)
- Bean Validation on request bodies with structured 400 error responses
- Consistent JSON error handling (400/401/403/404/409/429) via a security handler, a global exception handler, and the rate-limit filter
- Dashboard with live statistics
- Browser-calculated donation growth reports for administrators
- Public community-impact counts on the home page
- Profile management with database-stored image avatars (JPEG/PNG/WebP, max 5 MB)
- Progress tracking for donation goals
- Bucket4j rate limiting for the diagnostic endpoint
- Responsive Bootstrap 5 UI with safer account administration and output encoding
