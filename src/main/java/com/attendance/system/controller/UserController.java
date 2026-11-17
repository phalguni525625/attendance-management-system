package com.attendance.system.controller;

import com.attendance.system.dto.ApiResponse;
import com.attendance.system.dto.UserDto;
import com.attendance.system.model.Role;
import com.attendance.system.model.User;
import com.attendance.system.security.CustomUserDetails;
import com.attendance.system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<User>> list(@AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<User> get(@PathVariable Long id) {
        return userService.findById(id)
            .map(ApiResponse::ok)
            .orElseGet(() -> ApiResponse.<User>error("User not found"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<User> create(@Valid @RequestBody UserDto dto,
                                    @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.ok("User created", userService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<User> update(@PathVariable Long id, @Valid @RequestBody UserDto dto) {
        return ApiResponse.ok("User updated", userService.update(id, dto));
    }

    @PostMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ApiResponse<Void> changePassword(@PathVariable Long id,
                                            @RequestBody Map<String, String> body,
                                            @AuthenticationPrincipal CustomUserDetails user) {
        String newPassword = body.get("newPassword");
        userService.changePassword(id, newPassword);
        return ApiResponse.ok("Password changed", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal CustomUserDetails user) {
        if (user.getId().equals(id)) {
            return ApiResponse.error("You cannot delete your own account");
        }
        userService.delete(id);
        return ApiResponse.ok("User deleted", null);
    }

    public record RoleOption(String value, String label) {}

    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ApiResponse<List<RoleOption>> roles() {
        List<RoleOption> options = List.of(
            new RoleOption("ADMIN", "Administrator"),
            new RoleOption("FACULTY", "Faculty")
        );
        return ApiResponse.ok(options);
    }
}
