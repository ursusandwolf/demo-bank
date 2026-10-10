package com.example.bank.security;

import com.example.bank.user.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** No test transaction: verify committed revocation and failure auditing after rollback. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityLifecycleTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RefreshTokenRepository refresh;
    @Autowired SecurityAuditRepository audit;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper mapper;
    @Autowired LoginRateLimiter limiter;
    protected User user;
    protected User admin;

    @BeforeEach
    void setup() {
        user = create(Role.USER);
        admin = create(Role.ADMIN);
    }

    @AfterEach
    void cleanup() {
        for (User account : List.of(user, admin)) {
            refresh.findAll().stream().filter(t -> t.getUser().getId().equals(account.getId()))
                    .forEach(refresh::delete);
            users.deleteById(account.getId());
        }
    }

    @Test
    void logoutRevokesEverySessionAndNewLoginWorks() throws Exception {
        JsonNode first = login(user, "password123", 200);
        JsonNode second = login(user, "password123", 200);
        mvc.perform(post("/api/auth/logout").header("Authorization", bearer(first)))
                .andExpect(status().isNoContent());
        assertRevoked(first);
        assertRevoked(second);
        login(user, "password123", 200);
        assertThat(eventsFor(user)).contains(SecurityAuditEvent.Type.LOGOUT);
    }

    @Test
    void passwordChangeRequiresCurrentPasswordThenRevokesTokensAndPersistsHash() throws Exception {
        JsonNode session = login(user, "password123", 200);
        mvc.perform(post("/api/auth/change-password").header("Authorization", bearer(session))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("currentPassword", "wrong", "newPassword", "newPassword123"))))
                .andExpect(status().isUnauthorized());
        assertThat(eventsFor(user)).contains(SecurityAuditEvent.Type.PASSWORD_CHANGE_FAILURE);
        mvc.perform(get("/api/users/me").header("Authorization", bearer(session))).andExpect(status().isOk());
        mvc.perform(post("/api/auth/change-password").header("Authorization", bearer(session))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("currentPassword", "password123", "newPassword", "newPassword123"))))
                .andExpect(status().isNoContent());
        assertRevoked(session);
        login(user, "password123", 401);
        login(user, "newPassword123", 200);
        assertThat(encoder.matches("newPassword123", users.findById(user.getId()).orElseThrow().getPassword())).isTrue();
        assertThat(eventsFor(user)).contains(SecurityAuditEvent.Type.PASSWORD_CHANGED);
    }

    @Test
    void invalidPasswordsDoNotChangeCredentials() throws Exception {
        var session = login(user, "password123", 200);
        for (String candidate : List.of("short", "я".repeat(40), "password123")) {
            mvc.perform(post("/api/auth/change-password").header("Authorization", bearer(session))
                    .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(
                            Map.of("currentPassword", "password123", "newPassword", candidate))))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/auth/change-password").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
        assertThat(eventsFor(user)).doesNotContain(SecurityAuditEvent.Type.PASSWORD_CHANGED);
    }

    @Test
    void adminBlockRevokesTokensPermanentlyEvenAfterReactivation() throws Exception {
        var session = login(user, "password123", 200);
        var operator = login(admin, "password123", 200);
        mvc.perform(put("/api/admin/users/{id}/status", user.getId()).header("Authorization", bearer(session))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"BLOCKED\"}"))
                .andExpect(status().isForbidden());
        changeStatus(operator, "BLOCKED");
        assertRevoked(session);
        login(user, "password123", 401);
        changeStatus(operator, "ACTIVE");
        assertRevoked(session);
        login(user, "password123", 200);
        mvc.perform(put("/api/admin/users/{id}/status", UUID.randomUUID()).header("Authorization", bearer(operator))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"BLOCKED\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/security-events").header("Authorization", bearer(operator)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        mvc.perform(get("/api/admin/security-events").header("Authorization", bearer(login(user, "password123", 200))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/security-events?size=101").header("Authorization", bearer(operator)))
                .andExpect(status().isBadRequest());
        assertThat(audit.findAll().stream().filter(e -> user.getId().equals(e.getTargetId())).map(SecurityAuditEvent::getType))
                .contains(SecurityAuditEvent.Type.USER_BLOCKED, SecurityAuditEvent.Type.USER_ACTIVATED);
    }

    @Test
    void limiterRejectsBeforeAuthenticationAndIgnoresForwardedHeaders() throws Exception {
        String peer = "10.77.0.1";
        for (int i = 0; i < 1000; i++) assertThat(limiter.acquire(peer).allowed()).isTrue();
        mvc.perform(post("/api/auth/login").with(req -> { req.setRemoteAddr(peer); return req; })
                .header("X-Forwarded-For", "1.2.3.4")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("LOGIN_RATE_LIMITED"))
                .andExpect(jsonPath("$.traceId").exists());
        assertThat(audit.findAll().stream().map(SecurityAuditEvent::getType))
                .contains(SecurityAuditEvent.Type.LOGIN_THROTTLED);
    }

    @Test
    void corsHeadersAndNoCookieAuthenticationMatchApiContract() throws Exception {
        mvc.perform(options("/api/auth/login").header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
        mvc.perform(options("/api/auth/login").header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
        var session = login(user, "password123", 200);
        mvc.perform(get("/api/users/me").cookie(new jakarta.servlet.http.Cookie("accessToken", session.get("accessToken").asText())))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me").secure(true).header("Authorization", bearer(session)))
                .andExpect(status().isOk()).andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Strict-Transport-Security"))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    private void changeStatus(JsonNode operator, String status) throws Exception {
        mvc.perform(put("/api/admin/users/{id}/status", user.getId()).header("Authorization", bearer(operator))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isNoContent());
    }

    private List<SecurityAuditEvent.Type> eventsFor(User account) {
        return audit.findAll().stream().filter(e -> account.getId().equals(e.getActorId()))
                .map(SecurityAuditEvent::getType).toList();
    }

    private User create(Role role) {
        return users.saveAndFlush(User.builder().email(UUID.randomUUID() + "@example.com")
                .password(encoder.encode("password123")).roles(Set.of(role)).build());
    }

    protected JsonNode login(User account, String password, int expected) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("email", account.getEmail(), "password", password))))
                .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }

    protected String bearer(JsonNode session) { return "Bearer " + session.get("accessToken").asText(); }

    protected void assertRevoked(JsonNode session) throws Exception {
        mvc.perform(get("/api/users/me").header("Authorization", bearer(session))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.TEXT_PLAIN).content(session.get("refreshToken").asText()))
                .andExpect(status().isUnauthorized());
    }
}
