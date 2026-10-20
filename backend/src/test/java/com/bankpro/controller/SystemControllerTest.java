package com.bankpro.controller;

import com.bankpro.config.AppProperties;
import com.bankpro.security.ErrorResponseWriter;
import com.bankpro.security.JwtConfig;
import com.bankpro.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer test (no database) with the real security rules loaded.
 * A fixed clock makes the expected Dhaka time deterministic: 2026-10-19T02:00:00Z = 08:00 in Asia/Dhaka.
 */
@WebMvcTest(SystemController.class)
@Import({SystemControllerTest.FixedClockConfig.class, SecurityConfig.class, JwtConfig.class, ErrorResponseWriter.class})
@TestPropertySource(properties = {
        "app.business-zone=Asia/Dhaka",
        "app.cors-allowed-origins=http://localhost:5173",
        "app.jwt.ttl=60m",
        "app.jwt.issuer=bankpro",
        "app.jwt.allow-ephemeral-secret=true",
        "app.login-rate-limit.max-failures=5",
        "app.login-rate-limit.window=15m"
})
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void infoIsPublicAndReturnsServerTimeInBusinessZone() throws Exception {
        mockMvc.perform(get("/api/system/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("BankPro"))
                .andExpect(jsonPath("$.businessZone").value("Asia/Dhaka"))
                .andExpect(jsonPath("$.serverTimeUtc").value("2026-10-19T02:00:00Z"))
                .andExpect(jsonPath("$.businessLocalTime").value("2026-10-19 08:00:00"));
    }

    @TestConfiguration
    @EnableConfigurationProperties(AppProperties.class)
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-10-19T02:00:00Z"), ZoneOffset.UTC);
        }
    }
}
