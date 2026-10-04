# Donation System - Use Cases

## Use Case Keys

| Key | Use Case | Actor | Description |
|---|---|---|---|
| **UC-01** | Login | User / Admin | Allows a user or administrator to log into the system. |
| **UC-01b** | Logout | User / Admin | Ends the authenticated session. |
| **UC-02** | View Dashboard | User / Admin | Displays donation statistics and system overview. |
| **UC-03** | Manage Users | Admin | Create and view user accounts. |
| **UC-04** | Manage Administrators | Admin | Create and view administrator accounts. |
| **UC-05** | Manage Donation Goals | Admin | Create and view donation goals and monitor their progress. |
| **UC-06** | Manage Donation Records | Admin / User | Create and view donation records. |
| **UC-07** | View Donation History | Admin / User | View previous donation history. |
| **UC-08** | Manage Announcements | Admin | Create and view announcements. |
| **UC-09** | View Announcements | User | View announcements posted by administrators. |
| **UC-10** | Track Goal Progress | Admin / User | View the progress of donation goals. |
| **UC-11** | Access REST API | System / Application | Provides REST endpoints for users, admins, goals, records, history, and announcements. |
| **UC-12** | Access H2 Database | System | Stores and retrieves system data using the H2 database. |

## Main Actors

### Admin
- Login
- Manage users
- Manage administrators
- Manage donation goals
- Manage donation records
- View donation history
- Create announcements
- View dashboard

### User
- Login
- View dashboard
- View donation goals
- View donation records
- View donation history
- View announcements

### System
- Process REST API requests
- Store and retrieve data
- Calculate/display donation goal progress
- Provide dashboard statistics

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
 Manage Users  Manage Admins          View Dashboard  View Goals
    |            |                         |             |
 Manage Goals   Announcements          View Records   View History
    |                                      |
 Manage Records                        View Announcements
    |
 View History
    |
 View Dashboard
```

## Use Case Details

### UC-01: Login
- **Actor:** User / Admin
- **Precondition:** User has an account (see default accounts in PROJECT_OVERVIEW.md)
- **Flow:**
  1. User navigates to `/login`
  2. User enters username and password
  3. System validates credentials against BCrypt-hashed passwords and assigns the user's role (ADMIN/USER)
  4. User is redirected to the dashboard
- **Postcondition:** User is authenticated and the session carries their role

### UC-01b: Logout
- **Actor:** User / Admin
- **Precondition:** User is logged in
- **Flow:**
  1. User selects Logout from the account menu
  2. System submits `POST /logout` with the CSRF token
  3. Session is invalidated and the user is redirected to `/login?logout`
- **Postcondition:** User is logged out

### UC-02: View Dashboard
- **Actor:** User / Admin
- **Precondition:** User is logged in
- **Flow:**
  1. User navigates to `/dashboard`
  2. System displays total users, goals, records, announcements
  3. System shows recent goals with progress bars
  4. System shows recent donation history
- **Postcondition:** User sees system overview

### UC-03: Manage Users
- **Actor:** Admin
- **Precondition:** Admin is logged in
- **Flow:**
  1. Admin navigates to `/users`
  2. System displays all users in a table
  3. Admin clicks "Add User" button
  4. Admin fills in username, email, password
  5. System saves the new user
- **Postcondition:** New user is created

### UC-04: Manage Administrators
- **Actor:** Admin
- **Precondition:** Admin is logged in
- **Flow:**
  1. Admin navigates to `/admins`
  2. System displays all admins in a table
  3. Admin clicks "Add Admin" button
  4. Admin fills in admin name and password
  5. System saves the new admin
- **Postcondition:** New admin is created

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
  4. User fills in donor name, address, type, and date
  5. System saves the new record
- **Postcondition:** New donation record is created

### UC-07: View Donation History
- **Actor:** Admin / User
- **Precondition:** User is logged in
- **Flow:**
  1. User navigates to `/donation-history`
  2. System displays all donation history entries
  3. User can view username, item, amount, payment method, and date
- **Postcondition:** User sees donation history

### UC-08: Manage Announcements
- **Actor:** Admin
- **Precondition:** Admin is logged in
- **Flow:**
  1. Admin navigates to `/announcements`
  2. System displays all announcements
  3. Admin clicks "Post Announcement" button
  4. Admin enters message
  5. System saves the new announcement
- **Postcondition:** New announcement is posted

### UC-09: View Announcements
- **Actor:** User
- **Precondition:** User is logged in
- **Flow:**
  1. User navigates to `/announcements`
  2. System displays all announcements with timestamps
- **Postcondition:** User sees all announcements

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
  1. System connects to H2 in-memory database
  2. Data is stored and retrieved via JPA repositories
  3. H2 console available at `/h2-console` for direct access
- **Postcondition:** Data is persisted
