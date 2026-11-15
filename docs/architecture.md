# Architecture — Attendance Management System

## Overview
Monolithic Spring Boot 3 application running locally on Windows. Layered architecture:
- **Controllers** — handle HTTP/Thymeleaf requests, validation, delegate to services.
- **Services** — business logic, transactions, attendance calculations.
- **Repositories** — Spring JDBC `JdbcTemplate` with parameterized SQL; one repository per table.
- **Security** — Spring Security filter chain, BCrypt password hashing, session management, CSRF, role-based method security (`@PreAuthorize`).
- **Frontend** — Thymeleaf-rendered pages + vanilla JS calling JSON API endpoints.

## Database Schema (normalized, MySQL 8)
- `users` — accounts with username, hashed password, role (ADMIN/FACULTY), enabled flag.
- `students` — student_id (unique), name, email, course, semester, section.
- `courses` — code (unique), name, department.
- `subjects` — code (unique), name, course_code FK, semester.
- `attendance_sessions` — one row per class session; unique(course_code, subject_code, semester, section, session_date) prevents duplicate sessions.
- `attendance_records` — unique(session_id, student_id) prevents duplicate attendance entries; FK to session (cascade delete) and student.
- `settings` — key/value, e.g. attendance threshold.
- `notification_history` — audit of email notification attempts.

## Security Model
- `CustomUserDetailsService` loads users by username; disabled users cannot log in.
- `BCryptPasswordEncoder` (cost 12) hashes passwords.
- CSRF via `CookieCsrfTokenRepository` (XSRF-TOKEN cookie) + `XorCsrfTokenRequestAttributeHandler`.
- `@PreAuthorize("hasRole('ADMIN')")` for write/user-management endpoints; `hasAnyRole('ADMIN','FACULTY')` for read/attendance endpoints.

## Attendance Calculation
`percentage = present_sessions / total_eligible_sessions × 100`. Zero total sessions ⇒ 0% (no division by zero). Shortage = ceil(threshold% × total) − present, floored at 0. Cancelled sessions are excluded from active calculations. Threshold configurable by admin (default 75%).

## Configuration
`application.properties` reads all secrets from environment variables with safe defaults. `.env` is git-ignored.
