# Donation System - Use Cases

## Use Case Keys

| Key | Use Case | Actor | Description |
|---|---|---|---|
| **UC-01** | Login | User / Admin | Allows a user or administrator to log into the system. |
| **UC-01b** | Logout | User / Admin | Ends the authenticated session (per-tab or shared session depending on auth mode). |
| **UC-01c** | Tab-Specific Authentication | User / Admin | Server-side per-tab token machinery exists (when enabled), but no browser code requests, stores, or sends the token, so per-tab login is not available to users yet. |
| **UC-02** | View Dashboard | User / Admin | Displays donation statistics and system overview. Includes admin-only growth reports. |
| **UC-03** | Manage Users | Admin | Create, edit, reset passwords, change roles, and delete user accounts. |
| **UC-04** | Manage Administrator Roles | Admin | Grant or remove administrator access on real user accounts. |
| **UC-05** | Manage Donation Goals | Admin | Create, edit, delete, and monitor donation goals. |
| **UC-06** | Manage Donation Records | Admin / User | Both roles can create donation records; Admins can view, edit, and delete all records. |
| **UC-07** | View Donation History | Admin / User | Admins view all history; users view only their own entries. |
| **UC-08** | Manage Announcements | Admin | Create, edit, publish/unpublish, and delete announcements with Draft/Published status. |
| **UC-09** | View Announcements | User | View published announcements posted by administrators. |
| **UC-10** | Track Goal Progress | Admin / User | View the progress of donation goals. |
| **UC-11** | Access REST API | System / Application | Provides REST endpoints for users, admins, goals, records, history, and announcements. |
| **UC-12** | Access Database | System | Stores and retrieves system data in MySQL via JPA (H2 is used only by tests). |
| **UC-13** | Profile Picture Management | User / Admin | Upload and display profile pictures in Profile, Navbar, and Dashboard. |
| **UC-14** | View Community Impact | Public / User / Admin | View public aggregate statistics on the Home page. |
| **UC-15** | Receive Notifications | User / Admin | See donation and announcement notifications in the shared navbar bell, with per-user read state. |

## Main Actors

### Admin
- Login / Logout (per-tab or shared)
- Manage users
- Manage administrator roles
- Manage donation goals
- Manage donation records
- View donation history
- Create/edit announcements (Draft/Published)
- View dashboard with growth reports
- Manage profile picture
- View community impact
- Receive notifications

### User
- Login / Logout (per-tab or shared)
- View dashboard
- View donation goals
- Create donation records
- View own donation history
- View announcements
- Manage own profile picture
- View community impact
- Receive notifications

### Public Visitor
- View Home page
- View Community Impact statistics
- View published goals and announcements

### System
- Process REST API requests
- Store and retrieve data
- Calculate/display donation goal progress
- Provide dashboard statistics
- Issue/revoke tab authentication tokens (when enabled)
- Validate file uploads securely
- Create notifications for donations and published announcements
- Track notification read state per user and clean up expired notifications

## Use-Case Flow

```
                    DONATION MANAGEMENT SYSTEM
                              |
           +-------------------+-------------------+
           |                                       |
         ADMIN                                    USER
           |                                       |
        Login                                   Login
           |                                       |
     +-----+------+                         +------+------+
     |            |                         |             |
   Manage Users  Manage Roles           View Dashboard  View Goals
     |            |                         |             |
  Manage Goals   Announcements          View Records   View History
     |            |                         |             |
  Manage Records Profile Pic           View Announcements Profile Pic
     |
  View History
     |
  View Dashboard
     |
  Growth Reports
```

**Public:** Home page displays Community Impact statistics.

## Use Case Details

### UC-01: Login
- **Actor:** User / Admin
- **Precondition:** User has an account (registered via `/register` or provisioned by an administrator)
- **Flow:**
  1. User navigates to `/login`
  2. User enters username and password (optionally selects portal)
  3. System validates credentials against BCrypt-hashed passwords and assigns the user's role (ADMIN/USER)
  4. User is redirected to the dashboard
  5. In TOKEN mode, client may request a per-tab token via `/api/auth/token`
