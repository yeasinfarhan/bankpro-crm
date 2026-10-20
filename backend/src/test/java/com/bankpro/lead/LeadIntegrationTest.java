package com.bankpro.lead;

import com.bankpro.entity.Role;
import com.bankpro.security.AuthenticatedUser;
import com.bankpro.service.LeadService;
import com.bankpro.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeadIntegrationTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private LeadService leadService;

    private long id(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private long productId(String code) {
        return id("SELECT id FROM banking_products WHERE code = ?", code);
    }

    /** Creates a fresh prospect in the officer's branch so every test starts from a known state. */
    private long newCustomer(String token) throws Exception {
        Map<String, Object> body = Map.of("fullName", "Lead Test Customer", "phone", uniquePhone(), "segment", "MASS");
        return json(mockMvc.perform(post("/api/customers").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body))))
                .get("id").asLong();
    }

    private JsonNode newLead(String token, long customerId, String productCode, Long officerId) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("customerId", customerId, "productId", productId(productCode),
                "source", "BRANCH_WALK_IN", "estimatedValue", 500000, "priority", "HIGH"));
        if (officerId != null) body.put("assignedOfficerId", officerId);
        return json(mockMvc.perform(post("/api/leads").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated()));
    }

    private org.springframework.test.web.servlet.ResultActions move(String token, long leadId, String to, String reason)
            throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("status", to));
        if (reason != null) body.put("lossReason", reason);
        return mockMvc.perform(patch("/api/leads/" + leadId + "/status").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)));
    }

    private org.springframework.test.web.servlet.ResultActions convert(String token, long leadId, Object amount)
            throws Exception {
        return mockMvc.perform(post("/api/leads/" + leadId + "/convert").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":" + amount + "}"));
    }

    private void qualify(String token, long leadId) throws Exception {
        move(token, leadId, "CONTACTED", null).andExpect(status().isOk());
        move(token, leadId, "QUALIFIED", null).andExpect(status().isOk());
    }

    private AuthenticatedUser principal(String email) {
        Map<String, Object> u = jdbc.queryForMap("SELECT id, full_name, role, branch_id FROM users WHERE email = ?", email);
        return new AuthenticatedUser(((Number) u.get("id")).longValue(), email, (String) u.get("full_name"),
                Role.valueOf((String) u.get("role")), u.get("branch_id") == null ? null : ((Number) u.get("branch_id")).longValue());
    }

    @Test
    @DisplayName("an officer creates a lead for a branch customer; it is theirs, NEW, with a derived code")
    void createLead() throws Exception {
        String officer = bearer(OFFICER_MTJ);
        JsonNode lead = newLead(officer, newCustomer(officer), "FDR-1Y", null);
        assertThat(lead.get("status").asText()).isEqualTo("NEW");
        assertThat(lead.get("code").asText()).isEqualTo(String.format("LD-%06d", lead.get("id").asLong()));
        assertThat(lead.get("assignedOfficer").get("fullName").asText()).isEqualTo("Nusrat Jahan");
        assertThat(lead.get("allowedStatuses")).extracting(JsonNode::asText).containsExactlyInAnyOrder("CONTACTED", "LOST");
        assertThat(lead.get("permissions").get("canConvert").asBoolean()).isFalse();

        long gulshanCustomer = id("SELECT c.id FROM customers c JOIN branches b ON b.id = c.branch_id WHERE b.code = 'DHK-GUL' LIMIT 1");
        Map<String, Object> body = Map.of("customerId", gulshanCustomer, "productId", productId("SAV-STD"),
                "source", "REFERRAL", "estimatedValue", 1000);
        mockMvc.perform(post("/api/leads").header(HttpHeaders.AUTHORIZATION, officer)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("customerId"));
    }

    @Test
    @DisplayName("scope: officers see only their own leads, managers their branch, other branches get 404")
    void leadScope() throws Exception {
        String tanvir = bearer(OFFICER2_MTJ);
        JsonNode tanvirsLead = newLead(tanvir, newCustomer(tanvir), "SAV-STD", null);
        long leadId = tanvirsLead.get("id").asLong();

        mockMvc.perform(get("/api/leads").param("size", "100").header(HttpHeaders.AUTHORIZATION, bearer(OFFICER_MTJ)))
                .andExpect(jsonPath("$.content[*].assignedOfficer.fullName", everyItem(is("Nusrat Jahan"))))
                .andExpect(jsonPath("$.content[*].id", not(hasItem((int) leadId))));
        mockMvc.perform(get("/api/leads/" + leadId).header(HttpHeaders.AUTHORIZATION, bearer(OFFICER_MTJ)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/leads/" + leadId).header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_MTJ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canAssign").value(true));
        mockMvc.perform(get("/api/leads/" + leadId).header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_GUL)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/leads/" + leadId).header(HttpHeaders.AUTHORIZATION, bearer(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canEdit").value(false))
                .andExpect(jsonPath("$.allowedStatuses").isEmpty());
    }

    @Test
    @DisplayName("stage rules: one step at a time, no CONVERTED via PATCH, LOST needs a reason, only managers reopen")
    void stageRules() throws Exception {
        String officer = bearer(OFFICER_MTJ);
        long leadId = newLead(officer, newCustomer(officer), "DPS-STD", null).get("id").asLong();

        move(officer, leadId, "QUALIFIED", null).andExpect(status().isConflict());
        move(officer, leadId, "CONVERTED", null).andExpect(status().isBadRequest());
        move(officer, leadId, "CONTACTED", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONTACTED"))
                .andExpect(jsonPath("$.lastContactedAt").isNotEmpty());
        move(officer, leadId, "LOST", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("lossReason"));
        move(officer, leadId, "LOST", "CHOSE_COMPETITOR").andExpect(status().isOk())
                .andExpect(jsonPath("$.lossReason").value("CHOSE_COMPETITOR"))
                .andExpect(jsonPath("$.permissions.canEdit").value(false));
        move(officer, leadId, "CONTACTED", null).andExpect(status().isForbidden());
        move(bearer(MANAGER_MTJ), leadId, "CONTACTED", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.lossReason").doesNotExist());
        // Only the 3 successful moves are audited (CONTACTED, LOST, reopened CONTACTED); rejected attempts are not.
        assertThat(id("SELECT COUNT(*) FROM audit_logs WHERE action = 'LEAD_STATUS_CHANGED' AND entity_id = ?", leadId))
                .isEqualTo(3);
    }

    @Test
    @DisplayName("conversion creates exactly one COMPLETED sale for deposits; repeats return the same sale")
    void convertDeposit() throws Exception {
        String officer = bearer(OFFICER_MTJ);
        long customerId = newCustomer(officer);
        long leadId = newLead(officer, customerId, "FDR-1Y", null).get("id").asLong();

        convert(officer, leadId, 250000).andExpect(status().isConflict()); // still NEW
        qualify(officer, leadId);
        convert(officer, leadId, 0).andExpect(status().isBadRequest());

        JsonNode first = json(convert(officer, leadId, 250000).andExpect(status().isCreated()));
        assertThat(first.get("alreadyConverted").asBoolean()).isFalse();
        assertThat(first.get("sale").get("status").asText()).isEqualTo("COMPLETED");
        assertThat(first.get("lead").get("status").asText()).isEqualTo("CONVERTED");

        JsonNode again = json(convert(officer, leadId, 999).andExpect(status().isOk()));
        assertThat(again.get("alreadyConverted").asBoolean()).isTrue();
        assertThat(again.get("sale").get("id").asLong()).isEqualTo(first.get("sale").get("id").asLong());
        assertThat(again.get("sale").get("amount").decimalValue()).isEqualByComparingTo("250000");

        assertThat(id("SELECT COUNT(*) FROM sales WHERE lead_id = ?", leadId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM customers WHERE id = ?", String.class, customerId)).isEqualTo("ACTIVE");
        assertThat(id("SELECT COUNT(*) FROM customer_products WHERE customer_id = ? AND source_sale_id IS NOT NULL", customerId))
                .isEqualTo(1);
        move(officer, leadId, "LOST", "OTHER").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("concurrent convert requests for one lead still create exactly one sale")
    void concurrentConversion() throws Exception {
        String officer = bearer(OFFICER_MTJ);
        long leadId = newLead(officer, newCustomer(officer), "SAV-STD", null).get("id").asLong();
        qualify(officer, leadId);

        AuthenticatedUser caller = principal(OFFICER_MTJ);
        int threads = 6;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Callable<Boolean> task = () -> {
                start.await();
                return !leadService.convert(caller, leadId, new BigDecimal("1000"), "127.0.0.1").alreadyConverted();
            };
            results.add(pool.submit(task));
        }
        start.countDown();
        int created = 0;
        for (Future<Boolean> f : results) {
            if (f.get()) created++;
        }
        pool.shutdown();

        assertThat(created).as("exactly one request creates the sale").isEqualTo(1);
        assertThat(id("SELECT COUNT(*) FROM sales WHERE lead_id = ?", leadId)).isEqualTo(1);
    }

    @Test
    @DisplayName("loan conversion is PENDING_APPROVAL until a manager decides; officers cannot decide")
    void loanNeedsApproval() throws Exception {
        String officer = bearer(OFFICER_MTJ);
        long customerId = newCustomer(officer);
        long leadId = newLead(officer, customerId, "PL-STD", null).get("id").asLong();
        qualify(officer, leadId);
        long saleId = json(convert(officer, leadId, 800000).andExpect(status().isCreated()))
                .get("sale").get("id").asLong();

        assertThat(jdbc.queryForObject("SELECT status FROM sales WHERE id = ?", String.class, saleId)).isEqualTo("PENDING_APPROVAL");
        assertThat(id("SELECT COUNT(*) FROM customer_products WHERE customer_id = ?", customerId))
                .as("no holding until approved").isZero();

        String decision = "{\"decision\":\"APPROVE\",\"note\":\"Simulated approval\"}";
        mockMvc.perform(patch("/api/sales/" + saleId + "/decision").header(HttpHeaders.AUTHORIZATION, officer)
                .contentType(MediaType.APPLICATION_JSON).content(decision)).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/sales/" + saleId + "/decision").header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_GUL))
                .contentType(MediaType.APPLICATION_JSON).content(decision)).andExpect(status().isNotFound());

        String manager = bearer(MANAGER_MTJ);
        mockMvc.perform(get("/api/sales").param("status", "PENDING_APPROVAL").header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(jsonPath("$[?(@.id == " + saleId + ")].canDecide").value(hasItem(true)));
        mockMvc.perform(patch("/api/sales/" + saleId + "/decision").header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON).content(decision))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mockMvc.perform(patch("/api/sales/" + saleId + "/decision").header(HttpHeaders.AUTHORIZATION, manager)
                .contentType(MediaType.APPLICATION_JSON).content(decision)).andExpect(status().isConflict());
        assertThat(id("SELECT COUNT(*) FROM customer_products WHERE customer_id = ? AND source_sale_id = ?", customerId, saleId))
                .isEqualTo(1);
        assertThat(id("SELECT COUNT(*) FROM notifications n JOIN users u ON u.id = n.user_id WHERE u.email = ? AND n.type = 'SALE_DECIDED'",
                OFFICER_MTJ)).isGreaterThan(0);
    }

    @Test
    @DisplayName("managers reassign within their branch; the new officer is notified and gains access")
    void reassign() throws Exception {
        String nusrat = bearer(OFFICER_MTJ);
        long leadId = newLead(nusrat, newCustomer(nusrat), "CC-CLASSIC", null).get("id").asLong();
        long tanvirId = id("SELECT id FROM users WHERE email = ?", OFFICER2_MTJ);
        String body = "{\"officerId\":" + tanvirId + "}";

        mockMvc.perform(patch("/api/leads/" + leadId + "/assign").header(HttpHeaders.AUTHORIZATION, nusrat)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/leads/" + leadId + "/assign").header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_MTJ))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedOfficer.fullName").value("Tanvir Ahmed"));

        String tanvir = bearer(OFFICER2_MTJ);
        mockMvc.perform(get("/api/leads/" + leadId).header(HttpHeaders.AUTHORIZATION, tanvir)).andExpect(status().isOk());
        mockMvc.perform(get("/api/leads/" + leadId).header(HttpHeaders.AUTHORIZATION, nusrat)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, tanvir))
                .andExpect(jsonPath("$.items[0].type").value("LEAD_ASSIGNED"))
                .andExpect(jsonPath("$.items[0].link").value("/leads/" + leadId));

        long gulshanOfficer = id("SELECT id FROM users WHERE email = ?", OFFICER_GUL);
        mockMvc.perform(patch("/api/leads/" + leadId + "/assign").header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_MTJ))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"officerId\":" + gulshanOfficer + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("edits use optimistic locking; closed leads cannot be edited")
    void editLead() throws Exception {
        String officer = bearer(OFFICER_MTJ);
        JsonNode lead = newLead(officer, newCustomer(officer), "SAV-STD", null);
        long leadId = lead.get("id").asLong();
        Map<String, Object> edit = new HashMap<>(Map.of("productId", productId("DPS-STD"), "source", "REFERRAL",
                "estimatedValue", 120000, "notes", "Wants a 5-year DPS", "version", lead.get("version").asLong()));

        mockMvc.perform(put("/api/leads/" + leadId).header(HttpHeaders.AUTHORIZATION, officer)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(edit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.code").value("DPS-STD"))
                .andExpect(jsonPath("$.notes").value("Wants a 5-year DPS"));
        mockMvc.perform(put("/api/leads/" + leadId).header(HttpHeaders.AUTHORIZATION, officer)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(edit)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("pipeline summary and board are computed in SQL and match the visible leads")
    void summaryAndBoard() throws Exception {
        String officer = bearer(OFFICER_MTJ);
        long nusratId = id("SELECT id FROM users WHERE email = ?", OFFICER_MTJ);
        JsonNode summary = json(mockMvc.perform(get("/api/leads/summary").header(HttpHeaders.AUTHORIZATION, officer))
                .andExpect(status().isOk()));

        long open = id("SELECT COUNT(*) FROM leads WHERE assigned_officer_id = ? AND status IN ('NEW','CONTACTED','QUALIFIED','PROPOSAL')", nusratId);
        long converted = id("SELECT COUNT(*) FROM leads WHERE assigned_officer_id = ? AND status = 'CONVERTED'", nusratId);
        long lost = id("SELECT COUNT(*) FROM leads WHERE assigned_officer_id = ? AND status = 'LOST'", nusratId);
        BigDecimal openValue = jdbc.queryForObject("SELECT COALESCE(SUM(estimated_value),0) FROM leads WHERE assigned_officer_id = ? AND status IN ('NEW','CONTACTED','QUALIFIED','PROPOSAL')", BigDecimal.class, nusratId);

        assertThat(summary.get("openCount").asLong()).isEqualTo(open);
        assertThat(summary.get("convertedCount").asLong()).isEqualTo(converted);
        assertThat(summary.get("lostCount").asLong()).isEqualTo(lost);
        assertThat(summary.get("openPipelineValue").decimalValue()).isEqualByComparingTo(openValue);

        JsonNode board = json(mockMvc.perform(get("/api/leads/board").header(HttpHeaders.AUTHORIZATION, officer))
                .andExpect(status().isOk()));
        assertThat(board.get("NEW").get("count").asLong()).isEqualTo(summary.get("countByStatus").get("NEW").asLong());
        board.get("NEW").get("leads").forEach(l -> assertThat(l.get("status").asText()).isEqualTo("NEW"));
    }
}
