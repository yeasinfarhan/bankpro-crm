package com.bankpro.auth;

import com.bankpro.repository.AuditLogRepository;
import com.bankpro.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTest {

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private AuditLogRepository auditLogs;

    @Test
    @DisplayName("valid credentials return a bearer token and the user profile (no password hash)")
    void loginSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("  Manager.Motijheel@BankPro.demo ", DEMO_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken", not(containsString(" "))))
                .andExpect(jsonPath("$.expiresIn", greaterThan(3000)))
                .andExpect(jsonPath("$.user.email").value(MANAGER_MTJ))
                .andExpect(jsonPath("$.user.role").value("BRANCH_MANAGER"))
                .andExpect(jsonPath("$.user.branch.code").value("DHK-MTJ"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("wrong password and unknown email return the same generic 401")
    void loginFailureIsGeneric() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(OFFICER_MTJ, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("nobody@bankpro.demo", DEMO_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));

        assertThat(auditLogs.findTop20ByActionOrderByIdDesc("LOGIN_FAILED"))
                .anySatisfy(log -> assertThat(log.getDetails()).containsEntry("reason", "UNKNOWN_EMAIL"))
                .allSatisfy(log -> assertThat(log.getDetails().toString()).doesNotContain(DEMO_PASSWORD));
    }

    @Test
    @DisplayName("an inactive account cannot log in, even with the correct password")
    void inactiveUserRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(INACTIVE_OFFICER, DEMO_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    @DisplayName("invalid login input returns 400 with field errors")
    void loginValidation() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("not-an-email", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").exists());
    }

    @Test
    @DisplayName("after 5 failures for one email + IP, the 6th attempt is blocked with 429")
    void loginRateLimited() throws Exception {
        String email = "rate.limit.probe@bankpro.demo";
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(email, "guess-" + i)))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "guess-6")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", containsString("Too many failed sign-in attempts")));
    }

    @Test
    @DisplayName("/me returns the caller's profile with a valid token")
    void meWithToken() throws Exception {
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(OFFICER_MTJ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(OFFICER_MTJ))
                .andExpect(jsonPath("$.role").value("RELATIONSHIP_OFFICER"))
                .andExpect(jsonPath("$.lastLoginAt").isNotEmpty());
    }

    @Test
    @DisplayName("protected endpoints reject missing, tampered, expired and foreign-issuer tokens")
    void rejectsBadTokens() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("sign in")));

        String valid = bearer(OFFICER_MTJ);
        String tampered = valid.substring(0, valid.length() - 4) + (valid.endsWith("AAAA") ? "BBBB" : "AAAA");
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, tampered))
                .andExpect(status().isUnauthorized());

        Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
        String expired = token("bankpro", past, past.plus(30, ChronoUnit.MINUTES));
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized());

        Instant now = Instant.now();
        String otherIssuer = token("someone-else", now, now.plus(30, ChronoUnit.MINUTES));
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + otherIssuer))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("public system info stays reachable without a token and responses carry security headers")
    void publicEndpointAndHeaders() throws Exception {
        mockMvc.perform(get("/api/system/info"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    /** Signs a token with the real key but custom issuer/times, for negative tests. */
    private String token(String issuer, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer).subject("3").issuedAt(issuedAt).expiresAt(expiresAt)
                .claim("email", OFFICER_MTJ).claim("name", "Nusrat Jahan")
                .claim("role", "RELATIONSHIP_OFFICER").claim("branchId", 1L)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
