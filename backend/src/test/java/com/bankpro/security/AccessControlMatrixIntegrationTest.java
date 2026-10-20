package com.bankpro.security;

import com.bankpro.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Phase 7 access-control matrix: tries every write endpoint and the sensitive read endpoints as the "wrong"
 * user — another branch, a colleague in the same branch, or a role that should not act — using the real
 * IDs of Motijheel records. Every attempt must be refused (403/404, or 400 when the request names a record
 * the caller cannot use). Changing an ID in the URL must never reach another branch's data.
 */
class AccessControlMatrixIntegrationTest extends IntegrationTest {

    private static final Set<Integer> HIDDEN = Set.of(404);
    private static final Set<Integer> FORBIDDEN = Set.of(403);
    private static final Set<Integer> HIDDEN_OR_FORBIDDEN = Set.of(403, 404);
    private static final Set<Integer> REJECTED_INPUT = Set.of(400, 404);

    @Autowired
    private JdbcTemplate jdbc;

    private final Map<String, String> tokens = new HashMap<>();

    private record Attempt(String label, String user, HttpMethod method, String url, Object body, Set<Integer> expected) {
    }

    private String token(String email) throws Exception {
        if (!tokens.containsKey(email)) tokens.put(email, bearer(email));
        return tokens.get(email);
    }

    private Long id(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }

