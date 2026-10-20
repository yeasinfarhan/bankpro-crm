package com.bankpro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides a single {@link Clock} for the whole application.
 * <p>
 * Services ask this bean for "now" instead of calling {@code Instant.now()} directly.
 * In tests we can replace it with {@code Clock.fixed(...)}, which makes date logic such as
 * "is this follow-up overdue?" fully deterministic.
 * We store all timestamps in UTC; conversion to Asia/Dhaka happens only for business periods and display.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
