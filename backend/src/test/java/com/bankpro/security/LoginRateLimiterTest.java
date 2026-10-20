package com.bankpro.security;

import com.bankpro.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterTest {

    /** A clock we can move forward inside the test. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-19T03:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginRateLimiter limiter = new LoginRateLimiter(new AppProperties(
            ZoneId.of("Asia/Dhaka"), List.of("http://localhost:5173"),
            new AppProperties.Jwt("", Duration.ofMinutes(60), "bankpro", true),
            new AppProperties.LoginRateLimit(3, Duration.ofMinutes(15))), clock);

    @Test
    void blocksAfterMaxFailuresAndUnblocksWhenWindowPasses() {
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.isBlocked("a@x", "1.1.1.1")).isFalse();
            limiter.recordFailure("a@x", "1.1.1.1");
        }
        assertThat(limiter.isBlocked("a@x", "1.1.1.1")).isTrue();
        assertThat(limiter.isBlocked("a@x", "2.2.2.2")).as("different IP").isFalse();
        assertThat(limiter.isBlocked("b@x", "1.1.1.1")).as("different email").isFalse();

        clock.advance(Duration.ofMinutes(16));
        assertThat(limiter.isBlocked("a@x", "1.1.1.1")).isFalse();
    }

    @Test
    void successfulLoginResetsTheCounter() {
        limiter.recordFailure("a@x", "1.1.1.1");
        limiter.recordFailure("a@x", "1.1.1.1");
        limiter.reset("a@x", "1.1.1.1");
        limiter.recordFailure("a@x", "1.1.1.1");
        assertThat(limiter.isBlocked("a@x", "1.1.1.1")).isFalse();
    }
}
