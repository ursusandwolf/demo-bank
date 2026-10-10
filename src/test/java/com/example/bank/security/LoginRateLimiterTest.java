package com.example.bank.security;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterTest {
    private final AtomicLong now = new AtomicLong();
    private final Clock clock = new Clock() {
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return Instant.ofEpochMilli(now.get()); }
    };

    @Test
    void windowExpiresAndOnlyFirstRejectionIsAudited() {
        var limiter = new LoginRateLimiter(2, 60, 2, clock);
        assertThat(limiter.acquire("one").allowed()).isTrue();
        assertThat(limiter.acquire("one").allowed()).isTrue();
        assertThat(limiter.acquire("one").audit()).isTrue();
        assertThat(limiter.acquire("one").audit()).isFalse();
        now.set(59_001);
        assertThat(limiter.acquire("one").retryAfterSeconds()).isEqualTo(1);
        now.set(60_000);
        assertThat(limiter.acquire("one").allowed()).isTrue();
    }

    @Test
    void capacityFailsClosedUntilExpiredClientsCanBeRemoved() {
        var limiter = new LoginRateLimiter(1, 60, 1, clock);
        assertThat(limiter.acquire("one").allowed()).isTrue();
        assertThat(limiter.acquire("two").allowed()).isFalse();
        assertThat(limiter.acquire("one").allowed()).isFalse();
        now.set(60_000);
        assertThat(limiter.acquire("two").allowed()).isTrue();
    }

    @Test
    void parallelRequestsCannotExceedLimit() throws Exception {
        var limiter = new LoginRateLimiter(5, 60, 1, clock);
        try (var pool = Executors.newFixedThreadPool(8)) {
            var jobs = java.util.stream.IntStream.range(0, 50)
                    .mapToObj(i -> (Callable<Boolean>) () -> limiter.acquire("peer").allowed()).toList();
            int allowed = 0;
            for (var result : pool.invokeAll(jobs)) if (result.get()) allowed++;
            assertThat(allowed).isEqualTo(5);
        }
    }
}
