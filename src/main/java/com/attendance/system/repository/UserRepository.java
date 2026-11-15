package com.attendance.system.repository;

import com.attendance.system.model.Role;
import com.attendance.system.model.User;
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
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<User> USER_MAPPER = (rs, rowNum) -> new User(
        rs.getLong("id"),
        rs.getString("username"),
        rs.getString("password"),
        rs.getString("full_name"),
        rs.getString("email"),
        Role.valueOf(rs.getString("role")),
        rs.getBoolean("enabled"),
        rs.getString("created_at"),
        rs.getString("updated_at")
    );

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password, full_name, email, role, enabled, created_at, updated_at FROM users WHERE username = ?";
        try {
            return Optional.of(jdbc.queryForObject(sql, USER_MAPPER, username));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findById(Long id) {
        String sql = "SELECT id, username, password, full_name, email, role, enabled, created_at, updated_at FROM users WHERE id = ?";
        try {
            return Optional.of(jdbc.queryForObject(sql, USER_MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<User> findAll() {
        String sql = "SELECT id, username, password, full_name, email, role, enabled, created_at, updated_at FROM users ORDER BY username";
        return jdbc.query(sql, USER_MAPPER);
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(1) FROM users WHERE username = ?";
        return jdbc.queryForObject(sql, Integer.class, username) > 0;
    }

    public boolean existsByUsernameExcluding(String username, Long excludeId) {
        String sql = "SELECT COUNT(1) FROM users WHERE username = ? AND id <> ?";
        return jdbc.queryForObject(sql, Integer.class, username, excludeId) > 0;
    }

    public Long save(User user) {
        String sql = "INSERT INTO users (username, password, full_name, email, role, enabled, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getRole().name());
            ps.setBoolean(6, user.isEnabled());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    public int update(User user) {
        String sql = "UPDATE users SET username = ?, full_name = ?, email = ?, role = ?, enabled = ?, updated_at = NOW() WHERE id = ?";
        return jdbc.update(sql, user.getUsername(), user.getFullName(), user.getEmail(),
            user.getRole().name(), user.isEnabled(), user.getId());
    }

    public int updatePassword(Long userId, String encodedPassword) {
        String sql = "UPDATE users SET password = ?, updated_at = NOW() WHERE id = ?";
        return jdbc.update(sql, encodedPassword, userId);
    }

    public int delete(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        return jdbc.update(sql, id);
    }
}
