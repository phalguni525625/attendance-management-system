package com.attendance.system.repository;

import com.attendance.system.model.Subject;
import org.springframework.dao.EmptyResultDataAccessException;
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
public class SubjectRepository {

    private final JdbcTemplate jdbc;

    public SubjectRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Subject> MAPPER = (rs, rowNum) -> new Subject(
        rs.getLong("id"),
        rs.getString("code"),
        rs.getString("name"),
        rs.getString("course_code"),
        rs.getString("semester"),
        rs.getString("created_at"),
        rs.getString("updated_at")
    );

    public List<Subject> findAll() {
        String sql = "SELECT id, code, name, course_code, semester, created_at, updated_at FROM subjects ORDER BY code";
        return jdbc.query(sql, MAPPER);
    }

    public List<Subject> findByCourseAndSemester(String courseCode, String semester) {
        String sql = "SELECT id, code, name, course_code, semester, created_at, updated_at FROM subjects WHERE course_code = ? AND semester = ? ORDER BY code";
        return jdbc.query(sql, MAPPER, courseCode, semester);
    }

    public Optional<Subject> findById(Long id) {
        String sql = "SELECT id, code, name, course_code, semester, created_at, updated_at FROM subjects WHERE id = ?";
        try {
            return Optional.of(jdbc.queryForObject(sql, MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Subject> findByCode(String code) {
        String sql = "SELECT id, code, name, course_code, semester, created_at, updated_at FROM subjects WHERE code = ?";
        try {
            return Optional.of(jdbc.queryForObject(sql, MAPPER, code));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existsByCode(String code) {
        String sql = "SELECT COUNT(1) FROM subjects WHERE code = ?";
        return jdbc.queryForObject(sql, Integer.class, code) > 0;
    }

    public boolean existsByCodeExcluding(String code, Long excludeId) {
        String sql = "SELECT COUNT(1) FROM subjects WHERE code = ? AND id <> ?";
        return jdbc.queryForObject(sql, Integer.class, code, excludeId) > 0;
    }

    public boolean existsByCourseCode(String courseCode) {
        String sql = "SELECT COUNT(1) FROM subjects WHERE course_code = ?";
        return jdbc.queryForObject(sql, Integer.class, courseCode) > 0;
    }

    public Long save(Subject subject) {
        String sql = "INSERT INTO subjects (code, name, course_code, semester, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, subject.getCode());
            ps.setString(2, subject.getName());
            ps.setString(3, subject.getCourseCode());
            ps.setString(4, subject.getSemester());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    public int update(Subject subject) {
        String sql = "UPDATE subjects SET code = ?, name = ?, course_code = ?, semester = ?, updated_at = NOW() WHERE id = ?";
        return jdbc.update(sql, subject.getCode(), subject.getName(), subject.getCourseCode(), subject.getSemester(), subject.getId());
    }

    public int delete(Long id) {
        String sql = "DELETE FROM subjects WHERE id = ?";
        return jdbc.update(sql, id);
    }
}
