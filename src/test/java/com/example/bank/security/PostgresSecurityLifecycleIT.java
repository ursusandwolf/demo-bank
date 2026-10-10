package com.example.bank.security;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Map;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Failsafe only; requires a disposable PostgreSQL database. Inherits committed lifecycle tests. */
@ActiveProfiles(value = "postgres-test", inheritProfiles = false)
class PostgresSecurityLifecycleIT extends SecurityLifecycleTest {
    @Autowired TransactionTemplate transactions;

    @Test
    void twoRefreshRequestsCanConsumeTheSameTokenOnlyOnce() throws Exception {
        var session = login(user, "password123", 200);
        var outcomes = race(() -> refresh(session), () -> refresh(session));
        assertThat(outcomes.stream().map(r -> r.getResponse().getStatus())).containsExactlyInAnyOrder(200, 401);
        for (var outcome : outcomes) {
            if (outcome.getResponse().getStatus() == 200) {
                JsonNode rotated = mapper.readTree(outcome.getResponse().getContentAsString());
                mvc.perform(get("/api/users/me").header("Authorization", bearer(rotated))).andExpect(status().isOk());
                assertThat(refresh(rotated).getResponse().getStatus()).isEqualTo(200);
            }
        }
    }

    @Test
    void concurrentLogoutCannotLeaveARefreshedSessionAlive() throws Exception {
        var session = login(user, "password123", 200);
        var outcomes = race(() -> refresh(session), () -> mvc.perform(post("/api/auth/logout")
                .header("Authorization", bearer(session))).andReturn());
        assertThat(outcomes.get(1).getResponse().getStatus()).isEqualTo(204);
        assertThat(outcomes.get(0).getResponse().getStatus()).isIn(200, 401);
        assertRevoked(session);
        if (outcomes.get(0).getResponse().getStatus() == 200) {
            assertRevoked(mapper.readTree(outcomes.get(0).getResponse().getContentAsString()));
        }
        assertThat(refresh.findAll().stream().filter(t -> t.getUser().getId().equals(user.getId()))).isEmpty();
    }

    @Test
    void concurrentPasswordChangeCannotLeaveARefreshedSessionAlive() throws Exception {
        var session = login(user, "password123", 200);
        var outcomes = race(() -> refresh(session), () -> mvc.perform(post("/api/auth/change-password")
                .header("Authorization", bearer(session)).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("currentPassword", "password123", "newPassword", "changedPassword123"))))
                .andReturn());
        assertThat(outcomes.get(1).getResponse().getStatus()).isEqualTo(204);
        assertThat(outcomes.get(0).getResponse().getStatus()).isIn(200, 401);
        assertRevoked(session);
        if (outcomes.get(0).getResponse().getStatus() == 200) {
            assertRevoked(mapper.readTree(outcomes.get(0).getResponse().getContentAsString()));
        }
        login(user, "password123", 401);
        login(user, "changedPassword123", 200);
    }

    private MvcResult refresh(JsonNode session) throws Exception {
        return mvc.perform(post("/api/auth/refresh").contentType(MediaType.TEXT_PLAIN)
                .content(session.get("refreshToken").asText())).andReturn();
    }

    private java.util.List<MvcResult> race(Callable<MvcResult> first, Callable<MvcResult> second) throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var started = new CountDownLatch(2);
            var futures = transactions.execute(status -> {
                // Both requests contend with a real user-row lock before it is released.
                users.findForSecurityUpdate(user.getEmail()).orElseThrow();
                var one = pool.submit(() -> { started.countDown(); return first.call(); });
                var two = pool.submit(() -> { started.countDown(); return second.call(); });
                try {
                    assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted race test", ex);
                }
                return java.util.List.of(one, two);
            });
            return java.util.List.of(futures.get(0).get(15, TimeUnit.SECONDS),
                    futures.get(1).get(15, TimeUnit.SECONDS));
        }
    }
}
