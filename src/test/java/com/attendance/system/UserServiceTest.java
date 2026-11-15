package com.attendance.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.attendance.system.dto.UserDto;
import com.attendance.system.model.Role;
import com.attendance.system.model.User;
import com.attendance.system.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = AttendanceManagementApplication.class)
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    private UserDto adminDto;

    @BeforeEach
    void setUp() {
        adminDto = new UserDto();
        adminDto.setUsername("admin");
        adminDto.setPassword("admin123");
        adminDto.setFullName("Admin User");
        adminDto.setEmail("admin@example.edu");
        adminDto.setRole(Role.ADMIN);
        adminDto.setEnabled(true);
    }

    @Test
    @DisplayName("create and find user hashes password")
    void createAndFindUser() {
        User created = userService.create(adminDto);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getPassword()).isNotEqualTo("admin123");
        assertThat(userService.verifyPassword("admin123", created.getPassword())).isTrue();
        assertThat(userService.verifyPassword("wrong", created.getPassword())).isFalse();

        User found = userService.findByUsername("admin").orElseThrow();
        assertThat(found.getRole()).isEqualTo(Role.ADMIN);
        assertThat(found.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("duplicate username rejected")
    void duplicateUsernameRejected() {
        userService.create(adminDto);
        assertThatThrownBy(() -> userService.create(adminDto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("update user")
    void updateUser() {
        User created = userService.create(adminDto);
        UserDto updateDto = new UserDto();
        updateDto.setUsername("admin_updated");
        updateDto.setPassword("ignored");
        updateDto.setFullName("Updated Name");
        updateDto.setEmail("updated@example.edu");
        updateDto.setRole(Role.FACULTY);
        updateDto.setEnabled(true);
        User updated = userService.update(created.getId(), updateDto);
        assertThat(updated.getUsername()).isEqualTo("admin_updated");
        assertThat(updated.getFullName()).isEqualTo("Updated Name");
        assertThat(updated.getRole()).isEqualTo(Role.FACULTY);
    }

    @Test
    @DisplayName("change password")
    void changePassword() {
        User created = userService.create(adminDto);
        userService.changePassword(created.getId(), "newPass123");
        User found = userService.findById(created.getId()).orElseThrow();
        assertThat(userService.verifyPassword("newPass123", found.getPassword())).isTrue();
        assertThat(userService.verifyPassword("admin123", found.getPassword())).isFalse();
    }

    @Test
    @DisplayName("change password too short rejected")
    void changePasswordTooShort() {
        User created = userService.create(adminDto);
        assertThatThrownBy(() -> userService.changePassword(created.getId(), "abc"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("delete user")
    void deleteUser() {
        User created = userService.create(adminDto);
        userService.delete(created.getId());
        assertThat(userService.findById(created.getId())).isEmpty();
    }
}
