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
class AuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldAuthenticateSuccessfully() throws Exception {
        createUser("active@example.com", "password", UserStatus.ACTIVE, false, false);

        mockMvc.perform(get("/api/private/hello").with(httpBasic("active@example.com", "password")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldFailWhenUserIsBlocked() throws Exception {
        createUser("blocked@example.com", "password", UserStatus.BLOCKED, false, false);

        mockMvc.perform(get("/api/private/hello").with(httpBasic("blocked@example.com", "password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldFailWhenAccountIsExpired() throws Exception {
        createUser("expired@example.com", "password", UserStatus.ACTIVE, true, false);

        mockMvc.perform(get("/api/private/hello").with(httpBasic("expired@example.com", "password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldFailWhenCredentialsAreExpired() throws Exception {
        createUser("pass-expired@example.com", "password", UserStatus.ACTIVE, false, true);

        mockMvc.perform(get("/api/private/hello").with(httpBasic("pass-expired@example.com", "password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldFailWithWrongPassword() throws Exception {
        createUser("user@example.com", "password", UserStatus.ACTIVE, false, false);

        mockMvc.perform(get("/api/private/hello").with(httpBasic("user@example.com", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    private void createUser(String email, String password, UserStatus status, boolean accExpired, boolean credExpired) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password(passwordEncoder.encode(password))
                .status(status)
                .accountExpired(accExpired)
                .credentialsExpired(credExpired)
                .roles(Set.of(Role.USER))
                .createdAt(Instant.now())
                .build();
        userRepository.save(user);
    }
}
