package com.attendance.system.repository;

import com.attendance.system.model.NotificationHistory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class NotificationHistoryRepository {

    private final JdbcTemplate jdbc;

    public NotificationHistoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<NotificationHistory> MAPPER = (rs, rowNum) -> new NotificationHistory(
        rs.getLong("id"),
        rs.getString("type"),
        rs.getString("recipient"),
        rs.getString("subject"),
        rs.getString("status"),
        rs.getString("sent_at"),
        rs.getString("error_message")
    );

    public List<NotificationHistory> findAll() {
        String sql = "SELECT id, type, recipient, subject, status, sent_at, error_message FROM notification_history ORDER BY sent_at DESC";
        return jdbc.query(sql, MAPPER);
    }

    public Long save(String type, String recipient, String subject, String status, String errorMessage) {
        String sql = "INSERT INTO notification_history (type, recipient, subject, status, sent_at, error_message) VALUES (?, ?, ?, ?, NOW(), ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, type);
            ps.setString(2, recipient);
            ps.setString(3, subject);
            ps.setString(4, status);
            ps.setString(5, errorMessage);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }
}
