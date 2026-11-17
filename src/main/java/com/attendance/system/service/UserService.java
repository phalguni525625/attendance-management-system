package com.attendance.system.service;

import com.attendance.system.dto.UserDto;
import com.attendance.system.model.Role;
import com.attendance.system.model.User;
import com.attendance.system.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User create(UserDto dto) {
        if (dto.getPassword() == null || dto.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (dto.getRole() == null) {
            throw new IllegalArgumentException("Role is required");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + dto.getUsername());
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());
        user.setEnabled(true);
        Long id = userRepository.save(user);
        user.setId(id);
        return user;
    }

    @Transactional
    public User update(Long id, UserDto dto) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        if (userRepository.existsByUsernameExcluding(dto.getUsername(), id)) {
            throw new IllegalArgumentException("Username already exists: " + dto.getUsername());
        }
        if (dto.getRole() == null) {
            throw new IllegalArgumentException("Role is required");
        }
        user.setUsername(dto.getUsername());
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());
        user.setEnabled(dto.isEnabled());
        userRepository.update(user);
        return user;
    }

    @Transactional
    public void changePassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        userRepository.updatePassword(userId, passwordEncoder.encode(newPassword));
    }

    @Transactional
    public void delete(Long id) {
        userRepository.delete(id);
    }

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
