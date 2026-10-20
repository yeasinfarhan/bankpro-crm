package com.bankpro.security;

import com.bankpro.config.AppProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory brute-force protection: after N failed logins for the same email + IP inside the
 * time window, further attempts are refused until old failures expire. A successful login resets the count.
 * <p>
 * Limitation (documented): state lives in one JVM, so it resets on restart and is not shared between
 * multiple backend instances. A shared store (e.g. Redis) would be needed for horizontal scaling.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration window;
    private final Clock clock;

    public LoginRateLimiter(AppProperties props, Clock clock) {
        this.maxFailures = props.loginRateLimit().maxFailures();
        this.window = props.loginRateLimit().window();
        this.clock = clock;
    }

    public boolean isBlocked(String email, String ip) {
        Deque<Instant> attempts = failures.get(key(email, ip));
        if (attempts == null) {
            return false;
        }
        synchronized (attempts) {
            prune(attempts);
            return attempts.size() >= maxFailures;
        }
    }

    public void recordFailure(String email, String ip) {
        if (failures.size() > MAX_TRACKED_KEYS) {
            evictExpired();
        }
        Deque<Instant> attempts = failures.computeIfAbsent(key(email, ip), k -> new ArrayDeque<>());
        synchronized (attempts) {
            prune(attempts);
            attempts.addLast(clock.instant());
        }
    }

    public void reset(String email, String ip) {
        failures.remove(key(email, ip));
    }

    /** Keeps memory bounded if many different emails/IPs are tried. */
    private void evictExpired() {
        failures.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                prune(e.getValue());
                return e.getValue().isEmpty();
            }
        });
    }

    private void prune(Deque<Instant> attempts) {
        Instant cutoff = clock.instant().minus(window);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
            attempts.pollFirst();
        }
    }

    private static String key(String email, String ip) {
        return email + "|" + ip;
    }
}
