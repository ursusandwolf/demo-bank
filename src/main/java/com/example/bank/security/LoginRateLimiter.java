package com.example.bank.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/** Fixed windows per socket peer; bounded storage fails closed if all buckets are active. */
@Component
public class LoginRateLimiter {
    private final int maxAttempts;
    private final int maxClients;
    private final long windowMillis;
    private final Clock clock;
    private final Map<String, Window> clients = new HashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public LoginRateLimiter(@Value("${security.login.max-attempts:10}") int maxAttempts,
                            @Value("${security.login.window-seconds:60}") int windowSeconds,
                            @Value("${security.login.max-clients:10000}") int maxClients) {
        this(maxAttempts, windowSeconds, maxClients, Clock.systemUTC());
    }

    LoginRateLimiter(int maxAttempts, int windowSeconds, int maxClients, Clock clock) {
        if (maxAttempts < 1 || windowSeconds < 1 || maxClients < 1) {
            throw new IllegalArgumentException("Login rate limits must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.maxClients = maxClients;
        this.windowMillis = windowSeconds * 1000L;
        this.clock = clock;
    }

    public synchronized Decision acquire(String peer) {
        long now = clock.millis();
        Window window = clients.get(peer);
        if (window == null || now >= window.expiresAt) {
            if (window == null && clients.size() >= maxClients) {
                clients.values().removeIf(value -> now >= value.expiresAt);
                if (clients.size() >= maxClients) {
                    return new Decision(false, Math.max(1, windowMillis / 1000), false);
                }
            }
            window = new Window(now + windowMillis);
            clients.put(peer, window);
        }
        if (window.attempts < maxAttempts) {
            window.attempts++;
            return new Decision(true, 0, false);
        }
        boolean audit = !window.reported;
        window.reported = true;
        return new Decision(false, Math.max(1, (window.expiresAt - now + 999) / 1000), audit);
    }

    public record Decision(boolean allowed, long retryAfterSeconds, boolean audit) {}

    private static class Window {
        final long expiresAt;
        int attempts;
        boolean reported;
        Window(long expiresAt) { this.expiresAt = expiresAt; }
    }
}
