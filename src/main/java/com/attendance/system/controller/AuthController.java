package com.attendance.system.controller;

import com.attendance.system.dto.ApiResponse;
import com.attendance.system.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/csrf-token")
    public ApiResponse<Map<String, String>> csrfToken(CsrfToken csrfToken) {
        Map<String, String> data = new HashMap<>();
        data.put("header", csrfToken.getHeaderName());
        data.put("token", csrfToken.getToken());
        data.put("parameter", csrfToken.getParameterName());
        return ApiResponse.ok(data);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(@AuthenticationPrincipal CustomUserDetails user) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("fullName", user.getFullName());
        data.put("email", user.getEmail());
        data.put("role", user.getRole() != null ? user.getRole().name() : null);
        return ApiResponse.ok(data);
    }
}
