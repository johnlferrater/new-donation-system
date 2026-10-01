# Donation System - Project Overview

## Description

A web-based donation management system built with Spring Boot. It allows users to track donations, manage goals, record donor information, and post announcements.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2.5 |
| Database | H2 (in-memory) |
| Frontend | Thymeleaf, Bootstrap 5 |
| Build | Maven (wrapper included) |

## Project Structure

```
new-donation-system/
├── src/main/java/project2/
│   ├── Project2Application.java
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

| URL | Page |
|---|---|
| `/` | Home |
| `/dashboard` | Dashboard with stats |
| `/users` | User management |
| `/admins` | Admin management |
| `/goals` | Donation goals |
| `/donation-records` | Donation records |
| `/donation-history` | Donation history |
| `/announcements` | Announcements |
| `/login` | Login page |

### API Routes (REST)

| Method | URL | Description |
|---|---|---|
| GET | `/api/users` | Get all users |
| POST | `/api/users` | Create a user |
| GET | `/api/admins` | Get all admins |
| POST | `/api/admins` | Create an admin |
| GET | `/api/goals` | Get all goals |
| POST | `/api/goals` | Create a goal |
| GET | `/api/donation-records` | Get all records |
| POST | `/api/donation-records` | Create a record |
| GET | `/api/donation-history` | Get all history |
| POST | `/api/donation-history` | Create a history entry |
| GET | `/api/announcements` | Get all announcements |
| POST | `/api/announcements` | Create an announcement |

## How to Run

```powershell
# Using Maven wrapper (recommended)
.\mvnw spring-boot:run

# Or with Maven installed
mvn spring-boot:run
```

Then open: `http://localhost:8080/`

## Database

- **H2 Console**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:mem:donationdb`
- **Username**: `sa`
- **Password**: `john123`

## Features

- Dashboard with live statistics
- CRUD operations for all entities
- Progress tracking for donation goals
- Responsive Bootstrap 5 UI
- RESTful API for all data operations