    @TestFactory
    @DisplayName("wrong-branch, wrong-owner and wrong-role requests are refused")
    Stream<DynamicTest> matrix() {
        long mtj = id("SELECT id FROM branches WHERE code = 'DHK-MTJ'");
        long nusrat = id("SELECT id FROM users WHERE email = '" + OFFICER_MTJ + "'");
        long tanvir = id("SELECT id FROM users WHERE email = '" + OFFICER2_MTJ + "'");
        long nusratCustomer = id("SELECT MIN(id) FROM customers WHERE assigned_officer_id = " + nusrat);
        long tanvirCustomer = id("SELECT MIN(id) FROM customers WHERE assigned_officer_id = " + tanvir);
        long tanvirLead = id("SELECT MIN(id) FROM leads WHERE assigned_officer_id = " + tanvir);
        long nusratLead = id("SELECT MIN(id) FROM leads WHERE assigned_officer_id = " + nusrat);
        long tanvirFollowUp = id("SELECT MIN(id) FROM follow_up_activities WHERE assigned_officer_id = " + tanvir);
        long mtjSale = id("SELECT MIN(id) FROM sales WHERE branch_id = " + mtj);
        long mtjTarget = id("SELECT MIN(id) FROM sales_targets WHERE branch_id = " + mtj);
        jdbc.update("INSERT INTO notifications (user_id, type, message) VALUES (?, 'TEST', 'Access matrix probe')", tanvir);
        long tanvirNotification = id("SELECT MAX(id) FROM notifications WHERE user_id = " + tanvir);

        Map<String, Object> customer = Map.of("fullName", "Matrix Probe", "phone", uniquePhone(), "employmentType", "SALARIED",
                "incomeRange", "BELOW_30K", "city", "Dhaka", "segment", "MASS", "status", "PROSPECT", "version", 0);
        Map<String, Object> lost = Map.of("status", "LOST", "lossReason", "NO_LONGER_INTERESTED");
        Map<String, Object> amount = Map.of("amount", 1000);
        Map<String, Object> outcome = Map.of("outcome", "Matrix probe");
        Map<String, Object> when = Map.of("scheduledAt", "2026-12-01T10:00:00+06:00");
        Map<String, Object> approve = Map.of("decision", "APPROVE");
        Map<String, Object> note = Map.of("interactionType", "NOTE", "summary", "Matrix probe");
        Map<String, Object> leadFor = Map.of("customerId", nusratCustomer, "productId", 1, "source", "REFERRAL",
                "priority", "LOW", "estimatedValue", 1000);
        Map<String, Object> followUpFor = Map.of("customerId", nusratCustomer, "activityType", "PHONE_CALL",
                "scheduledAt", "2026-12-01T10:00:00+06:00");
        Map<String, Object> target = Map.of("userId", nusrat, "periodType", "MONTHLY", "periodStart", "2026-11-01",
                "category", "LOAN_LEADS", "targetValue", 2);

        List<Attempt> attempts = new ArrayList<>();
        // 1. Another branch: Gulshan manager and Gulshan officer against Motijheel records.
        for (String user : List.of(MANAGER_GUL, OFFICER_GUL)) {
            attempts.add(new Attempt("read customer", user, HttpMethod.GET, "/api/customers/" + nusratCustomer, null, HIDDEN));
            attempts.add(new Attempt("customer timeline", user, HttpMethod.GET, "/api/customers/" + nusratCustomer + "/timeline", null, HIDDEN));
            attempts.add(new Attempt("edit customer", user, HttpMethod.PUT, "/api/customers/" + nusratCustomer, customer, HIDDEN));
            attempts.add(new Attempt("log interaction", user, HttpMethod.POST, "/api/customers/" + nusratCustomer + "/interactions", note, HIDDEN));
            attempts.add(new Attempt("read lead", user, HttpMethod.GET, "/api/leads/" + tanvirLead, null, HIDDEN));
            attempts.add(new Attempt("move lead", user, HttpMethod.PATCH, "/api/leads/" + tanvirLead + "/status", lost, HIDDEN));
            attempts.add(new Attempt("convert lead", user, HttpMethod.POST, "/api/leads/" + tanvirLead + "/convert", amount, HIDDEN));
            attempts.add(new Attempt("create lead for their customer", user, HttpMethod.POST, "/api/leads", leadFor, REJECTED_INPUT));
            attempts.add(new Attempt("schedule follow-up for their customer", user, HttpMethod.POST, "/api/follow-ups", followUpFor, REJECTED_INPUT));
            attempts.add(new Attempt("complete follow-up", user, HttpMethod.PATCH, "/api/follow-ups/" + tanvirFollowUp + "/complete", outcome, HIDDEN));
            attempts.add(new Attempt("reschedule follow-up", user, HttpMethod.PATCH, "/api/follow-ups/" + tanvirFollowUp + "/reschedule", when, HIDDEN));
            attempts.add(new Attempt("decide sale", user, HttpMethod.PATCH, "/api/sales/" + mtjSale + "/decision", approve, HIDDEN_OR_FORBIDDEN));
            attempts.add(new Attempt("change target", user, HttpMethod.PUT, "/api/targets/" + mtjTarget, Map.of("targetValue", 1), HIDDEN_OR_FORBIDDEN));
            attempts.add(new Attempt("delete target", user, HttpMethod.DELETE, "/api/targets/" + mtjTarget, null, HIDDEN_OR_FORBIDDEN));
            attempts.add(new Attempt("read someone else's notification", user, HttpMethod.PATCH, "/api/notifications/" + tanvirNotification + "/read", null, HIDDEN));
        }
        attempts.add(new Attempt("assign customer to other-branch officer", MANAGER_GUL, HttpMethod.POST, "/api/customers",
                Map.of("fullName", "Matrix Probe", "phone", uniquePhone(), "employmentType", "SALARIED", "incomeRange", "BELOW_30K",
                        "city", "Dhaka", "segment", "MASS", "status", "PROSPECT", "assignedOfficerId", nusrat), REJECTED_INPUT));
        attempts.add(new Attempt("target for other-branch officer", MANAGER_GUL, HttpMethod.POST, "/api/targets", target, REJECTED_INPUT));

        // 2. Same branch, someone else's records (ownership).
        attempts.add(new Attempt("edit colleague's customer", OFFICER_MTJ, HttpMethod.PUT, "/api/customers/" + tanvirCustomer, customer, HIDDEN_OR_FORBIDDEN));
        attempts.add(new Attempt("read colleague's lead", OFFICER_MTJ, HttpMethod.GET, "/api/leads/" + tanvirLead, null, HIDDEN));
        attempts.add(new Attempt("move colleague's lead", OFFICER_MTJ, HttpMethod.PATCH, "/api/leads/" + tanvirLead + "/status", lost, HIDDEN_OR_FORBIDDEN));
        attempts.add(new Attempt("convert colleague's lead", OFFICER_MTJ, HttpMethod.POST, "/api/leads/" + tanvirLead + "/convert", amount, HIDDEN_OR_FORBIDDEN));
        attempts.add(new Attempt("complete colleague's follow-up", OFFICER_MTJ, HttpMethod.PATCH, "/api/follow-ups/" + tanvirFollowUp + "/complete", outcome, HIDDEN_OR_FORBIDDEN));
        attempts.add(new Attempt("read colleague's notification", OFFICER_MTJ, HttpMethod.PATCH, "/api/notifications/" + tanvirNotification + "/read", null, HIDDEN));
        attempts.add(new Attempt("manager reads officer's notification", MANAGER_MTJ, HttpMethod.PATCH, "/api/notifications/" + tanvirNotification + "/read", null, HIDDEN));

        // 3. Role limits (officer and admin actions that belong to a branch manager or admin).
        attempts.add(new Attempt("officer reassigns lead", OFFICER_MTJ, HttpMethod.PATCH, "/api/leads/" + nusratLead + "/assign", Map.of("officerId", tanvir), FORBIDDEN));
        attempts.add(new Attempt("officer decides sale", OFFICER_MTJ, HttpMethod.PATCH, "/api/sales/" + mtjSale + "/decision", approve, FORBIDDEN));
        attempts.add(new Attempt("officer archives customer", OFFICER_MTJ, HttpMethod.DELETE, "/api/customers/" + nusratCustomer, null, FORBIDDEN));
        attempts.add(new Attempt("officer creates target", OFFICER_MTJ, HttpMethod.POST, "/api/targets", target, FORBIDDEN));
        attempts.add(new Attempt("officer lists team", OFFICER_MTJ, HttpMethod.GET, "/api/users", null, FORBIDDEN));
        attempts.add(new Attempt("officer views officer performance", OFFICER_MTJ, HttpMethod.GET, "/api/dashboard/officer-performance", null, FORBIDDEN));
        attempts.add(new Attempt("admin creates customer", ADMIN, HttpMethod.POST, "/api/customers",
                Map.of("fullName", "Matrix Probe", "phone", uniquePhone(), "employmentType", "SALARIED", "incomeRange", "BELOW_30K",
                        "city", "Dhaka", "segment", "MASS", "status", "PROSPECT"), FORBIDDEN));
        attempts.add(new Attempt("admin moves lead", ADMIN, HttpMethod.PATCH, "/api/leads/" + nusratLead + "/status", lost, FORBIDDEN));
        attempts.add(new Attempt("admin decides sale", ADMIN, HttpMethod.PATCH, "/api/sales/" + mtjSale + "/decision", approve, FORBIDDEN));
        attempts.add(new Attempt("admin creates target", ADMIN, HttpMethod.POST, "/api/targets", target, FORBIDDEN));
        attempts.add(new Attempt("manager creates product", MANAGER_MTJ, HttpMethod.POST, "/api/products",
                Map.of("code", "MATRIX-1", "name", "Matrix Probe Account", "category", "ACCOUNT", "description", "Fictional",
                        "benefits", List.of("None")), FORBIDDEN));

        return attempts.stream().map(a -> DynamicTest.dynamicTest(
                a.label() + " — " + a.user() + " " + a.method() + " " + a.url(), () -> {
                    MockHttpServletRequestBuilder req = request(a.method(), a.url()).header(HttpHeaders.AUTHORIZATION, token(a.user()));
                    if (a.body() != null) {
                        req.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(a.body()));
                    }
                    int status = mockMvc.perform(req).andReturn().getResponse().getStatus();
                    assertThat(status).as(a.label()).isIn(a.expected());
                }));
    }

    @Test
    @DisplayName("a foreign branchId or officerId in list, dashboard, report and export URLs is ignored for non-admins")
    void parameterTampering() throws Exception {
        long mtj = id("SELECT id FROM branches WHERE code = 'DHK-MTJ'");
        long nusrat = id("SELECT id FROM users WHERE email = '" + OFFICER_MTJ + "'");
        for (String user : List.of(MANAGER_GUL, OFFICER_GUL)) {
            String auth = token(user);
            JsonNode customers = json(mockMvc.perform(get("/api/customers").param("branchId", String.valueOf(mtj)).param("size", "100")
                    .header(HttpHeaders.AUTHORIZATION, auth)));
            customers.get("content").forEach(c -> assertThat(c.get("branch").get("code").asText()).isEqualTo("DHK-GUL"));

            JsonNode leads = json(mockMvc.perform(get("/api/leads").param("branchId", String.valueOf(mtj))
                    .param("officerId", String.valueOf(nusrat)).param("size", "100").header(HttpHeaders.AUTHORIZATION, auth)));
            leads.get("content").forEach(l -> assertThat(l.get("branch").get("code").asText()).isEqualTo("DHK-GUL"));

            String own = mockMvc.perform(get("/api/dashboard/summary").header(HttpHeaders.AUTHORIZATION, auth))
                    .andReturn().getResponse().getContentAsString();
            String tampered = mockMvc.perform(get("/api/dashboard/summary").param("branchId", String.valueOf(mtj))
                    .header(HttpHeaders.AUTHORIZATION, auth)).andReturn().getResponse().getContentAsString();
            assertThat(tampered).isEqualTo(own);

            JsonNode report = json(mockMvc.perform(get("/api/reports/officer-performance").param("branchId", String.valueOf(mtj))
                    .param("size", "100").header(HttpHeaders.AUTHORIZATION, auth)));
            report.get("rows").forEach(r -> assertThat(r.get("branch").asText()).isEqualTo("Gulshan Branch"));

            String csv = mockMvc.perform(get("/api/customers/export").param("branchId", String.valueOf(mtj))
                    .header(HttpHeaders.AUTHORIZATION, auth)).andReturn().getResponse().getContentAsString();
            assertThat(csv).doesNotContain("Motijheel");

            JsonNode staff = json(mockMvc.perform(get("/api/users/assignable").param("branchId", String.valueOf(mtj))
                    .header(HttpHeaders.AUTHORIZATION, auth)));
            staff.forEach(u -> assertThat(u.get("branch").get("code").asText()).isEqualTo("DHK-GUL"));
        }
        // An officer asking for a colleague's data still only gets their own.
        long tanvir = id("SELECT id FROM users WHERE email = '" + OFFICER2_MTJ + "'");
        JsonNode mine = json(mockMvc.perform(get("/api/reports/officer-performance").param("officerId", String.valueOf(tanvir))
                .header(HttpHeaders.AUTHORIZATION, token(OFFICER_MTJ))));
        assertThat(mine.get("rows")).isNotEmpty();
        mine.get("rows").forEach(r -> assertThat(r.get("officer").asText()).isEqualTo("Nusrat Jahan"));
    }

    @Test
    @DisplayName("every protected endpoint needs a valid token")
    void anonymousAndTamperedTokens() throws Exception {
        for (String url : List.of("/api/customers", "/api/leads", "/api/follow-ups", "/api/sales", "/api/notifications",
                "/api/dashboard/summary", "/api/reports/conversion", "/api/customers/export", "/api/users", "/api/branches")) {
            assertThat(mockMvc.perform(get(url)).andReturn().getResponse().getStatus()).as(url).isEqualTo(401);
        }
        String good = token(OFFICER_MTJ);
        String forged = good.substring(0, good.length() - 4) + (good.endsWith("AAAA") ? "BBBB" : "AAAA");
        assertThat(mockMvc.perform(get("/api/customers").header(HttpHeaders.AUTHORIZATION, forged))
                .andReturn().getResponse().getStatus()).isEqualTo(401);
    }
}
