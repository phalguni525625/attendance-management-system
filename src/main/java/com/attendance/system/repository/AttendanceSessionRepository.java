package com.attendance.system.repository;

import com.attendance.system.model.AttendanceSession;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AttendanceSessionRepository {

    private final JdbcTemplate jdbc;

    public AttendanceSessionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<AttendanceSession> MAPPER = (rs, rowNum) -> new AttendanceSession(
        rs.getLong("id"),
        rs.getString("course_code"),
        rs.getString("subject_code"),
        rs.getString("semester"),
        rs.getString("section"),
        rs.getString("session_date"),
        rs.getString("created_by"),
        rs.getString("created_at"),
        rs.getString("status")
    );

    public List<AttendanceSession> findAll() {
        String sql = "SELECT id, course_code, subject_code, semester, section, session_date, created_by, created_at, status FROM attendance_sessions ORDER BY session_date DESC, id DESC";
        return jdbc.query(sql, MAPPER);
    }

    public List<AttendanceSession> findAllActive() {
        String sql = "SELECT id, course_code, subject_code, semester, section, session_date, created_by, created_at, status "
            + "FROM attendance_sessions WHERE status = 'active' ORDER BY session_date DESC, id DESC";
        return jdbc.query(sql, MAPPER);
    }

    public long countActive() {
        return jdbc.queryForObject("SELECT COUNT(1) FROM attendance_sessions WHERE status = 'active'", Long.class);
    }

    public boolean existsByCourseCode(String courseCode) {
        return jdbc.queryForObject("SELECT COUNT(1) FROM attendance_sessions WHERE course_code = ?", Integer.class, courseCode) > 0;
    }

    public boolean existsBySubjectCode(String subjectCode) {
        return jdbc.queryForObject("SELECT COUNT(1) FROM attendance_sessions WHERE subject_code = ?", Integer.class, subjectCode) > 0;
    }

    public List<AttendanceSession> findByFilters(String courseCode, String subjectCode, String semester, String section, String date) {
        StringBuilder sql = new StringBuilder("SELECT id, course_code, subject_code, semester, section, session_date, created_by, created_at, status FROM attendance_sessions WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (courseCode != null && !courseCode.isBlank()) {
            sql.append(" AND course_code = ?");
            args.add(courseCode);
        }
        if (subjectCode != null && !subjectCode.isBlank()) {
            sql.append(" AND subject_code = ?");
            args.add(subjectCode);
        }
        if (semester != null && !semester.isBlank()) {
            sql.append(" AND semester = ?");
            args.add(semester);
        }
        if (section != null && !section.isBlank()) {
            sql.append(" AND section = ?");
            args.add(section);
        }
        if (date != null && !date.isBlank()) {
            sql.append(" AND session_date = ?");
            args.add(date);
        }
        sql.append(" ORDER BY session_date DESC, id DESC");
        return jdbc.query(sql.toString(), MAPPER, args.toArray());
    }

    public Optional<AttendanceSession> findById(Long id) {
        String sql = "SELECT id, course_code, subject_code, semester, section, session_date, created_by, created_at, status FROM attendance_sessions WHERE id = ?";
        try {
            return Optional.of(jdbc.queryForObject(sql, MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<AttendanceSession> findByCourseSubjectDateSection(String courseCode, String subjectCode, String semester, String section, LocalDate date) {
        String sql = "SELECT id, course_code, subject_code, semester, section, session_date, created_by, created_at, status FROM attendance_sessions WHERE course_code = ? AND subject_code = ? AND semester = ? AND section = ? AND session_date = ?";
        try {
            return Optional.of(jdbc.queryForObject(sql, MAPPER, courseCode, subjectCode, semester, section, date.toString()));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Long save(AttendanceSession session) {
        String sql = "INSERT INTO attendance_sessions (course_code, subject_code, semester, section, session_date, created_by, created_at, status) VALUES (?, ?, ?, ?, ?, ?, NOW(), 'active')";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, session.getCourseCode());
            ps.setString(2, session.getSubjectCode());
            ps.setString(3, session.getSemester());
            ps.setString(4, session.getSection());
            ps.setString(5, session.getSessionDate());
            ps.setString(6, session.getCreatedBy());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    public int cancel(Long id) {
        String sql = "UPDATE attendance_sessions SET status = 'cancelled' WHERE id = ?";
        return jdbc.update(sql, id);
    }
}