- **Registration (`/register`, public):** The form asks for username, email, password, and a role choice (User by default). Choosing Admin reveals a required admin code field; the server grants ADMIN only when the code matches `app.admin-signup-code` (environment variable `ADMIN_SIGNUP_CODE`, compared in constant time). A blank config disables admin sign-up, and any missing/bad code or unrecognized role is rejected or defaults to USER as applicable.
- **Postcondition:** User is authenticated and the session/token carries their role

### UC-01b: Logout
- **Actor:** User / Admin
- **Precondition:** User is logged in
- **Flow:**
  1. User selects Logout from the account menu
  2. In SESSION mode: System submits `POST /logout` with the CSRF token; shared session is invalidated
  3. In TOKEN mode: Client sends `POST /api/auth/logout` with `X-Tab-Auth-Token` header; only that tab's token is revoked
  4. User is redirected to `/login?logout`
- **Postcondition:** Selected authentication context is ended

### UC-01c: Tab-Specific Authentication
- **Actor:** User / Admin
- **Precondition:** Application running with `app.auth.mode=TOKEN`
- **Status:** Server-side token machinery exists (issue/validate/revoke via `/api/auth/*` and `TabAuthFilter`), but no browser code requests a token, stores it in `sessionStorage`, or sends the `X-Tab-Auth-Token` header. Per-tab login is therefore not available to users yet; the session cookie is used for all browser requests.
- **Server-side behavior (verified by tests):**
  1. An authenticated caller can obtain an in-memory per-tab token via `POST /api/auth/token`
  2. A request carrying a valid token is authenticated as that token's user, even when the session cookie belongs to a different user
  3. In TOKEN mode, a request carrying an invalid or expired token is rejected with `401` and does not fall back to the session identity (fail-closed); `POST /api/auth/logout` still returns `200` with a bad or missing token
  4. In SESSION mode the filter ignores the token header completely


### UC-02: View Dashboard
- **Actor:** User / Admin
- **Precondition:** User is logged in
- **Flow:**
  1. User navigates to `/dashboard`
  2. System displays donation metrics to all users and the account count to administrators
  3. System shows recent goals with progress bars
  4. System shows recent donation history
- **Postcondition:** User sees system overview

### UC-03: Manage Users
- **Actor:** Admin
- **Precondition:** Admin is logged in
- **Flow:**
  1. Admin navigates to `/users`
  2. System displays all users in a table
  3. Admin searches accounts or chooses "Add user"
  4. Admin creates, edits, resets a password, changes a role, or deletes an account
  5. The system validates the request and prevents removal of the last administrator
- **Postcondition:** The account changes are saved with the correct role

### UC-04: Manage Administrator Roles
- **Actor:** Admin
- **Precondition:** Admin is logged in
- **Flow:**
  1. Admin navigates to `/users`
  2. System displays login accounts and current roles
  3. Admin edits a user and selects the `ADMIN` role
  4. System validates the role change and preserves at least one administrator
- **Postcondition:** The selected login account has administrator access

### UC-05: Manage Donation Goals
- **Actor:** Admin
- **Precondition:** Admin is logged in
- **Flow:**
  1. Admin navigates to `/goals`
  2. System displays all goals with progress bars
  3. Admin clicks "Add Goal" button
  4. Admin fills in title and target amount
  5. System saves the new goal
- **Postcondition:** New goal is created

### UC-06: Manage Donation Records
- **Actor:** Admin / User
- **Precondition:** User is logged in
- **Flow:**
  1. User navigates to `/donation-records`
  2. The "Add Record" button is available to both roles
  3. User fills in donor name, address, donation type, date, amount, and payment method (Cash, GCash, PayMaya, Maya, Palawan Pawnshop, Cebuana Lhuillier, Check, Personal)
  4. System saves the new record and a matching history entry
  5. System creates one notification for the other users; the actor is not notified
  6. Admins can view the records table and edit or delete records; users cannot read the all-records API
