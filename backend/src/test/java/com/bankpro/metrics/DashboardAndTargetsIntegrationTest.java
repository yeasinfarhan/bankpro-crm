package com.bankpro.metrics;

import com.bankpro.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Every figure is checked against an independent SQL query on the same database. */
class DashboardAndTargetsIntegrationTest extends IntegrationTest {

    private static final ZoneId BST = ZoneId.of("Asia/Dhaka");

    @Autowired
    private JdbcTemplate jdbc;

    private long id(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private BigDecimal dec(String sql, Object... args) {
        BigDecimal v = jdbc.queryForObject(sql, BigDecimal.class, args);
        return v == null ? BigDecimal.ZERO : v;
    }

    private String[] utcBounds(LocalDate from, LocalDate to) {
        var f = from.atStartOfDay(BST).toInstant().toString().replace("T", " ").replace("Z", "");
        var t = to.plusDays(1).atStartOfDay(BST).toInstant().toString().replace("T", " ").replace("Z", "");
        return new String[]{f, t};
    }

    /** Converts a fresh lead for the officer and returns the sale amount used. */
    private void sellDeposit(String token, BigDecimal amount) throws Exception {
        String phone = uniquePhone();
        long customerId = json(mockMvc.perform(post("/api/customers").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("fullName", "Target Test", "phone", phone, "segment", "MASS")))))
                .get("id").asLong();
        long productId = id("SELECT id FROM banking_products WHERE code = 'FDR-1Y'");
        long leadId = json(mockMvc.perform(post("/api/leads").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of(
                        "customerId", customerId, "productId", productId, "source", "REFERRAL", "estimatedValue", amount)))))
                .get("id").asLong();
        for (String s : new String[]{"CONTACTED", "QUALIFIED"}) {
            mockMvc.perform(patch("/api/leads/" + leadId + "/status").header(HttpHeaders.AUTHORIZATION, token)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + s + "\"}")).andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/leads/" + leadId + "/convert").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":" + amount + "}")).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("manager KPIs equal independent SQL over the branch; officer KPIs only cover their own data")
    void kpisMatchDatabase() throws Exception {
        LocalDate today = LocalDate.now(BST);
        LocalDate from = today.minusMonths(6).withDayOfMonth(1);
        String[] b = utcBounds(from, today);
        long branch = id("SELECT id FROM branches WHERE code = 'DHK-MTJ'");

        JsonNode k = json(mockMvc.perform(get("/api/dashboard/summary").param("from", from.toString()).param("to", today.toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_MTJ))).andExpect(status().isOk()));

        assertThat(k.get("totalCustomers").asLong()).isEqualTo(
                id("SELECT COUNT(*) FROM customers WHERE branch_id = ? AND status <> 'ARCHIVED'", branch));
        assertThat(k.get("activeLeads").asLong()).isEqualTo(
                id("SELECT COUNT(*) FROM leads WHERE branch_id = ? AND status IN ('NEW','CONTACTED','QUALIFIED','PROPOSAL')", branch));
        assertThat(k.get("leadsConverted").asLong()).isEqualTo(
                id("SELECT COUNT(*) FROM sales WHERE branch_id = ? AND status <> 'DECLINED' AND converted_at >= ? AND converted_at < ?", branch, b[0], b[1]));
        assertThat(k.get("totalSalesValue").decimalValue()).isEqualByComparingTo(dec(
                "SELECT SUM(amount) FROM sales WHERE branch_id = ? AND status = 'COMPLETED' AND COALESCE(decided_at, converted_at) >= ? AND COALESCE(decided_at, converted_at) < ?",
                branch, b[0], b[1]));
        assertThat(k.get("depositSalesValue").decimalValue()).isEqualByComparingTo(dec(
                "SELECT SUM(s.amount) FROM sales s JOIN banking_products p ON p.id = s.product_id WHERE s.branch_id = ? AND s.status = 'COMPLETED' AND p.category = 'DEPOSIT' AND COALESCE(s.decided_at, s.converted_at) >= ? AND COALESCE(s.decided_at, s.converted_at) < ?",
                branch, b[0], b[1]));
        assertThat(k.get("overdueFollowUps").asLong()).isEqualTo(
                id("SELECT COUNT(*) FROM follow_up_activities WHERE branch_id = ? AND status = 'SCHEDULED' AND scheduled_at < UTC_TIMESTAMP(6)", branch));
        assertThat(k.get("totalSalesValue").decimalValue()).as("pending/declined are never counted")
                .isLessThanOrEqualTo(dec("SELECT SUM(amount) FROM sales WHERE branch_id = ?", branch));

        long nusrat = id("SELECT id FROM users WHERE email = ?", OFFICER_MTJ);
        JsonNode mine = json(mockMvc.perform(get("/api/dashboard/summary").param("from", from.toString()).param("to", today.toString())
                .param("branchId", String.valueOf(id("SELECT id FROM branches WHERE code = 'DHK-GUL'"))) // ignored for officers
                .header(HttpHeaders.AUTHORIZATION, bearer(OFFICER_MTJ))));
        assertThat(mine.get("activeLeads").asLong()).isEqualTo(
                id("SELECT COUNT(*) FROM leads WHERE assigned_officer_id = ? AND status IN ('NEW','CONTACTED','QUALIFIED','PROPOSAL')", nusrat));
        assertThat(mine.get("totalCustomers").asLong()).isEqualTo(
                id("SELECT COUNT(*) FROM customers WHERE assigned_officer_id = ? AND status <> 'ARCHIVED'", nusrat));
    }

    @Test
    @DisplayName("charts: zero-filled months, all funnel stages, and scoped officer performance")
    void chartSeries() throws Exception {
        LocalDate today = LocalDate.now(BST);
        String manager = bearer(MANAGER_MTJ);
        JsonNode trend = json(mockMvc.perform(get("/api/dashboard/sales-trend").header(HttpHeaders.AUTHORIZATION, manager)));
        assertThat(trend).hasSize(6);
        assertThat(trend.get(5).get("month").asText()).isEqualTo(YearMonth.from(today).toString());

        JsonNode funnel = json(mockMvc.perform(get("/api/dashboard/lead-funnel").header(HttpHeaders.AUTHORIZATION, manager)));
        assertThat(funnel).extracting(n -> n.get("stage").asText())
                .containsExactly("NEW", "CONTACTED", "QUALIFIED", "PROPOSAL", "CONVERTED", "LOST");

        mockMvc.perform(get("/api/dashboard/officer-performance").header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].branch", everyItem(is("Motijheel Branch"))));
        mockMvc.perform(get("/api/dashboard/officer-performance").header(HttpHeaders.AUTHORIZATION, bearer(OFFICER_MTJ)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard/summary").param("from", "2026-10-10").param("to", "2026-10-01")
                .header(HttpHeaders.AUTHORIZATION, manager)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("targets: a completed sale moves actual, achievement % and remaining by exactly its amount")
    void targetCalculation() throws Exception {
        String manager = bearer(MANAGER_MTJ);
        long tanvir = id("SELECT id FROM users WHERE email = ?", OFFICER2_MTJ);
        // Use a future month so seeded data cannot interfere; dates are inside the period only via our sale below.
        LocalDate thisMonth = LocalDate.now(BST).withDayOfMonth(1);
        jdbc.update("DELETE FROM sales_targets WHERE user_id = ? AND period_type = 'MONTHLY' AND period_start = ? AND category = 'DEPOSIT_SALES'",
                tanvir, thisMonth);

        Map<String, Object> body = new HashMap<>(Map.of("userId", tanvir, "periodType", "MONTHLY",
                "periodStart", thisMonth.plusDays(9).toString(), "category", "DEPOSIT_SALES", "targetValue", 1000000));
        JsonNode created = json(mockMvc.perform(post("/api/targets").header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.periodStart").value(thisMonth.toString())));
        long targetId = created.get("id").asLong();
        BigDecimal before = created.get("actual").decimalValue();

        sellDeposit(bearer(OFFICER2_MTJ), new BigDecimal("250000"));

        JsonNode after = json(mockMvc.perform(get("/api/targets").param("from", thisMonth.toString())
                        .param("to", thisMonth.plusMonths(1).minusDays(1).toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(OFFICER2_MTJ))));
        JsonNode t = null;
        for (JsonNode n : after) if (n.get("id").asLong() == targetId) t = n;
        assertThat(t).isNotNull();
        BigDecimal actual = t.get("actual").decimalValue();
        assertThat(actual.subtract(before)).isEqualByComparingTo("250000");
        assertThat(t.get("achievementPct").decimalValue()).isEqualByComparingTo(
                actual.multiply(BigDecimal.valueOf(100)).divide(new BigDecimal("1000000"), 1, java.math.RoundingMode.HALF_UP));
        assertThat(t.get("remaining").decimalValue()).isEqualByComparingTo(new BigDecimal("1000000").subtract(actual).max(BigDecimal.ZERO));

        // duplicate, invalid values, permissions
        mockMvc.perform(post("/api/targets").header(HttpHeaders.AUTHORIZATION, manager)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body))).andExpect(status().isConflict());
        body.put("targetValue", 0);
        mockMvc.perform(post("/api/targets").header(HttpHeaders.AUTHORIZATION, manager)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body))).andExpect(status().isBadRequest());
        Map<String, Object> fractional = new HashMap<>(body);
        fractional.put("category", "NEW_CUSTOMERS");
        fractional.put("targetValue", 2.5);
        mockMvc.perform(post("/api/targets").header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(fractional)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("targetValue"));
        Map<String, Object> valid = new HashMap<>(body);
        valid.put("targetValue", 500000);
        mockMvc.perform(post("/api/targets").header(HttpHeaders.AUTHORIZATION, bearer(OFFICER_MTJ))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(valid))).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/targets/" + targetId).header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_GUL))
                .contentType(MediaType.APPLICATION_JSON).content("{\"targetValue\":5}")).andExpect(status().isNotFound());

        // ranking & clean-up
        mockMvc.perform(get("/api/targets/ranking").param("from", thisMonth.toString()).param("to", thisMonth.plusMonths(1).minusDays(1).toString())
                .header(HttpHeaders.AUTHORIZATION, manager)).andExpect(status().isOk()).andExpect(jsonPath("$[0].rank").value(1));
        mockMvc.perform(delete("/api/targets/" + targetId).header(HttpHeaders.AUTHORIZATION, manager)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("achievement % handles missing/zero targets without dividing by zero")
    void achievementGuard() {
        assertThat(com.bankpro.service.TargetService.achievement(new BigDecimal("50"), new BigDecimal("200"))).isEqualByComparingTo("25.0");
        assertThat(com.bankpro.service.TargetService.achievement(new BigDecimal("300"), new BigDecimal("200"))).isEqualByComparingTo("150.0");
        assertThat(com.bankpro.service.TargetService.achievement(BigDecimal.TEN, BigDecimal.ZERO)).isNull();
        assertThat(com.bankpro.service.TargetService.achievement(BigDecimal.TEN, null)).isNull();
    }
}
