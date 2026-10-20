package com.bankpro.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHashingTest {

    private final PasswordEncoder encoder = new SecurityConfig().passwordEncoder();

    @Test
    void hashesAreSaltedAndVerifiable() {
        String first = encoder.encode("BankPro@2026");
        String second = encoder.encode("BankPro@2026");

        assertThat(first).startsWith("$2a$10$").isNotEqualTo("BankPro@2026");
        assertThat(first).as("random salt => different hash each time").isNotEqualTo(second);
        assertThat(encoder.matches("BankPro@2026", first)).isTrue();
        assertThat(encoder.matches("bankpro@2026", first)).isFalse();
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void everySeededDemoHashMatchesTheDocumentedPassword() throws IOException {
        String sql;
        try (var in = getClass().getResourceAsStream("/db/dev/R__dev_seed_users.sql")) {
            assertThat(in).isNotNull();
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = Pattern.compile("'(\\$2a\\$10\\$[./A-Za-z0-9]{53})'").matcher(sql);
        int count = 0;
        while (m.find()) {
            assertThat(encoder.matches("BankPro@2026", m.group(1))).as("hash #%d", count + 1).isTrue();
            count++;
        }
        assertThat(count).isEqualTo(7);
    }
}
