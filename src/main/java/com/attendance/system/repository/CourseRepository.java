package com.attendance.system.repository;

import com.attendance.system.model.Course;
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
public class CourseRepository {

    private final JdbcTemplate jdbc;

    public CourseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Course> MAPPER = (rs, rowNum) -> new Course(
        rs.getLong("id"),
        rs.getString("code"),
        rs.getString("name"),
        rs.getString("department"),
        rs.getString("created_at"),
        rs.getString("updated_at")
    );

    public List<Course> findAll() {
        String sql = "SELECT id, code, name, department, created_at, updated_at FROM courses ORDER BY code";
        return jdbc.query(sql, MAPPER);
    }

    public Optional<Course> findById(Long id) {
        String sql = "SELECT id, code, name, department, created_at, updated_at FROM courses WHERE id = ?";
        try {
            return Optional.of(jdbc.queryForObject(sql, MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Course> findByCode(String code) {
        String sql = "SELECT id, code, name, department, created_at, updated_at FROM courses WHERE code = ?";
        try {
            return Optional.of(jdbc.queryForObject(sql, MAPPER, code));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existsByCode(String code) {
        String sql = "SELECT COUNT(1) FROM courses WHERE code = ?";
        return jdbc.queryForObject(sql, Integer.class, code) > 0;
    }

    public boolean existsByCodeExcluding(String code, Long excludeId) {
        String sql = "SELECT COUNT(1) FROM courses WHERE code = ? AND id <> ?";
        return jdbc.queryForObject(sql, Integer.class, code, excludeId) > 0;
    }

    public Long save(Course course) {
        String sql = "INSERT INTO courses (code, name, department, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, course.getCode());
            ps.setString(2, course.getName());
            ps.setString(3, course.getDepartment());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    public int update(Course course) {
        String sql = "UPDATE courses SET code = ?, name = ?, department = ?, updated_at = NOW() WHERE id = ?";
        return jdbc.update(sql, course.getCode(), course.getName(), course.getDepartment(), course.getId());
    }

    public int delete(Long id) {
        String sql = "DELETE FROM courses WHERE id = ?";
        return jdbc.update(sql, id);
    }
}
