-- ============================================================================
-- Attendance Management System - Demo Seed Data
-- ============================================================================
-- All names, emails, and IDs below are FICTIONAL and for demonstration only.
-- Demo users (admin / faculty) are created by the setup script via the app API
-- so their passwords are hashed with BCrypt by the application.
-- ============================================================================

-- Courses (fictional)
INSERT IGNORE INTO courses (code, name, department) VALUES
('CS101', 'Introduction to Programming', 'Computer Science'),
('CS202', 'Data Structures and Algorithms', 'Computer Science'),
('EE101', 'Electrical Circuits', 'Electrical Engineering'),
('MA101', 'Calculus I', 'Mathematics');

-- Subjects (fictional), linked to courses and semesters
INSERT IGNORE INTO subjects (code, name, course_code, semester) VALUES
('CS101', 'Introduction to Programming', 'CS101', 'Fall 2026'),
('CS202', 'Data Structures and Algorithms', 'CS202', 'Fall 2026'),
('EE101', 'Electrical Circuits', 'EE101', 'Fall 2026'),
('MA101', 'Calculus I', 'MA101', 'Fall 2026');

-- Students (fictional)
INSERT IGNORE INTO students (student_id, name, email, course, semester, section) VALUES
('S1001', 'Alice Johnson', 'alice.johnson@example.edu', 'CS101', 'Fall 2026', 'A'),
('S1002', 'Bob Smith', 'bob.smith@example.edu', 'CS101', 'Fall 2026', 'A'),
('S1003', 'Carol Davis', 'carol.davis@example.edu', 'CS101', 'Fall 2026', 'B'),
('S1004', 'David Wilson', 'david.wilson@example.edu', 'CS202', 'Fall 2026', 'A'),
('S1005', 'Eva Martinez', 'eva.martinez@example.edu', 'CS202', 'Fall 2026', 'A'),
('S1006', 'Frank Miller', 'frank.miller@example.edu', 'EE101', 'Fall 2026', 'A'),
('S1007', 'Grace Lee', 'grace.lee@example.edu', 'MA101', 'Fall 2026', 'A'),
('S1008', 'Henry Clark', 'henry.clark@example.edu', 'MA101', 'Fall 2026', 'B');
