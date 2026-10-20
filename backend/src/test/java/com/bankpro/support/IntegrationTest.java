package com.bankpro.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for full-stack tests: real Spring context, real security filters, real MySQL.
 * All subclasses share one cached application context, so the database starts only once.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class IntegrationTest {

    /** Documented demo password (db/dev/R__dev_seed_users.sql). */
    protected static final String DEMO_PASSWORD = "BankPro@2026";
    protected static final String ADMIN = "admin@bankpro.demo";
    protected static final String MANAGER_MTJ = "manager.motijheel@bankpro.demo";
    protected static final String OFFICER_MTJ = "nusrat.jahan@bankpro.demo";
    protected static final String MANAGER_GUL = "manager.gulshan@bankpro.demo";
    protected static final String OFFICER2_MTJ = "tanvir.ahmed@bankpro.demo";
    protected static final String OFFICER_GUL = "imran.hossain@bankpro.demo";
    protected static final String INACTIVE_OFFICER = "former.officer@bankpro.demo";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String loginBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new LoginPayload(email, password));
    }

    /** Logs in and returns "Bearer <token>" for use in an Authorization header. */
    protected String bearer(String email) throws Exception {
        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, DEMO_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(json);
        return "Bearer " + node.get("accessToken").asText();
    }

    private static final java.util.concurrent.atomic.AtomicInteger PHONE_SEQ =
            new java.util.concurrent.atomic.AtomicInteger((int) (System.nanoTime() % 1_000_000));

    /** A fresh, valid, unique Bangladesh mobile number in local format (01XXXXXXXXX). */
    protected static String uniquePhone() {
        return String.format("0139%07d", PHONE_SEQ.incrementAndGet() % 10_000_000);
    }

    protected JsonNode json(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private record LoginPayload(String email, String password) {
    }
}
