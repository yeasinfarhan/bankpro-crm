package com.bankpro.metrics;

import com.bankpro.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportIntegrationTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"customer-acquisition", "lead-pipeline", "conversion", "product-sales",
            "officer-performance", "target-achievement", "follow-up-completion"})
    @DisplayName("every report runs for a manager with columns, summary, chart and pagination")
    void reportsRun(String type) throws Exception {
        mockMvc.perform(get("/api/reports/" + type).param("size", "2").header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_MTJ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns").isNotEmpty())
                .andExpect(jsonPath("$.summary").isNotEmpty())
                .andExpect(jsonPath("$.chart.xKey").isNotEmpty())
                .andExpect(jsonPath("$.size").value(2));
    }

    @Test
    @DisplayName("officers only get their own row; managers only their branch; unknown reports are 404")
    void reportScope() throws Exception {
        mockMvc.perform(get("/api/reports/officer-performance").header(HttpHeaders.AUTHORIZATION, bearer(OFFICER_MTJ)))
                .andExpect(jsonPath("$.rows[*].officer", everyItem(is("Nusrat Jahan"))));
        mockMvc.perform(get("/api/reports/officer-performance").param("size", "100")
                        .param("branchId", String.valueOf(jdbc.queryForObject("SELECT id FROM branches WHERE code = 'DHK-GUL'", Long.class)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_MTJ)))
                .andExpect(jsonPath("$.rows[*].branch", everyItem(is("Motijheel Branch"))));
        mockMvc.perform(get("/api/reports/salaries").header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_MTJ)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("product-sales totals match SQL, and CSV export neutralises formulas in data")
    void productSalesAndCsv() throws Exception {
        // A product whose name looks like a spreadsheet formula
        String admin = bearer(ADMIN);
        long productId = json(mockMvc.perform(post("/api/products").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of(
                        "code", "TST-CSV", "name", "=HYPERLINK(\"http://evil.example\",\"x\")", "category", "DEPOSIT",
                        "description", "Fictional test product."))))).get("id").asLong();
        String officer = bearer(OFFICER_MTJ);
        long customerId = json(mockMvc.perform(post("/api/customers").header(HttpHeaders.AUTHORIZATION, officer)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of(
                        "fullName", "Csv Buyer", "phone", uniquePhone(), "segment", "MASS"))))).get("id").asLong();
        long leadId = json(mockMvc.perform(post("/api/leads").header(HttpHeaders.AUTHORIZATION, officer)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of(
                        "customerId", customerId, "productId", productId, "source", "CAMPAIGN", "estimatedValue", 1000))))).get("id").asLong();
        for (String s : new String[]{"CONTACTED", "QUALIFIED"}) {
            mockMvc.perform(patch("/api/leads/" + leadId + "/status").header(HttpHeaders.AUTHORIZATION, officer)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + s + "\"}"));
        }
        mockMvc.perform(post("/api/leads/" + leadId + "/convert").header(HttpHeaders.AUTHORIZATION, officer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":1000}")).andExpect(status().isCreated());

        String manager = bearer(MANAGER_MTJ);
        JsonNode report = json(mockMvc.perform(get("/api/reports/product-sales").param("size", "100").header(HttpHeaders.AUTHORIZATION, manager)));
        long branch = jdbc.queryForObject("SELECT id FROM branches WHERE code = 'DHK-MTJ'", Long.class);
        long pendingSql = jdbc.queryForObject("SELECT COUNT(*) FROM sales WHERE branch_id = ? AND status = 'PENDING_APPROVAL' AND converted_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 7 MONTH)", Long.class, branch);
        long completedRows = 0;
        for (JsonNode r : report.get("rows")) completedRows += r.get("completedCount").asLong();
        assertThat(report.get("summary").get(0).get("value").asLong()).isEqualTo(completedRows);
        assertThat(report.get("summary").get(2).get("value").asLong()).isLessThanOrEqualTo(pendingSql);

        String csv = mockMvc.perform(get("/api/reports/product-sales/export").header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).contains("\"'=HYPERLINK(").doesNotContain(",=HYPERLINK").contains("Product,Category,Completed");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE action = 'REPORT_EXPORTED'", Integer.class)).isPositive();
    }
}
