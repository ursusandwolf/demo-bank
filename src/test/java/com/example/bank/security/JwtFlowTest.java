package com.example.bank.security;

import com.example.bank.user.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JwtFlowTest {
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RefreshTokenRepository tokens;
    @Autowired RefreshTokenService tokenService;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @Autowired ObjectMapper mapper;
    private User user;

    @BeforeEach
    void setUp() {
        user = users.saveAndFlush(User.builder().email("jwt@example.com")
                .password(encoder.encode("password123"))
                .firstName("Jane").lastName("Doe").roles(Set.of(Role.USER)).build());
    }

    @Test
    void loginAuthenticatesProfileAndRefreshRotatesToken() throws Exception {
        var body = mapper.readTree(mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("email", user.getEmail(), "password", "password123"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn().getResponse().getContentAsString());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + body.get("accessToken").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.password").doesNotExist());
        String oldToken = body.get("refreshToken").asText();
        assertThat(tokens.findByToken(oldToken)).isEmpty();
        assertThat(tokens.findByToken(RefreshTokenService.hashToken(oldToken))).isPresent();
        var refreshed = mapper.readTree(mvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.TEXT_PLAIN).content(oldToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(refreshed.get("refreshToken").asText()).isNotEqualTo(oldToken);
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.TEXT_PLAIN).content(oldToken))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void malformedAndExpiredJwtReturn401WithTraceId() throws Exception {
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.traceId").exists());
        String expired = io.jsonwebtoken.Jwts.builder().subject(user.getEmail())
                .expiration(java.util.Date.from(Instant.now().minusSeconds(60)))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(
                        "c3VwZXItbG9uZy10ZXN0LW9ubHktc2VjcmV0LWtleS1mb3ItSFMzODQtc2lnbmluZw=="))).compact();
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blockingUserInvalidatesAccessAndRefresh() throws Exception {
        String access = jwt.generateToken(user);
        var refresh = tokenService.createRefreshToken(user.getEmail());
        user.setStatus(UserStatus.BLOCKED);
        users.saveAndFlush(user);
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.TEXT_PLAIN).content(refresh.token()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredRefreshAndInvalidLoginReturnClientErrors() throws Exception {
        tokens.saveAndFlush(RefreshToken.builder().user(user).token(RefreshTokenService.hashToken("expired-refresh"))
                .expiryDate(Instant.now().minusSeconds(1)).build());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.TEXT_PLAIN).content("expired-refresh"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrationAliasCreatesOnlyUserRoleAndHashesPassword() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"new-jwt@example.com\",\"password\":\"password123\",\"firstName\":\"John\",\"lastName\":\"Doe\",\"roles\":[\"ADMIN\"]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.password").doesNotExist());
        User created = users.findByEmail("new-jwt@example.com").orElseThrow();
        assertThat(created.getRoles()).containsExactly(Role.USER);
        assertThat(encoder.matches("password123", created.getPassword())).isTrue();
    }
}
