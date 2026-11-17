package com.attendance.system.config;

import com.attendance.system.model.Role;
import com.attendance.system.model.User;
import com.attendance.system.repository.SettingRepository;
import com.attendance.system.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the default administrator/faculty accounts and the default attendance
 * threshold the first time the application starts against an empty database.
 * Existing accounts are never modified, so passwords changed by the user are kept.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final SettingRepository settingRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${attendance.demo.enabled:true}")
    private boolean demoEnabled;

    public DataInitializer(UserRepository userRepository,
                           SettingRepository settingRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.settingRepository = settingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (settingRepository.findByKey("attendance.threshold").isEmpty()) {
            settingRepository.upsert("attendance.threshold", "75");
            log.info("Initialized default attendance threshold (75%)");
        }

        if (!demoEnabled) {
            log.info("Demo account creation is disabled (attendance.demo.enabled=false)");
            return;
        }

        createIfMissing("admin", "admin123", "Administrator", "admin@example.edu", Role.ADMIN);
        createIfMissing("faculty", "faculty123", "Faculty User", "faculty@example.edu", Role.FACULTY);
    }

    private void createIfMissing(String username, String rawPassword, String fullName,
                                 String email, Role role) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(role);
        user.setEnabled(true);
        userRepository.save(user);
        log.info("Created default {} account '{}'", role, username);
    }
}