package com.attendance.system.repository;

import com.attendance.system.model.Student;
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
public class StudentRepository {

    private final JdbcTemplate jdbc;

    public StudentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Student> MAPPER = (rs, rowNum) -> new Student(
        rs.getLong("id"),
        rs.getString("student_id"),
        rs.getString("name"),
        rs.getString("email"),
        rs.getString("course"),
        rs.getString("semester"),
        rs.getString("section"),
        rs.getString("created_at"),
        rs.getString("updated_at")
    );

    public List<Student> findAll() {
        String sql = "SELECT id, student_id, name, email, course, semester, section, created_at, updated_at FROM students ORDER BY name";
        return jdbc.query(sql, MAPPER);
    }

    public Optional<Student> findById(Long id) {
        String sql = "SELECT id, student_id, name, email, course, semester, section, created_at, updated_at FROM students WHERE id = ?";
        try {
            return Optional.of(jdbc.queryForObject(sql, MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Student> findByStudentId(String studentId) {
        String sql = "SELECT id, student_id, name, email, course, semester, section, created_at, updated_at FROM students WHERE student_id = ?";
        try {
            return Optional.of(jdbc.queryForObject(sql, MAPPER, studentId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existsByStudentId(String studentId) {
        String sql = "SELECT COUNT(1) FROM students WHERE student_id = ?";
        return jdbc.queryForObject(sql, Integer.class, studentId) > 0;
    }

    public boolean existsByStudentIdExcluding(String studentId, Long excludeId) {
        String sql = "SELECT COUNT(1) FROM students WHERE student_id = ? AND id <> ?";
        return jdbc.queryForObject(sql, Integer.class, studentId, excludeId) > 0;
    }

    public Long save(Student student) {
        String sql = "INSERT INTO students (student_id, name, email, course, semester, section, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, student.getStudentId());
            ps.setString(2, student.getName());
            ps.setString(3, student.getEmail());
            ps.setString(4, student.getCourse());
            ps.setString(5, student.getSemester());
            ps.setString(6, student.getSection());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    public int update(Student student) {
        String sql = "UPDATE students SET student_id = ?, name = ?, email = ?, course = ?, semester = ?, section = ?, updated_at = NOW() WHERE id = ?";
        return jdbc.update(sql, student.getStudentId(), student.getName(), student.getEmail(),
            student.getCourse(), student.getSemester(), student.getSection(), student.getId());
    }

    public int delete(Long id) {
        String sql = "DELETE FROM students WHERE id = ?";
        return jdbc.update(sql, id);
    }
}