- **Postcondition:** New donation record is created; Admin-only record management remains enforced server-side

### UC-07: View Donation History
- **Actor:** Admin / User
- **Precondition:** User is logged in
- **Flow:**
  1. User navigates to `/donation-history`
  2. Admins see all history entries; users see only their own entries, newest first
  3. User can view username, item, amount, payment method (extended options), and date
  4. Users cannot read another user's history entry
- **Postcondition:** User sees the donation history they are allowed to read

### UC-08: Manage Announcements
- **Actor:** Admin
- **Precondition:** Admin is logged in
- **Flow:**
  1. Admin navigates to `/announcements`
  2. System displays all announcements
  3. Admin clicks "Create Announcement" button
  4. Admin enters title, content, category, dates, and selects status (Draft or Published)
  5. Button label updates dynamically based on status selection
  6. System saves announcement with correct status
- **Postcondition:** Announcement saved as Draft or Published

### UC-09: View Announcements
- **Actor:** User
- **Precondition:** User is logged in or public
- **Flow:**
  1. User navigates to `/announcements`
  2. System displays only published announcements to non-admin users
  3. Admins see all announcements with status
- **Postcondition:** User sees published announcements only

### UC-10: Track Goal Progress
- **Actor:** Admin / User
- **Precondition:** User is logged in
- **Flow:**
  1. User navigates to `/goals` or `/dashboard`
  2. System displays progress bars for each goal
  3. Progress is calculated as (currentAmount / targetAmount) * 100
- **Postcondition:** User sees goal progress

### UC-11: Access REST API
- **Actor:** System / Application
- **Precondition:** Application is running; caller is authenticated (and has ADMIN role where required)
- **Flow:**
  1. Client sends HTTP request to `/api/*` endpoints (GET, POST, PUT, DELETE), including `/api/notifications`
  2. System validates the request body and the caller's role/CSRF token
  3. System processes the request and returns a JSON response
  4. Invalid input returns 400 with field-level details; unknown ids return 404
- **Postcondition:** Client receives data or a structured JSON error

### UC-12: Access Database
- **Actor:** System
- **Precondition:** Application is running
- **Flow:**
  1. System connects to the MySQL database
  2. Data is stored and retrieved via JPA repositories
  3. The H2 console is disabled by default and should only be enabled for local debugging.
- **Postcondition:** Data is persisted

### UC-13: Profile Picture Management
- **Actor:** User / Admin
- **Precondition:** User is logged in
- **Flow:**
  1. User navigates to `/profile`
  2. User selects an image file (JPG, PNG, or WebP, max 5MB)
  3. System validates the image by content (JPEG, PNG, or WebP) and size (max 5 MB), stores the image in the database, and updates the user profile
  4. Profile picture displays in Profile page, Navbar, and Dashboard
- **Postcondition:** Profile picture updated and displayed

### UC-14: View Community Impact
- **Actor:** Public Visitor / User / Admin
- **Precondition:** None (public)
- **Flow:**
  1. Visitor navigates to home page `/`
  2. System displays Community Impact section with aggregate statistics
  3. Statistics include total goals, active goals, and published announcements
  4. No private donor information or financial details are exposed
- **Postcondition:** Public aggregates are displayed

### UC-15: Receive Notifications
- **Actor:** User / Admin
- **Precondition:** User is logged in
- **Flow:**
  1. System creates one notification when a donation record or history entry is created
  2. System creates one notification when an announcement is created as published or transitions from draft to published
  3. The actor who triggered the event is excluded from that notification
  4. User sees the unread count on the shared navbar bell; the bell polls every 30 seconds and pauses while the tab is hidden
  5. User opens the bell to see the latest 20 notifications and can open one or mark all as read
  6. Read state is stored separately for each user; opening or marking notifications never changes another user's state
  7. Notifications older than the account creation date are shown as read and do not count toward the unread badge
  8. A scheduled cleanup removes notifications older than `app.notifications.retention-days` (default 30 days)
- **Postcondition:** User sees only their own read state; donor address, payment method, and exact amount are not exposed in notification text
