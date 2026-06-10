package com.example.bank.security;

import com.example.bank.user.Role;
import com.example.bank.user.User;
import com.example.bank.user.UserRepository;
import com.example.bank.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        
        // Создаем обычного пользователя
        createUser("user@example.com", "password", Role.USER);
        // Создаем админа
        createUser("admin@example.com", "password", Role.ADMIN);
    }

    @Test
    void userShouldAccessPrivateEndpoint() throws Exception {
        mockMvc.perform(get("/api/private/hello").with(httpBasic("user@example.com", "password")))
                .andExpect(status().isOk());
    }

    @Test
    void userShouldNotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(httpBasic("user@example.com", "password")))
                .andExpect(status().isForbidden()); // 403 Forbidden
    }

    @Test
    void adminShouldAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(httpBasic("admin@example.com", "password")))
                .andExpect(status().isOk());
    }

    @Test
    void adminShouldAccessPrivateEndpoint() throws Exception {
        mockMvc.perform(get("/api/private/hello").with(httpBasic("admin@example.com", "password")))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousShouldNotAccessPrivateEndpoint() throws Exception {
        mockMvc.perform(get("/api/private/hello"))
                .andExpect(status().isUnauthorized()); // 401 Unauthorized
    }

    @Test
    void anonymousShouldNotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized()); // 401 Unauthorized
    }

    private void createUser(String email, String password, Role role) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password(passwordEncoder.encode(password))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(role))
                .createdAt(Instant.now())
                .build();
        userRepository.save(user);
    }
}
