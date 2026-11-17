package com.attendance.system;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.attendance.system.model.User;
import com.attendance.system.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AttendanceManagementApplication.class)
@AutoConfigureMockMvc
class AuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("csrf token endpoint is public")
    void csrfPublic() throws Exception {
        mockMvc.perform(get("/api/auth/csrf-token")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("protected users endpoint requires authentication")
    void usersRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("faculty can access protected endpoint")
    void facultyCanAccess() throws Exception {
        mockMvc.perform(get("/api/students")
            .with(SecurityMockMvcRequestPostProcessors.user("faculty").roles("FACULTY")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("faculty cannot list users")
    void facultyCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/users")
            .with(SecurityMockMvcRequestPostProcessors.user("faculty").roles("FACULTY")))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin can access admin endpoint")
    void adminCanAccessAdmin() throws Exception {
        mockMvc.perform(get("/api/settings/attendance-threshold")
            .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
            .andExpect(status().isOk());
    }
}
