# Donation System - Project Overview

## Description

A web-based donation management system built with Spring Boot. It allows users to track donations, manage goals, record donor information, and post announcements.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2.5 |
| Security | Spring Security 6 (form login, BCrypt, roles, CSRF) |
| Database | H2 (file-based, persists across restarts) |
| Frontend | Thymeleaf, Bootstrap 5, Thymeleaf Spring Security extras |
| Build | Maven (wrapper included) |

## Project Structure

```
new-donation-system/
├── src/main/java/project2/
│   ├── Project2Application.java
│   ├── config/
│   │   └── DataSeeder.java              (seed admin/user accounts on startup)
│   ├── security/
│   │   ├── SecurityConfig.java          (filter chain, roles, CSRF, BCrypt)
│   │   └── DatabaseUserDetailsService.java (loads users for authentication)
│   ├── validation/
│   │   ├── OnCreate.java                (create-only validation group)
│   │   └── OnUpdate.java                (update validation group)
│   ├── exception/
│   │   ├── ResourceNotFoundException.java (unknown id -> 404)
│   │   ├── ApiError.java                (standard JSON error body)
│   │   └── GlobalExceptionHandler.java  (maps exceptions to JSON errors)
│   ├── service/
│   │   └── DonationSyncService.java     (records <-> history sync)
│   ├── controller/
│   │   ├── PageController.java          (HTML page routes)
│   │   ├── UserController.java          (REST API: /api/users)
│   │   ├── AdminController.java         (REST API: /api/admins)
│   │   ├── DonationGoalController.java  (REST API: /api/goals)
│   │   ├── DonationRecordController.java(REST API: /api/donation-records)
│   │   ├── DonationHistoryController.java(REST API: /api/donation-history)
│   │   └── AnnouncementController.java  (REST API: /api/announcements)
│   ├── entity/
│   │   ├── User.java
│   │   ├── Admin.java
│   │   ├── DonationGoal.java
│   │   ├── DonationRecord.java
│   │   ├── DonationHistory.java
│   │   ├── PaymentMethod.java           (shared enum)
│   │   └── Announcement.java
│   └── repository/
│       ├── UserRepository.java
│       ├── AdminRepository.java
│       ├── DonationGoalRepository.java
│       ├── DonationRecordRepository.java
│       ├── DonationHistoryRepository.java
│       └── AnnouncementRepository.java
├── src/main/resources/
│   ├── application.properties
│   ├── static/
│   │   ├── css/styles.css
│   │   └── js/app.js           (shared fetch helper with CSRF support)
│   └── templates/
│       ├── index.html          (Home page)
│       ├── dashboard.html      (Stats overview)
│       ├── users.html          (User management)
│       ├── admins.html         (Admin management)
│       ├── goals.html          (Donation goals with progress)
│       ├── donation-records.html(Donation records table)
│       ├── donation-history.html(Donation history table)
│       ├── announcements.html  (Announcement board)
│       └── login.html          (Login page)
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
| `/dashboard` | Dashboard with stats | Authenticated |
| `/users` | User management | Admin only |
| `/admins` | Admin management | Admin only |
| `/goals` | Donation goals | Authenticated |
| `/donation-records` | Donation records | Authenticated |
| `/donation-history` | Donation history | Authenticated |
| `/announcements` | Announcements | Authenticated |

### API Routes (REST)

| Method | URL | Description | Access |
|---|---|---|---|
| GET | `/api/users` | Get all users | Admin |
| GET | `/api/users/{username}` | Get one user | Admin |
| POST | `/api/users` | Create a user (password hashed) | Admin |
| PUT | `/api/users/{username}` | Update a user (password optional) | Admin |
| DELETE | `/api/users/{username}` | Delete a user (cannot delete self) | Admin |
| GET | `/api/admins` | Get all admins | Admin |
| GET | `/api/admins/{name}` | Get one admin | Admin |
| POST | `/api/admins` | Create an admin (password hashed) | Admin |
| PUT | `/api/admins/{name}` | Update an admin (password optional) | Admin |
| DELETE | `/api/admins/{name}` | Delete an admin | Admin |
| GET | `/api/goals` | Get all goals | Authenticated |
| GET | `/api/goals/{id}` | Get one goal | Authenticated |
| POST | `/api/goals` | Create a goal | Admin |
| PUT | `/api/goals/{id}` | Update a goal | Admin |
| DELETE | `/api/goals/{id}` | Delete a goal | Admin |
| GET | `/api/donation-records` | Get all records | Authenticated |
| GET | `/api/donation-records/{id}` | Get one record | Authenticated |
| POST | `/api/donation-records` | Create a record (also creates a history entry) | Authenticated |
| PUT | `/api/donation-records/{id}` | Update a record | Authenticated |
| DELETE | `/api/donation-records/{id}` | Delete a record | Authenticated |
| GET | `/api/donation-history` | Get all history | Authenticated |
| GET | `/api/donation-history/{id}` | Get one history entry | Authenticated |
| POST | `/api/donation-history` | Create a history entry (also creates a record) | Authenticated |
| PUT | `/api/donation-history/{id}` | Update a history entry | Authenticated |
| DELETE | `/api/donation-history/{id}` | Delete a history entry | Authenticated |

### Record ↔ History sync

Donation records and donation history are kept in sync automatically by
`service/DonationSyncService`:

- Adding a **record** also writes a matching **history** entry.
- Adding a **history** entry also writes a matching **record**.
- The sync is one hop only (the internal save writes the counterpart directly via
  its repository), so it never loops or creates duplicates.

Field mapping:

| Record | → | History |
|---|---|---|
| donorName | | username |
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
| GET | `/api/announcements` | Get all announcements | Authenticated |
| GET | `/api/announcements/{id}` | Get one announcement | Authenticated |
| POST | `/api/announcements` | Create an announcement | Admin |
| PUT | `/api/announcements/{id}` | Update an announcement | Admin |
| DELETE | `/api/announcements/{id}` | Delete an announcement | Admin |

### Validation & errors

- Request bodies are validated with Jakarta Bean Validation. Failures return **400**
  with a `details` array of `field: message` entries.
- Unknown ids return **404**; duplicate keys return **400**/**409**; malformed JSON
  returns **400**.
- All errors use a consistent JSON shape:

  ```json
  {
    "timestamp": "2026-10-04T22:15:00Z",
    "status": 400,
    "error": "Bad Request",
    "message": "Validation failed",
    "path": "/api/goals",
    "details": ["title: Title is required"]
  }
  ```

- Generated ids and passwords are write-protected/omitted in responses.

All state-changing requests require a CSRF token. The browser pages obtain the token
from the `XSRF-TOKEN` cookie and send it in the `X-XSRF-TOKEN` header (handled
automatically by `static/js/app.js`).

## Security

- **Authentication:** form login at `/login` backed by `DatabaseUserDetailsService`.
- **Passwords:** hashed with BCrypt; never returned in JSON responses.
- **Roles:** `ADMIN` and `USER`. Admin-only pages and endpoints are enforced server-side.
- **CSRF:** enabled with a cookie repository. Failed/absent tokens return HTTP 403.
- **Logout:** `POST /logout`.
- **Default accounts** (created by `DataSeeder` when the user table is empty):

  | Username | Password | Role |
  |---|---|---|
  | `admin` | `admin123` | ADMIN |
  | `user` | `user123` | USER |

  Change these before any real deployment.

## How to Run

```powershell
# Using Maven wrapper (recommended)
.\mvnw spring-boot:run

# Or with Maven installed
mvn spring-boot:run
```

Then open: `http://localhost:8080/`

## Database

The app uses a **file-based H2 database** so data survives restarts.

- **H2 Console**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:file:./data/donationdb`
- **Username**: `root`
- **Password**: `john123`

The database files are created under the `data/` directory in the project root
(`donationdb.mv.db`, `donationdb.lock.db`) and are excluded from git via `.gitignore`.
Deleting the `data/` folder resets the database (it will be recreated with the
default seeded accounts on next start).

> **Note:** Earlier versions used `jdbc:h2:mem:donationdb`, which stores everything
> in memory only — every restart wiped all data. If records "don't save", confirm
> this property points at a `file:` URL, not `mem:`.

## Features

- Spring Security authentication with BCrypt-hashed passwords and ADMIN/USER roles
- CSRF-protected REST API and forms
- Full REST CRUD (create, read, update, delete) for every entity
- Automatic record ↔ history sync (add one, the other is created too)
- Bean Validation on all request bodies with structured 400 error responses
- Consistent JSON error handling (400/404/409) via a global exception handler
- Dashboard with live statistics
- Progress tracking for donation goals
- Responsive Bootstrap 5 UI
