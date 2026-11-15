# Attendance Management System

A complete, local-first college Attendance Management System built from scratch with **Java 17**, **Spring Boot 3.3.5**, **Maven**, **MySQL 8.x**, **Spring Security**, **Spring JDBC**, **Thymeleaf**, and **vanilla JavaScript**. Runs entirely on a Windows PC with no paid services or cloud dependencies.

## Features

- **Authentication & authorization** — login/logout, BCrypt password hashing, session management, role-based access control (Administrator / Faculty), change password.
- **Dashboard** — real database-driven stats: total students, courses/subjects, attendance sessions, overall attendance %, students below threshold, recent activity.
- **Student management** — add/edit/view/delete, search & filter, duplicate student-ID prevention, backend + frontend validation, safe handling of related attendance records on deletion.
- **Course & subject management** — CRUD for courses and subjects, semesters, links between subjects/courses/students, duplicate relationship prevention.
- **Attendance management** — create sessions (course, subject, semester, section, date), mark Present/Absent per student, edit attendance, duplicate-session/duplicate-entry prevention, attendance history by student/subject/date, bulk-save confirmation.
- **Attendance calculations** — `Present / Total eligible × 100`, zero-session safety, per-subject and overall percentages, configurable minimum threshold (admin), defaulter identification, shortage calculation, boundary cases (exactly 75%, below 75%, zero sessions), cancelled sessions excluded from active calculations.
- **Reports & exports** — student-wise, subject-wise, date-range, course/semester filters, defaulter reports, percentage summaries, CSV export (UTF-8 BOM, opens correctly in spreadsheet software).
- **Notifications** — optional email notifications for defaulters via SMTP read from environment variables; clear message when SMTP not configured; no false "sent" claims; notification history to avoid duplicates.

## Technology Stack

| Layer | Technology |
|-------|------------|
| Language | Java 17 (LTS) |
| Framework | Spring Boot 3.3.5 |
| Build | Maven 3.10.1 |
| Database | MySQL 8.x |
| Security | Spring Security (BCrypt, sessions, CSRF) |
| Data access | Spring JDBC (JdbcTemplate) |
| Templates | Thymeleaf |
| Frontend | HTML5, CSS3, vanilla JavaScript |
| Mail | Spring Mail (optional SMTP) |
| Testing | JUnit 5, Spring Boot Test, Spring Security Test |

## Directory Structure

```
attendance-management-system/
├── pom.xml
├── README.md
├── .env.example
├── .gitignore
├── Run-Setup.bat
├── Setup-AttendanceApp.ps1
├── Run-AttendanceApp.bat
├── Stop-AttendanceApp.bat
├── database/
│   ├── schema.sql          # normalized MySQL schema (idempotent, non-destructive)
│   └── seed.sql            # fictional demo data
├── docs/
│   └── architecture.md
├── src/
│   ├── main/java/com/attendance/system/
│   │   ├── AttendanceManagementApplication.java
│   │   ├── config/         # SecurityConfig
│   │   ├── controller/     # MVC + REST API controllers
│   │   ├── dto/            # request/response DTOs
│   │   ├── exception/      # GlobalExceptionHandler
│   │   ├── model/          # domain POJOs
│   │   ├── repository/     # Spring JDBC repositories
│   │   ├── security/       # CustomUserDetails, UserDetailsService
│   │   ├── service/        # business logic + calculations
│   │   └── util/
│   └── main/resources/
│       ├── application.properties
│       ├── db/migration/   # version-controlled migrations (V1__, V2__)
│       ├── templates/      # Thymeleaf pages
│       └── static/
│           ├── css/style.css
│           └── js/app.js
└── src/test/java/com/attendance/system/   # JUnit 5 tests
```

## Prerequisites

- Windows 10/11
- Internet connection (for one-time prerequisite download)
- ~5 GB free disk space
- Administrator privileges are required **only** to install MySQL server

The setup script installs (or detects) Java 17, Maven, MySQL 8.x, and Git.

## One-click Installation

1. Open a PowerShell or Command Prompt in the project folder.
2. Run:

   ```
   Run-Setup.bat
   ```

   or, from PowerShell:

   ```
   .\Setup-AttendanceApp.ps1
   ```

3. The script will:
   - Detect Windows version and admin privileges
   - Check internet and disk space
   - Install missing Java 17, Maven, Git (via winget or direct download)
   - Attempt MySQL install (prompts if admin action is required)
   - Prompt securely for database credentials (never logged)
   - Create the `attendance_db` database and a restricted `attendance_app` user
   - Initialize the schema safely (no existing data is destroyed)
   - Build with Maven and run automated tests
   - Start the app and open `http://localhost:8080` in your browser

4. Log in with demo credentials (see below).

## Manual Installation Fallback

If the setup script cannot install a component (e.g. no admin rights for MySQL):

