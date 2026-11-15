package com.attendance.system.repository;

import com.attendance.system.model.Setting;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SettingRepository {

    private final JdbcTemplate jdbc;

    public SettingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Setting> MAPPER = (rs, rowNum) -> new Setting(
        rs.getString("setting_key"),
        rs.getString("setting_value"),
        rs.getString("updated_at")
    );

    public List<Setting> findAll() {
        String sql = "SELECT setting_key, setting_value, updated_at FROM settings ORDER BY setting_key";
        return jdbc.query(sql, MAPPER);
    }

    public Optional<Setting> findByKey(String key) {
        String sql = "SELECT setting_key, setting_value, updated_at FROM settings WHERE setting_key = ?";
        try {
            return Optional.of(jdbc.queryForObject(sql, MAPPER, key));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public int upsert(String key, String value) {
        String sql = "INSERT INTO settings (setting_key, setting_value, updated_at) VALUES (?, ?, NOW()) " +
            "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = NOW()";
        return jdbc.update(sql, key, value);
    }
}
