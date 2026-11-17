# Attendance Management System

A complete, production-style **Attendance Management System** built with **Java 17 + Spring Boot 3 + Spring Security + Thymeleaf + MySQL 8**. It runs on a normal Windows 10/11 machine with a single setup script, has role-based access (Administrator / Faculty), and includes a responsive web UI, REST APIs, CSV reports, defaulters calculation, and a safe demo-data workflow.

---

## Table of Contents

1. [What it does](#1-what-it-does)
2. [Features](#2-features)
3. [Technology stack](#3-technology-stack)
4. [Project structure](#4-project-structure)
5. [One-click setup (recommended)](#5-one-click-setup-recommended)
6. [What the setup script installs](#6-what-the-setup-script-installs)
7. [Demo accounts](#7-demo-accounts)
8. [Running, stopping and restarting](#8-running-stopping-and-restarting)
9. [Configuration](#9-configuration)
10. [Database](#10-database)
11. [How to add / update data](#11-how-to-add--update-data)
12. [Using the application (demo walkthrough)](#12-using-the-application-demo-walkthrough)
13. [REST API reference](#13-rest-api-reference)
14. [Tests](#14-tests)
15. [Manual installation](#15-manual-installation)
16. [Troubleshooting](#16-troubleshooting)
17. [Security notes](#17-security-notes)
18. [Updating the project later](#18-updating-the-project-later)
19. [Backup and restore](#19-backup-and-restore)
20. [License](#20-license)

---

## 1. What it does

The system lets an administrator or faculty member:

- Manage **courses**, **subjects** and **students**.
- Create **attendance sessions** for a course + subject + semester + section + date.
- Mark students **Present / Absent** in bulk and edit a saved session.
- View **dashboard statistics** (students, courses, subjects, sessions, overall attendance, students below threshold).
- Generate **student-wise reports**, **subject-wise reports**, **session lists** and **defaulter lists**.
- **Export reports to CSV**.
- Manage **users** (Admin only) and change the **attendance threshold** (Admin only).
- Change their own **password**.

All names, emails and IDs in the demo seed are **fictional**.

---

## 2. Features

| Area | Details |
|---|---|
| Authentication | Spring Security, form login, BCrypt password hashing, session-based auth, CSRF protection, 30-minute idle timeout |
| Roles | `ADMIN` (full access) and `FACULTY` (students, courses, subjects, attendance, reports, profile) |
| Students | Add / edit / delete, search by name or ID, filter by course/semester/section |
| Courses | Add / edit / delete, safe checks before deleting referenced courses |
| Subjects | Add / edit / delete, linked to course + semester, safe checks before deleting referenced subjects |
| Attendance | Create session, bulk mark, edit saved session, cancel session; cancelled sessions excluded from calculations |
| Reports | Student-wise, subject-wise, session list, defaulters, CSV export |
| Dashboard | Live statistics from the database |
| Settings | Attendance threshold (default 75%) |
| Users | Admin can create/edit/delete users, enable/disable accounts, reset passwords |
| Validation | Server-side bean validation on all DTOs, friendly error messages, field-level errors in the UI |
| Data safety | Student deletion removes their attendance records first; code changes are blocked when referenced; unique constraints enforced |
| Setup | One-click PowerShell script that detects and installs missing prerequisites, initializes the DB, builds, tests and starts the app |

---

## 3. Technology stack

- **Java 17** (Amazon Corretto 17 recommended)
- **Spring Boot 3.2.5**
  - Spring Web (MVC + REST)
  - Spring Security
  - Spring Data JDBC (`JdbcTemplate`)
  - Spring Boot Actuator
  - Thymeleaf server-side templates
  - Validation (`spring-boot-starter-validation`)
  - Mail (optional SMTP notifications)
- **MySQL 8.x** (8.4 recommended)
- **Apache Maven 3.10.1** (build + tests)
- **HTML5 + CSS3 + vanilla JavaScript** (no SPA framework required)
- **Git** (optional, for version control)

> **Python is not required.** This is a Java/Spring Boot project; the setup script only uses Python if you want to regenerate the PDF guide.

---

## 4. Project structure

```
attendance-management-system/
├── README.md
├── .env.example                 # environment template (copied to .env by setup)
├── .gitignore
├── pom.xml                      # Maven build + dependencies
├── Run-Setup.bat                # one-click installer (calls the PS1)
├── Setup-AttendanceApp.ps1      # detects/installs Java, Maven, MySQL, Git; builds; runs
├── Run-AttendanceApp.bat        # starts the built app
├── Run-AttendanceApp.ps1
├── Stop-AttendanceApp.bat       # stops only this app
├── Stop-AttendanceApp.ps1
├── attendance-management-system-1.0.0.jar   # built by setup (git-ignored)
├── database/
│   ├── schema.sql               # idempotent schema
│   ├── seed.sql                 # fictional demo data
│   └── README.md
├── docs/
│   ├── architecture.md
│   ├── Attendance-System-Demo-Guide.pdf
│   └── ...
└── src/
    ├── main/
    │   ├── java/com/attendance/system/
    │   │   ├── AttendanceManagementApplication.java
    │   │   ├── config/          # SecurityConfig, DataInitializer
    │   │   ├── controller/      # REST + page controllers
    │   │   ├── dto/             # request/response DTOs with validation
    │   │   ├── exception/       # global error handler
    │   │   ├── model/           # entities + enums
    │   │   ├── repository/      # JdbcTemplate repositories
    │   │   ├── security/        # CustomUserDetails
    │   │   └── service/         # business logic
    │   └── resources/
    │       ├── application.properties
    │       ├── static/css/style.css
    │       ├── static/js/app.js
    │       └── templates/       # Thymeleaf pages + fragments
    └── test/java/...            # JUnit 5 + MockMvc tests
```

---

## 5. One-click setup (recommended)

### On a fresh Windows 10/11 PC

1. Copy the whole `attendance-management-system` folder to the PC.
2. Double-click **`Run-Setup.bat`** (or run `Setup-AttendanceApp.ps1` from PowerShell).
3. Wait. The script will:
   - Check for Java 17, Maven, MySQL and Git.
   - Download/install only what is missing (no admin rights needed in most cases).
   - Create `attendance_db` and `attendance_db_test`.
   - Create the restricted application user `attendance_app` with a random password saved in `.env`.
   - Run `schema.sql` in both databases (safe, no data is dropped).
   - Load the fictional demo data **only if the database is empty**.
   - Build the project and run all automated tests.
   - Start the app on `http://localhost:8080` and open your browser.

### What you need

- Windows 10/11 (64-bit)
- Internet connection (only for the first run / missing prerequisites)
- ~4 GB free disk space

### Notes

- The script is **idempotent**: running it again will not delete your data and will not reset an existing database password.
- If MySQL root is protected by a password, the script will ask for it once.
- If port 3306 is already used by an existing MySQL, the script uses that MySQL.
- If port 8080 is already used, the script assumes the app is already running and does not start a second instance.

---

## 6. What the setup script installs

| Component | Version | How |
|---|---|---|
| Amazon Corretto JDK | 17.x | winget (user scope) → fallback to portable MSI extract |
| Apache Maven | 3.10.1 | downloaded ZIP → `%LOCALAPPDATA%\AttendanceApp\prereqs\maven` |
| MySQL | 8.4.x | winget (machine scope) → fallback to portable MSI extract |
| Git | latest | winget (optional, only for version control) |
| Python | — | **not required**, skipped intentionally |

The script sets `JAVA_HOME`, `MAVEN_HOME` and adds them to your **User PATH**. It does not modify system-wide settings.

---

## 7. Demo accounts

On a **fresh database**, the application creates these accounts automatically (BCrypt-hashed):

| Role | Username | Password | Access |
|---|---|---|---|
| Administrator | `admin` | `admin123` | Everything |
| Faculty | `faculty` | `faculty123` | Students, courses, subjects, attendance, reports, profile |

> If the `users` table already has data, the initializer does **not** overwrite passwords. Create/reset users from **Users** (Admin) or via SQL.

Demo academic data (fictional):

- Courses: `CS101`, `CS202`, `EE101`, `MA101`
- Subjects: one per course, semester `Fall 2026`
- Students: `S1001` … `S1008`, across courses/semesters/sections

---

## 8. Running, stopping and restarting

### Start

Double-click **`Run-AttendanceApp.bat`**, or:

```powershell
powershell -ExecutionPolicy Bypass -File .\Run-AttendanceApp.ps1
```

Then open **http://localhost:8080**.

### Stop

Double-click **`Stop-AttendanceApp.bat`**, or:

```powershell
powershell -ExecutionPolicy Bypass -File .\Stop-AttendanceApp.ps1
```

`Stop-AttendanceApp` only stops the Spring Boot process recorded in `app.pid`. It does **not** stop MySQL.

### Logs

- `app.out.log` – application standard output
- `app.err.log` – application errors
- `setup.log` – full setup transcript

---

## 9. Configuration

All configuration is environment-based. The setup script creates a git-ignored `.env` from `.env.example`.

| Variable | Default | Purpose |
|---|---|---|
| `ATTENDANCE_DB_URL` | `jdbc:mysql://localhost:3306/attendance_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` | JDBC URL |
| `ATTENDANCE_DB_USER` | `attendance_app` | restricted DB user |
| `ATTENDANCE_DB_PASS` | generated random password | DB password |
| `ATTENDANCE_SMTP_HOST` | empty | SMTP host for email alerts (optional) |
| `ATTENDANCE_SMTP_PORT` | `587` | SMTP port |
| `ATTENDANCE_SMTP_USER` | empty | SMTP username |
| `ATTENDANCE_SMTP_PASS` | empty | SMTP password |
| `ATTENDANCE_SMTP_AUTH` | `false` | enable SMTP auth |
| `ATTENDANCE_SMTP_STARTTLS` | `true` | enable STARTTLS |
| `SERVER_PORT` | `8080` | HTTP port |
| `attendance.threshold.default` | `75` | default attendance percentage (also stored in `settings` table) |
| `attendance.demo.enabled` | `true` | create demo users on first start |

To change the DB password later:

1. Update the MySQL user password.
2. Update `ATTENDANCE_DB_PASS` in `.env`.
3. Restart the app.

---

## 10. Database

### Tables

| Table | Key columns | Notes |
|---|---|---|
| `users` | `username` unique, `password` (BCrypt), `role`, `enabled` | Admin/Faculty accounts |
| `courses` | `code` unique, `name`, `department` | Course catalog |
| `subjects` | `code` unique, `course_code` → `courses.code`, `semester` | Subject catalog |
| `students` | `student_id` unique, `name`, `email`, `course`, `semester`, `section` | Student roster |
| `attendance_sessions` | `course_code`, `subject_code`, `semester`, `section`, `session_date`, `status` (`active`/`cancelled`) | One row per class session |
| `attendance_records` | `session_id` → sessions, `student_id` → students, `status` (`PRESENT`/`ABSENT`) | One row per student per session |
| `settings` | `setting_key` unique, `setting_value` | App settings (e.g. `attendance.threshold`) |

### Schema / seed files

- `database/schema.sql` – idempotent; creates tables and indexes if missing. Safe to re-run.
- `database/seed.sql` – fictional demo data; uses `INSERT IGNORE`, so it is safe to re-run.

### Connect manually

```powershell
mysql -u root -p attendance_db
```

Or with the app user:

```powershell
mysql -u attendance_app -p attendance_db
```

### Useful queries

```sql
-- Students in a class
SELECT student_id, name, section
FROM students
WHERE course = 'CS101' AND semester = 'Fall 2026'
ORDER BY section, name;

-- Attendance for a session
SELECT r.student_id, r.student_name, r.status
FROM attendance_records r
JOIN attendance_sessions s ON r.session_id = s.id
WHERE s.course_code = 'CS101' AND s.subject_code = 'CS101'
  AND s.session_date = CURDATE()
ORDER BY r.student_name;

-- Per-student attendance percentage (active sessions only)
SELECT st.student_id, st.name,
       COUNT(r.id) AS total,
       SUM(r.status = 'PRESENT') AS present,
       ROUND(SUM(r.status = 'PRESENT') * 100.0 / NULLIF(COUNT(r.id), 0), 2) AS pct
FROM students st
LEFT JOIN attendance_records r ON r.student_id = st.student_id
LEFT JOIN attendance_sessions s ON r.session_id = s.id AND s.status = 'active'
GROUP BY st.student_id, st.name
ORDER BY pct ASC;
```

---

## 11. How to add / update data

There are three supported ways. Use the UI for day-to-day work, SQL for bulk/admin tasks, and the REST API for integrations.

### A. Using the web UI (recommended)

1. Log in as **admin**.
2. **Students** → **Add Student** → fill ID, name, email, course, semester, section → **Save**.
3. **Courses** → **Add Course** → code, name, department.
4. **Subjects** → **Add Subject** → code, name, course, semester.
   - A subject must reference an existing course.
   - The subject's semester should match the students' semester.
5. **Attendance** → choose course → semester → section → subject → date → **Load Students** → mark Present/Absent → **Save Attendance**.
   - Saving creates the session automatically.
   - To edit an existing session, click **View/Edit** next to it.
   - **Cancel Session** marks it cancelled and excludes it from reports/percentages.
6. **Users** (Admin) → **Add User** → username, full name, email, password, role, enabled.
7. **Settings** (Admin) → set the attendance threshold (e.g. `75`).

### B. Using SQL

```sql
-- Add a course
INSERT INTO courses (code, name, department)
VALUES ('CS303', 'Database Systems', 'Computer Science');

-- Add a subject
INSERT INTO subjects (code, name, course_code, semester)
VALUES ('CS303', 'Database Systems', 'CS303', 'Fall 2026');

-- Add a student
INSERT INTO students (student_id, name, email, course, semester, section)
VALUES ('S1009', 'Ivy Chen', 'ivy.chen@example.edu', 'CS101', 'Fall 2026', 'A');

-- Update a student's section
UPDATE students SET section = 'B' WHERE student_id = 'S1009';

-- Update the attendance threshold
INSERT INTO settings (setting_key, setting_value) VALUES ('attendance.threshold', '80')
ON DUPLICATE KEY UPDATE setting_value = '80';

-- Create an attendance session (today)
INSERT INTO attendance_sessions (course_code, subject_code, semester, section, session_date, created_by, status)
VALUES ('CS101', 'CS101', 'Fall 2026', 'A', CURDATE(), 'admin', 'active');

-- Mark attendance (use the session id returned above)
INSERT INTO attendance_records (session_id, student_id, student_name, status, updated_by)
VALUES
  (@session_id, 'S1001', 'Alice Johnson', 'PRESENT', 'admin'),
  (@session_id, 'S1002', 'Bob Smith', 'ABSENT', 'admin');

-- Create an admin user (BCrypt hash must be generated by the app; use the UI instead)
-- The UI / API is the safe way to create users.
```

> **Important:** Never insert a plain-text password into `users.password`. Use the **Users** page or the API so the password is BCrypt-hashed.

### C. Using the REST API

All API calls need the session cookie and, for unsafe methods, the CSRF token. For a quick demo you can log in through the browser and use the same session in a REST client.

Example: add a student with `curl` (Windows PowerShell):

```powershell
# 1. Get CSRF token (after logging in, reuse the JSESSIONID cookie)
$csrf = (Invoke-RestMethod -Uri http://localhost:8080/api/auth/csrf-token -WebSession $session).data

# 2. Create student
$body = @{
  studentId = 'S1009'
  name      = 'Ivy Chen'
  email     = 'ivy.chen@example.edu'
  course    = 'CS101'
  semester  = 'Fall 2026'
  section   = 'A'
} | ConvertTo-Json

Invoke-RestMethod -Uri http://localhost:8080/api/students -Method Post `
  -ContentType 'application/json' -Body $body -WebSession $session `
  -Headers @{ $csrf.header = $csrf.token }
```

> In a browser-based demo, the easiest path is to use the UI. The API is mainly for integration/testing.

### D. Bulk import

For many students, generate `INSERT` statements from a CSV in Excel and run them in MySQL Workbench / CLI. Always back up first.

---

## 12. Using the application (demo walkthrough)

A 5-minute demo flow:

1. **Login** – `admin / admin123`.
2. **Dashboard** – shows total students, courses, subjects, sessions, overall attendance %, and students below threshold.
3. **Students** – search `Alice`, filter by course/semester/section. Add a new student.
4. **Courses / Subjects** – show the catalog. Add a course and a subject linked to it.
5. **Attendance** – select `CS101` → `Fall 2026` → `A` → `CS101` → today → **Load Students**. Mark one student Absent, save. Reopen the session and show the saved marks.
6. **Reports** – generate a student-wise report; click **Refresh Defaulters**; export CSV.
7. **Users** – create a faculty user; log out and log in as that user to show the reduced menu (no Users/Settings, no edit/delete buttons).
8. **Settings** – change the threshold to `80` and show defaulters recalculate.

> A printable short guide is available at **`docs/Attendance-System-Demo-Guide.pdf`**.

---

## 13. REST API reference

Base URL: `http://localhost:8080/api`

| Method | Endpoint | Role | Purpose |
|---|---|---|---|
| GET | `/auth/csrf-token` | public | Get CSRF header/token |
| GET | `/auth/me` | authenticated | Current user info |
| GET | `/dashboard/stats` | ADMIN, FACULTY | Dashboard statistics |
| GET | `/dashboard/recent-activity` | ADMIN, FACULTY | Recent sessions |
| GET/POST | `/students` | GET: all; POST: ADMIN | List/create students |
| GET/PUT/DELETE | `/students/{id}` | GET: all; write: ADMIN | Read/update/delete a student |
| GET/POST | `/courses` | GET: all; POST: ADMIN | List/create courses |
| GET/PUT/DELETE | `/courses/{id}` | GET: all; write: ADMIN | Read/update/delete a course |
| GET/POST | `/subjects` | GET: all; POST: ADMIN | List/create subjects |
| GET/PUT/DELETE | `/subjects/{id}` | GET: all; write: ADMIN | Read/update/delete a subject |
| GET/POST | `/attendance/sessions` | ADMIN, FACULTY | List/create sessions |
| GET | `/attendance/sessions/{id}` | ADMIN, FACULTY | Session detail |
| GET | `/attendance/sessions/{id}/records` | ADMIN, FACULTY | Records for a session |
| POST | `/attendance/sessions/{id}/marks` | ADMIN, FACULTY | Save marks |
| POST | `/attendance/sessions/{id}/cancel` | ADMIN, FACULTY | Cancel session |
| GET | `/reports/student-wise` | ADMIN, FACULTY | Student report |
| GET | `/reports/subject-wise` | ADMIN, FACULTY | Subject report |
| GET | `/reports/sessions` | ADMIN, FACULTY | Sessions in date range |
| GET | `/reports/defaulters` | ADMIN, FACULTY | Students below threshold |
| GET | `/reports/export-csv` | ADMIN, FACULTY | CSV export |
| GET/POST/PUT/DELETE | `/users` `/users/{id}` | ADMIN | User management |
| POST | `/users/{id}/password` | ADMIN or self | Change password |
| GET | `/users/roles` | ADMIN | List roles |
| GET/POST | `/settings/attendance-threshold` | GET: all; POST: ADMIN | Attendance threshold |

All responses use a consistent envelope:

```json
{
  "success": true,
  "message": null,
  "data": { },
  "timestamp": "2026-11-17T12:00:00"
}
```

Errors return the same envelope with `success: false` and a human-readable `message`. Validation errors include a `data` object with field → message.

---

## 14. Tests

Run the automated test suite:

```powershell
# from the project directory
mvn test
```

or let the setup script do it:

```powershell
powershell -ExecutionPolicy Bypass -File .\Setup-AttendanceApp.ps1
```

Tests included:

- **AttendanceServiceTest** – session creation, duplicate session detection, marking attendance, defaulters calculation.
- **AuthSecurityTest** – unauthenticated access is blocked, admin/faculty access rules, method security.
- **CourseServiceTest** – course CRUD and safe delete.
- **StudentServiceTest** – student CRUD, duplicate ID handling, attendance-record cleanup on delete.

> Tests use the `attendance_db_test` database. Do not point them at production data.

---

## 15. Manual installation

If you prefer to install by hand:

### 15.1 Prerequisites

- JDK 17 (Amazon Corretto 17 recommended)
- Maven 3.10+
- MySQL 8.x
- Git (optional)

### 15.2 Database

```sql
CREATE DATABASE attendance_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'attendance_app'@'localhost' IDENTIFIED BY 'ChooseAStrongPassword';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, REFERENCES, INDEX, ALTER,
      LOCK TABLES, CREATE TEMPORARY TABLES, EXECUTE, TRIGGER
  ON attendance_db.* TO 'attendance_app'@'localhost';
FLUSH PRIVILEGES;
```

### 15.3 Configure

```powershell
Copy-Item .env.example .env
notepad .env
```

Set `ATTENDANCE_DB_USER`, `ATTENDANCE_DB_PASS`, `ATTENDANCE_DB_URL`.

### 15.4 Schema + seed

```powershell
mysql -u root -p attendance_db < database\schema.sql
mysql -u root -p attendance_db < database\seed.sql
```

### 15.5 Build & run

```powershell
mvn clean package
java -jar target\attendance-management-system-1.0.0.jar
```

Open **http://localhost:8080**.

---

## 16. Troubleshooting

| Problem | Fix |
|---|---|
| `Access denied for user 'attendance_app'` | `.env` password does not match MySQL. Re-run setup or update both. |
| `Port 8080 is already in use` | Another app is using 8080. Change `SERVER_PORT` in `.env` or stop the other process. |
| `Port 3306 is already in use` | The script uses the existing MySQL. If root has a password, enter it when prompted. |
| `Java not found` | Install Corretto 17 or run `Run-Setup.bat` again. |
| `Maven build failed` | Check `app.out.log` / Maven output. Ensure `JAVA_HOME` points to JDK 17. |
| Login fails for `faculty` | The existing DB may have an old password. Reset it from **Users** (admin) or delete the users table and restart to let the initializer recreate demo users. |
| Attendance page shows no semesters/sections | Add at least one student for that course/semester. The dropdowns are derived from the student roster. |
| Changes not visible | Hard-refresh the browser (`Ctrl+F5`). Static JS/CSS may be cached. |
| Setup cannot download | Check internet/proxy. The script needs access to `dlcdn.apache.org`, `dev.mysql.com`, `corretto.aws`. |

### Reset demo data (careful!)

```sql
-- WARNING: deletes all attendance history and students
DELETE FROM attendance_records;
DELETE FROM attendance_sessions;
DELETE FROM students;
-- then re-run database/seed.sql
```

Do **not** drop the database unless you intend to lose all data.

---

## 17. Security notes

- Passwords are hashed with **BCrypt** (strength 10). Plain-text passwords are never stored.
- All write endpoints require authentication; Admin-only endpoints use method security (`@PreAuthorize`).
- CSRF protection is enabled for state-changing requests.
- The app uses a **restricted MySQL user** (`attendance_app`) with privileges only on `attendance_db` (and full privileges on the test DB).
- `.env` is git-ignored. Never commit real passwords.
- User passwords are excluded from JSON responses (`@JsonIgnore`).
- Session idle timeout: 30 minutes.
- For production, put the app behind HTTPS (reverse proxy), set a strong DB password, disable demo users, and configure SMTP secrets outside the repository.

---

## 18. Updating the project later

### Pull new code

```powershell
git pull
powershell -ExecutionPolicy Bypass -File .\Setup-AttendanceApp.ps1
```

The setup script is idempotent: it keeps your data, rebuilds, runs tests, and restarts the app.

### Apply database changes

1. Put new schema changes in `database/schema.sql` (use `CREATE TABLE IF NOT EXISTS` / `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` where possible).
2. For destructive changes, write a separate migration script and back up first.
3. Run:

```powershell
mysql -u root -p attendance_db < database\schema.sql
```

### Add a new page / feature

- Backend: controller + service + repository + DTO.
- Frontend: Thymeleaf template + JS init function in `static/js/app.js`.
- Add tests in `src/test/java`.
- Rebuild and restart.

---

## 19. Backup and restore

### Backup

```powershell
mysqldump -u root -p --databases attendance_db > attendance_db_backup.sql
```

### Restore

```powershell
mysql -u root -p < attendance_db_backup.sql
```

For regular backups, schedule `mysqldump` with Task Scheduler.

---

## 20. License

This project is provided as-is for educational/demo use. Review the licenses of the dependencies (Spring Boot, MySQL, etc.) before production use.

---

**Default URL:** http://localhost:8080  
**Admin:** `admin` / `admin123`  
**Faculty:** `faculty` / `faculty123`  
**Stop:** `Stop-AttendanceApp.bat`  
**Setup log:** `setup.log`