1. Install Java 17 LTS (Amazon Corretto or Eclipse Temurin) and set `JAVA_HOME`.
2. Install Maven 3.x and set `MAVEN_HOME`. Add both `bin` directories to `PATH`.
3. Install MySQL 8.x and start it on port 3306.
4. Copy `.env.example` to `.env` and set `ATTENDANCE_DB_USER`, `ATTENDANCE_DB_PASS`, and `ATTENDANCE_DB_URL`.
5. Create the database and user:

   ```sql
   CREATE DATABASE attendance_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'attendance_app'@'localhost' IDENTIFIED BY '<password>';
   GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, REFERENCES, INDEX, ALTER,
           LOCK TABLES, CREATE TEMPORARY TABLES, EXECUTE, TRIGGER
           ON attendance_db.* TO 'attendance_app'@'localhost';
   FLUSH PRIVILEGES;
   ```

6. Initialize the schema:

   ```
   mysql -u attendance_app -p<password> attendance_db < database\schema.sql
   mysql -u attendance_app -p<password> attendance_db < database\seed.sql   # optional demo data
   ```

7. Build and run:

   ```
   mvn -q clean package
   java -jar attendance-management-system-1.0.0.jar
   ```

8. Open `http://localhost:8080`.

## Database Setup & Configuration

- Schema: `database/schema.sql` (idempotent `CREATE ... IF NOT EXISTS`, never drops data).
- Migrations: version-controlled under `src/main/resources/db/migration/` (`V1__schema.sql`, `V2__seed.sql`).
- All DB credentials come from environment variables / `.env` (git-ignored). Never hardcode passwords.
- The application user (`attendance_app`) has restricted privileges on `attendance_db` only.

## Demo Login Credentials

Created automatically during setup (passwords hashed with BCrypt by the application):

| Role | Username | Password |
|------|----------|----------|
| Administrator | `admin` | `admin123` |
| Faculty | `faculty` | `faculty123` |

Change these passwords after first login via **Profile > Change Password**. Demo student/course data is entirely fictional.

## Launch & Stop

- Start: `Run-AttendanceApp.bat` (starts the already-built jar; does not reinstall or reset the DB).
  - URL: `http://localhost:8080`
- Stop: `Stop-AttendanceApp.bat` (stops only the app process launched by this project; does not touch MySQL or unrelated Java processes).

## Running Tests

```
mvn test
```

Tests cover login success/failure, role restrictions, CRUD, attendance marking/editing, duplicate prevention, percentage calculations, threshold/shortcut boundary cases, invalid inputs, report filtering, CSV export, DB constraints/transactions, missing configuration, API error handling, and email behavior with/without SMTP.

## Optional SMTP Configuration

Email notifications for defaulters are sent only when SMTP is configured via environment variables:

- `ATTENDANCE_SMTP_HOST`, `ATTENDANCE_SMTP_PORT`, `ATTENDANCE_SMTP_USER`, `ATTENDANCE_SMTP_PASS`
- `ATTENDANCE_SMTP_AUTH` (true/false), `ATTENDANCE_SMTP_STARTTLS` (true/false)

If SMTP is not configured, the app reports "SMTP is not configured. Email was not sent." and no email is attempted. Credentials are never committed.

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `java` / `mvn` not recognized | Ensure JAVA_HOME and MAVEN_HOME are set and their `bin` folders are in PATH; restart your terminal. |
| MySQL connection refused | Confirm MySQL is running on port 3306 and `ATTENDANCE_DB_*` in `.env` are correct. |
| Build fails | Run `mvn clean package` and read the error; ensure Java 17 is active. |
| Tests fail against MySQL | Tests use a dedicated test configuration; ensure the MySQL instance is reachable. |
| App won't start | Check `app.err.log` and `app.out.log` in the project folder. |
| 403 Forbidden | Your role lacks permission; log in as an administrator for admin-only pages. |

## Database Backup & Restore

Backup:

```
mysqldump -u root -p attendance_db > attendance_db_backup.sql
```

Restore:

```
mysql -u root -p attendance_db < attendance_db_backup.sql
```

Never back up or restore while the application is writing without pausing it first.

## Git Commands for Future Development

```bash
# View status and changes
git status
git diff

# Stage and commit (never commit .env, logs, target/, or credentials)
git add .
git commit -m "feat: describe your change"

# Push only when authorized (this repo has no remote configured by default)
git remote add origin <url>
git push -u origin main
```

Do not force-push or publish without authorization.

## Security Notes

- Passwords are hashed with BCrypt (cost 12); never stored plaintext.
- CSRF protection enabled on state-changing requests.
- All input validated on the backend (Jakarta Validation) and frontend.
- Parameterized queries / Spring JDBC prevent SQL injection.
- Secrets live in `.env` (git-ignored); `.env.example` contains placeholders only.
- No stack traces or credentials are exposed to users.
- Logs never record passwords or sensitive student data.
