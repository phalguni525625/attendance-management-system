-- ============================================================================
-- Attendance Management System - MySQL 8.x Schema
-- ============================================================================
-- This script is idempotent-safe: it creates tables only if they do not exist
-- and never drops existing data. Run once during setup.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS attendance_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE attendance_db;

-- -----------------------------------------------------
-- Table: users
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    username       VARCHAR(50)  NOT NULL,
    password       VARCHAR(255) NOT NULL,
    full_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(150) NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    enabled        TINYINT(1)   NOT NULL DEFAULT 1,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: students
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS students (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    student_id     VARCHAR(30)  NOT NULL,
    name           VARCHAR(100) NOT NULL,
    email          VARCHAR(150),
    course         VARCHAR(100),
    semester       VARCHAR(20),
    section        VARCHAR(20),
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_students_student_id UNIQUE (student_id)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: courses
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS courses (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    code           VARCHAR(20)  NOT NULL,
    name           VARCHAR(100) NOT NULL,
    department     VARCHAR(100),
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_courses_code UNIQUE (code)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: subjects
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS subjects (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    code           VARCHAR(20)  NOT NULL,
    name           VARCHAR(100) NOT NULL,
    course_code    VARCHAR(100) NOT NULL,
    semester       VARCHAR(20)  NOT NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_subjects_code UNIQUE (code),
    CONSTRAINT fk_subjects_course_code FOREIGN KEY (course_code) REFERENCES courses(code)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: attendance_sessions
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS attendance_sessions (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    course_code    VARCHAR(20)  NOT NULL,
    subject_code   VARCHAR(20)  NOT NULL,
    semester       VARCHAR(20)  NOT NULL,
    section        VARCHAR(20)  NOT NULL,
    session_date   DATE         NOT NULL,
    created_by     VARCHAR(100) NOT NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status         VARCHAR(20)  NOT NULL DEFAULT 'active',
    PRIMARY KEY (id),
    CONSTRAINT fk_sessions_course_code FOREIGN KEY (course_code) REFERENCES courses(code),
    CONSTRAINT fk_sessions_subject_code FOREIGN KEY (subject_code) REFERENCES subjects(code),
    CONSTRAINT uk_session_unique UNIQUE (course_code, subject_code, semester, section, session_date)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: attendance_records
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS attendance_records (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    session_id     BIGINT       NOT NULL,
    student_id     VARCHAR(30)  NOT NULL,
    student_name   VARCHAR(100) NOT NULL,
    status         VARCHAR(10)  NOT NULL,
    updated_by     VARCHAR(100),
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_records_session FOREIGN KEY (session_id) REFERENCES attendance_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_records_student FOREIGN KEY (student_id) REFERENCES students(student_id),
    CONSTRAINT uk_record_session_student UNIQUE (session_id, student_id)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: settings
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS settings (
    setting_key    VARCHAR(100) NOT NULL,
    setting_value  VARCHAR(500) NOT NULL,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (setting_key)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: notification_history
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_history (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    type           VARCHAR(50)  NOT NULL,
    recipient      VARCHAR(150) NOT NULL,
    subject        VARCHAR(200) NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    sent_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    error_message  TEXT,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Default application settings
-- -----------------------------------------------------
INSERT IGNORE INTO settings (setting_key, setting_value) VALUES ('attendance.threshold', '75');
