package com.attendance.system.repository;

import com.attendance.system.model.AttendanceRecord;
import com.attendance.system.model.AttendanceStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class AttendanceRecordRepository {

    private final JdbcTemplate jdbc;

    public AttendanceRecordRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<AttendanceRecord> MAPPER = (rs, rowNum) -> new AttendanceRecord(
        rs.getLong("id"),
        rs.getLong("session_id"),
        rs.getString("student_id"),
        rs.getString("student_name"),
        AttendanceStatus.valueOf(rs.getString("status")),
        rs.getString("updated_by"),
        rs.getString("updated_at")
    );

    public List<AttendanceRecord> findBySession(Long sessionId) {
        String sql = "SELECT id, session_id, student_id, student_name, status, updated_by, updated_at FROM attendance_records WHERE session_id = ? ORDER BY student_id";
        return jdbc.query(sql, MAPPER, sessionId);
    }

    public List<AttendanceRecord> findAll() {
        String sql = "SELECT id, session_id, student_id, student_name, status, updated_by, updated_at FROM attendance_records ORDER BY id";
        return jdbc.query(sql, MAPPER);
    }

    /**
     * Records belonging to non-cancelled sessions only. Used by all attendance
     * percentage/defaulters calculations so cancelled sessions are excluded.
     */
    public List<AttendanceRecord> findAllActive() {
        String sql = "SELECT ar.id, ar.session_id, ar.student_id, ar.student_name, ar.status, ar.updated_by, ar.updated_at "
            + "FROM attendance_records ar JOIN attendance_sessions s ON ar.session_id = s.id "
            + "WHERE s.status = 'active' ORDER BY ar.id";
        return jdbc.query(sql, MAPPER);
    }

    public List<AttendanceRecord> findByStudentId(String studentId) {
        String sql = "SELECT id, session_id, student_id, student_name, status, updated_by, updated_at FROM attendance_records WHERE student_id = ? ORDER BY id";
        return jdbc.query(sql, MAPPER, studentId);
    }

    public long countAll() {
        return jdbc.queryForObject("SELECT COUNT(1) FROM attendance_records", Long.class);
    }

    public long countPresent() {
        return jdbc.queryForObject("SELECT COUNT(1) FROM attendance_records WHERE status = 'PRESENT'", Long.class);
    }

    public Optional<AttendanceRecord> findBySessionAndStudent(Long sessionId, String studentId) {
        String sql = "SELECT id, session_id, student_id, student_name, status, updated_by, updated_at FROM attendance_records WHERE session_id = ? AND student_id = ?";
        try {
            return Optional.of(jdbc.queryForObject(sql, MAPPER, sessionId, studentId));
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public int countBySession(Long sessionId) {
        String sql = "SELECT COUNT(1) FROM attendance_records WHERE session_id = ?";
        return jdbc.queryForObject(sql, Integer.class, sessionId);
    }

    public Long save(AttendanceRecord record) {
        String sql = "INSERT INTO attendance_records (session_id, student_id, student_name, status, updated_by, updated_at) VALUES (?, ?, ?, ?, ?, NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, record.getSessionId());
            ps.setString(2, record.getStudentId());
            ps.setString(3, record.getStudentName());
            ps.setString(4, record.getStatus().name());
            ps.setString(5, record.getUpdatedBy());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    public int update(Long sessionId, String studentId, AttendanceStatus status, String updatedBy) {
        String sql = "UPDATE attendance_records SET status = ?, updated_by = ?, updated_at = NOW() WHERE session_id = ? AND student_id = ?";
        return jdbc.update(sql, status.name(), updatedBy, sessionId, studentId);
    }

    public int deleteBySession(Long sessionId) {
        String sql = "DELETE FROM attendance_records WHERE session_id = ?";
        return jdbc.update(sql, sessionId);
    }

    public int deleteByStudent(String studentId) {
        String sql = "DELETE FROM attendance_records WHERE student_id = ?";
        return jdbc.update(sql, studentId);
    }

    public int updateStudentId(String oldStudentId, String newStudentId) {
        String sql = "UPDATE attendance_records SET student_id = ? WHERE student_id = ?";
        return jdbc.update(sql, newStudentId, oldStudentId);
    }

    public int deleteBySubjectAndDateRange(String subjectCode, String startDate, String endDate) {
        String sql = "DELETE ar FROM attendance_records ar JOIN attendance_sessions s ON ar.session_id = s.id WHERE s.subject_code = ? AND s.session_date BETWEEN ? AND ?";
        return jdbc.update(sql, subjectCode, startDate, endDate);
    }
}
