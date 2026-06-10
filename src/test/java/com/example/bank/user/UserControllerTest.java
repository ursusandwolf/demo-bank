package com.example.bank.user;

import com.example.bank.user.dto.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password(passwordEncoder.encode("password"))
                .firstName("Test")
                .lastName("User")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.USER))
                .createdAt(Instant.now())
                .build();
        userRepository.save(user);
    }

    @Test
    void shouldReturn401WhenAnonymous() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @org.springframework.security.test.context.support.WithUserDetails(value = "test@example.com", setupBefore = org.springframework.security.test.context.support.TestExecutionEvent.TEST_EXECUTION)
    void shouldReturnUserProfileWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("test@example.com")))
                .andExpect(jsonPath("$.firstName", is("Test")))
                .andExpect(jsonPath("$.lastName", is("User")));
    }

    @Test
    @org.springframework.security.test.context.support.WithUserDetails(value = "test@example.com", setupBefore = org.springframework.security.test.context.support.TestExecutionEvent.TEST_EXECUTION)
    void shouldReturnUserCountWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/users/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(1)));
    }

    @Test
    void shouldReturn401ForCountWhenAnonymous() throws Exception {
        mockMvc.perform(get("/api/users/count"))
                .andExpect(status().isUnauthorized());
    }
}
