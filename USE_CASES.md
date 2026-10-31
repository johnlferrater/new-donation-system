# Donation System - Use Cases

## Use Case Keys

| Key | Use Case | Actor | Description |
|---|---|---|---|
| **UC-01** | Login | User / Admin | Allows a user or administrator to log into the system. |
| **UC-01b** | Logout | User / Admin | Ends the authenticated session (per-tab or shared session depending on auth mode). |
| **UC-01c** | Tab-Specific Authentication | User / Admin | Supports independent authentication of Admin and User in separate tabs using per-tab tokens (when enabled). |
| **UC-02** | View Dashboard | User / Admin | Displays donation statistics and system overview. Includes admin-only growth reports. |
| **UC-03** | Manage Users | Admin | Create and view user accounts. |
| **UC-04** | Manage Administrator Roles | Admin | Grant or remove administrator access on real user accounts. |
| **UC-05** | Manage Donation Goals | Admin | Create and view donation goals and monitor their progress. |
| **UC-06** | Manage Donation Records | Admin / User | Create and view donation records with extended payment methods. |
| **UC-07** | View Donation History | Admin / User | View previous donation history with extended payment methods. |
| **UC-08** | Manage Announcements | Admin | Create and view announcements with dynamic status (Draft/Published). |
| **UC-09** | View Announcements | User | View published announcements posted by administrators. |
| **UC-10** | Track Goal Progress | Admin / User | View the progress of donation goals. |
| **UC-11** | Access REST API | System / Application | Provides REST endpoints for users, admins, goals, records, history, and announcements. |
| **UC-12** | Access H2 Database | System | Stores and retrieves system data using the H2 database. |
| **UC-13** | Profile Picture Management | User / Admin | Upload and display profile pictures in Profile, Navbar, and Dashboard. |
| **UC-14** | View Community Impact | Public / User / Admin | View public aggregate statistics on the Home page. |

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

### User
- Login / Logout (per-tab or shared)
- View dashboard
- View donation goals
- View donation records
- View donation history
- View announcements
- Manage own profile picture
- View community impact

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
- **Precondition:** User has an account provisioned by an administrator
- **Flow:**
  1. User navigates to `/login`
  2. User enters username and password (optionally selects portal)
  3. System validates credentials against BCrypt-hashed passwords and assigns the user's role (ADMIN/USER)
  4. User is redirected to the dashboard
  5. In TOKEN mode, client may request a per-tab token via `/api/auth/token`
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
- **Flow:**
  1. Admin logs in Tab 1, receives tokenA stored in Tab 1's sessionStorage
  2. User logs in Tab 2, receives tokenB stored in Tab 2's sessionStorage
  3. Each tab sends its own token via `X-Tab-Auth-Token` header
  4. Spring Security authenticates each request independently per token
- **Postcondition:** Two different accounts authenticated simultaneously in separate tabs

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
  2. System displays all donation records in a table
  3. User clicks "Add Record" button
  4. User fills in donor name, address, donation type, date, amount, and payment method (Cash, GCash, PayMaya, Maya, Palawan Pawnshop, Cebuana Lhuillier, Check, Personal)
  5. System saves the new record
- **Postcondition:** New donation record is created

### UC-07: View Donation History
- **Actor:** Admin / User
- **Precondition:** User is logged in
- **Flow:**
  1. User navigates to `/donation-history`
  2. System displays all donation history entries
  3. User can view username, item, amount, payment method (extended options), and date
- **Postcondition:** User sees donation history

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
  1. Client sends HTTP request to `/api/*` endpoints (GET, POST, PUT, DELETE)
  2. System validates the request body and the caller's role/CSRF token
  3. System processes the request and returns a JSON response
  4. Invalid input returns 400 with field-level details; unknown ids return 404
- **Postcondition:** Client receives data or a structured JSON error

### UC-12: Access H2 Database
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
  3. System validates file type and size, generates unique filename
  4. System stores file securely outside webroot and updates user profile
  5. Profile picture displays in Profile page, Navbar, and Dashboard
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
