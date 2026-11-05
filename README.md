# Donation System

A Spring Boot web application for managing donation goals, donation records and history, announcements, and users. It has a public site (home, goals, announcements, pledge form) and an administrator area (dashboard, records, users, reports, and more).

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security 6 (form login, BCrypt, CSRF cookie) |
| Views | Thymeleaf, Bootstrap 5.3, Bootstrap Icons |
| Database | MySQL 8 (`donation_system`) |
| Tests | JUnit with H2 (in-memory, tests only) |
| Build | Maven (Maven Wrapper included) |

## Roles

- **Public visitor**: can view the home page, goals, and announcements.
- **USER**: registered user who can sign in, create donations, and view their history.
- **ADMIN**: full access to the admin area (goals, records, announcements, users, reports, audit logs, settings).

---

## 1. Prerequisites

Install the following before you start:

1. **JDK 17** (verify with `java -version`)
2. **MySQL 8** (verify with `mysql --version`)
3. **Git**
4. **VS Code** with the *Extension Pack for Java* and *Spring Boot Extension Pack* (optional but recommended)

You do **not** need to install Maven. Use the included wrapper (`mvnw` / `mvnw.cmd`).

## 2. Get the Project

```powershell
git clone https://github.com/johnlferrater/new-donation-system.git
cd new-donation-system
```

## 3. Create the Database

Open the MySQL shell (or MySQL Workbench) and run:

```sql
CREATE DATABASE donation_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Optional: a dedicated user instead of root
CREATE USER 'donation_user'@'localhost' IDENTIFIED BY 'change-this-password';
GRANT ALL PRIVILEGES ON donation_system.* TO 'donation_user'@'localhost';
FLUSH PRIVILEGES;
```

Tables are created automatically by Hibernate on first run (see `spring.jpa.hibernate.ddl-auto` in `application.properties`).

## 4. Configure the Application

Settings live in `src/main/resources/application.properties`. Open it and set the values below.

### 4.1 Database connection

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/donation_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=donation_user
spring.datasource.password=change-this-password
```

### 4.2 Server port

```properties
server.port=8080
```

Change this if port 8080 is already in use.

### 4.3 Admin sign-up code

Users can register themselves on the `/register` page. To let someone register as an **ADMIN**, they must enter an admin code.

```properties
app.admin-signup-code=choose-a-long-secret-code
```

You can also supply it as an environment variable instead of writing it in the file:

```powershell
$env:ADMIN_SIGNUP_CODE = "choose-a-long-secret-code"
```

Keep this code private. Anyone who knows it can create an admin account.

### 4.4 Authentication mode

```properties
app.auth.mode=SESSION
```

- `SESSION` (default): standard browser session login.
- `TOKEN`: per-tab token login (server side only for now; the browser UI does not use it yet).

### 4.5 Password rules

| Role | Minimum length |
|------|----------------|
| USER | 6 characters |
| ADMIN | 12 characters |

## 5. Keep Secrets Out of Git

Do not commit real passwords or codes. Two safe options:

**Option A: environment variables** (Spring maps them automatically):

```powershell
$env:SPRING_DATASOURCE_USERNAME = "donation_user"
$env:SPRING_DATASOURCE_PASSWORD = "change-this-password"
$env:ADMIN_SIGNUP_CODE          = "choose-a-long-secret-code"
```

**Option B: a local override file**, e.g. `src/main/resources/application-local.properties` (add it to `.gitignore`), then run with the `local` profile:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

## 6. Run the Application

From the project folder in PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Or in VS Code, open `Project2Application.java` and click **Run** above the `main` method.

When you see `Started Project2Application`, open:

**http://localhost:8080**

## 7. First Login and Seed Data

On startup, `DataSeeder` can create initial data (such as a default admin account and sample records). Check `DataSeeder.java` for the exact usernames and passwords it creates, and **change those passwords immediately** after your first login.

If no seeded admin exists, register at `/register` using your admin sign-up code (section 4.3).

## 8. Run the Tests

```powershell
.\mvnw.cmd test
```

Tests use an in-memory H2 database, so MySQL does not need to be running for them.

## 9. Build a Runnable JAR

```powershell
.\mvnw.cmd clean package -DskipTests
java -jar target\*.jar
```

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `Communications link failure` or `Access denied for user` | Make sure MySQL is running, and that the URL, username, and password in section 4.1 are correct. |
| `Unknown database 'donation_system'` | Run the `CREATE DATABASE` command in section 3. |
| `Port 8080 was already in use` | Change `server.port` or stop the other program using it. |
| `JAVA_HOME` not set or wrong Java version | Install JDK 17 and point `JAVA_HOME` to it. |
| `.\mvnw.cmd` is not recognized | Make sure you are inside the project folder (the one containing `pom.xml`). |
| Cannot create an admin account | Check that the admin code matches `app.admin-signup-code` and that the password is at least 12 characters. |
| `git commit` fails with "Please tell me who you are" | Run `git config --global user.name "Your Name"` and `git config --global user.email "you@example.com"`. |

## Project Layout

```
new-donation-system/
├── pom.xml
├── mvnw / mvnw.cmd
└── src/
    ├── main/
    │   ├── java/project2/        # controllers, services, repositories, security config
    │   └── resources/
    │       ├── application.properties
    │       ├── static/           # css/styles.css, js/app.js
    │       └── templates/        # Thymeleaf pages and fragments
    └── test/                     # unit and integration tests
```
