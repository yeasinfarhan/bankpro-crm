package com.bankpro.customer;

import com.bankpro.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerIntegrationTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    private Map<String, Object> payload(String name, String phone, String email) {
        Map<String, Object> m = new HashMap<>();
        m.put("fullName", name);
        m.put("phone", phone);
        m.put("email", email);
        m.put("occupation", "Test Occupation");
        m.put("employmentType", "SALARIED");
        m.put("incomeRange", "FROM_30K_TO_60K");
        m.put("city", "Dhaka");
        m.put("segment", "MASS");
        return m;
    }

    private JsonNode create(String token, Map<String, Object> body) throws Exception {
        return json(mockMvc.perform(post("/api/customers").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated()));
    }

    private long productId(String code) {
        return jdbc.queryForObject("SELECT id FROM banking_products WHERE code = ?", Long.class, code);
    }

    private long userId(String email) {
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    @Test
    @DisplayName("an officer creates a customer: contact details normalised, owned by the officer, code derived from id")
    void officerCreatesCustomer() throws Exception {
        String officer = bearer(OFFICER_MTJ);
        String local = uniquePhone();
        JsonNode c = create(officer, payload("  Test   Customer  ", local.substring(0, 5) + "-" + local.substring(5),
                " Test.Customer." + local + "@Example.COM "));

        assertThat(c.get("fullName").asText()).isEqualTo("Test Customer");
        assertThat(c.get("phone").asText()).isEqualTo("+88" + local);
        assertThat(c.get("email").asText()).isEqualTo("test.customer." + local + "@example.com");
        assertThat(c.get("status").asText()).isEqualTo("PROSPECT");
        assertThat(c.get("assignedOfficer").get("fullName").asText()).isEqualTo("Nusrat Jahan");
        assertThat(c.get("branch").get("code").asText()).isEqualTo("DHK-MTJ");
        assertThat(c.get("code").asText()).isEqualTo(String.format("CUS-%06d", c.get("id").asLong()));
        assertThat(c.get("permissions").get("canEdit").asBoolean()).isTrue();
        assertThat(c.get("permissions").get("canArchive").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("duplicates are detected after normalisation; invalid phones are rejected")
    void duplicatesAndValidation() throws Exception {
        String officer = bearer(OFFICER_MTJ);
        String local = uniquePhone();
        create(officer, payload("Original Person", local, "dup." + local + "@example.com"));

        Map<String, Object> samePhone = payload("Other Person", "+88" + local, null);
        mockMvc.perform(post("/api/customers").header(HttpHeaders.AUTHORIZATION, bearer(OFFICER_GUL))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(samePhone)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("phone"));

        Map<String, Object> sameEmail = payload("Other Person", uniquePhone(), "DUP." + local + "@EXAMPLE.com");
        mockMvc.perform(post("/api/customers").header(HttpHeaders.AUTHORIZATION, officer)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(sameEmail)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));

        mockMvc.perform(post("/api/customers").header(HttpHeaders.AUTHORIZATION, officer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload("Bad Phone", "12345", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("phone"));

        mockMvc.perform(post("/api/customers").header(HttpHeaders.AUTHORIZATION, officer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"\",\"email\":\"nope\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'fullName')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'segment')]").exists());
    }

    @Test
    @DisplayName("branch isolation: lists only show the caller's branch; other branches' records are 404")
    void branchIsolation() throws Exception {
        mockMvc.perform(get("/api/customers").param("size", "100").header(HttpHeaders.AUTHORIZATION, bearer(OFFICER_MTJ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].branch.code", everyItem(is("DHK-MTJ"))));

        long gulshanCustomer = jdbc.queryForObject(
                "SELECT c.id FROM customers c JOIN branches b ON b.id = c.branch_id WHERE b.code = 'DHK-GUL' LIMIT 1", Long.class);
        mockMvc.perform(get("/api/customers/" + gulshanCustomer).header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_MTJ)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/customers/" + gulshanCustomer).header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_MTJ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload("Hijack", uniquePhone(), null))))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/customers/" + gulshanCustomer).header(HttpHeaders.AUTHORIZATION, bearer(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canEdit").value(false));

        // ADMIN is read-only for branch business data
        mockMvc.perform(post("/api/customers").header(HttpHeaders.AUTHORIZATION, bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload("Admin Made", uniquePhone(), null))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ownership: officers edit only their own customers; stale versions are rejected")
    void ownershipAndOptimisticLocking() throws Exception {
        JsonNode mine = create(bearer(OFFICER2_MTJ), payload("Tanvir Customer", uniquePhone(), null));
        long id = mine.get("id").asLong();

        Map<String, Object> edit = payload("Edited Name", mine.get("phone").asText(), null);
        edit.put("version", mine.get("version").asLong());

        mockMvc.perform(put("/api/customers/" + id).header(HttpHeaders.AUTHORIZATION, bearer(OFFICER_MTJ))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(edit)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/customers/" + id).header(HttpHeaders.AUTHORIZATION, bearer(OFFICER_MTJ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canEdit").value(false));

        String owner = bearer(OFFICER2_MTJ);
        JsonNode updated = json(mockMvc.perform(put("/api/customers/" + id).header(HttpHeaders.AUTHORIZATION, owner)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(edit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Edited Name")));
        assertThat(updated.get("version").asLong()).isGreaterThan(mine.get("version").asLong());

        // Re-sending the old version = someone else changed it meanwhile
        mockMvc.perform(put("/api/customers/" + id).header(HttpHeaders.AUTHORIZATION, owner)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(edit)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("managers assign within their branch only; holding products makes a customer ACTIVE")
    void assignmentAndHoldings() throws Exception {
        String manager = bearer(MANAGER_MTJ);
        Map<String, Object> body = payload("Assigned Customer", uniquePhone(), null);
        body.put("assignedOfficerId", userId(OFFICER2_MTJ));
        body.put("productIds", List.of(productId("SAV-STD"), productId("DPS-STD")));
        JsonNode c = create(manager, body);
        assertThat(c.get("assignedOfficer").get("fullName").asText()).isEqualTo("Tanvir Ahmed");
        assertThat(c.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(c.get("products")).hasSize(2);

        Map<String, Object> wrongBranch = payload("Wrong Branch", uniquePhone(), null);
        wrongBranch.put("assignedOfficerId", userId(OFFICER_GUL));
        mockMvc.perform(post("/api/customers").header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(wrongBranch)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("assignedOfficerId"));
    }

    @Test
    @DisplayName("archive is manager-only, hides the customer by default, and can be restored")
    void archiveAndRestore() throws Exception {
        JsonNode c = create(bearer(OFFICER_MTJ), payload("To Archive", uniquePhone(), null));
        long id = c.get("id").asLong();

        mockMvc.perform(delete("/api/customers/" + id).header(HttpHeaders.AUTHORIZATION, bearer(OFFICER_MTJ)))
                .andExpect(status().isForbidden());

        String manager = bearer(MANAGER_MTJ);
        mockMvc.perform(delete("/api/customers/" + id).header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM customers WHERE id = ?", Integer.class, id))
                .as("soft delete keeps the row").isEqualTo(1);

        mockMvc.perform(get("/api/customers").param("q", c.get("code").asText()).header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/customers").param("q", c.get("code").asText()).param("includeArchived", "true")
                        .header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(jsonPath("$.content[0].status").value("ARCHIVED"));

        mockMvc.perform(post("/api/customers/" + id + "/restore").header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROSPECT"));
    }

    @Test
    @DisplayName("search by code, name and phone digits; server-side paging and safe sorting")
    void searchPagingSorting() throws Exception {
        String officer = bearer(OFFICER_MTJ);
        String local = uniquePhone();
        JsonNode c = create(officer, payload("Zubaida Searchable", local, null));

        mockMvc.perform(get("/api/customers").param("q", c.get("code").asText()).header(HttpHeaders.AUTHORIZATION, officer))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/customers").param("q", "zubaida search").header(HttpHeaders.AUTHORIZATION, officer))
                .andExpect(jsonPath("$.content[*].id", hasItem(c.get("id").asInt())));
        mockMvc.perform(get("/api/customers").param("q", local.substring(3)).header(HttpHeaders.AUTHORIZATION, officer))
                .andExpect(jsonPath("$.content[*].id", hasItem(c.get("id").asInt())));
        mockMvc.perform(get("/api/customers").param("q", "100%_\\").header(HttpHeaders.AUTHORIZATION, officer))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/customers").param("size", "5").param("page", "1").param("sort", "fullName,asc")
                        .header(HttpHeaders.AUTHORIZATION, officer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.page").value(1));
        mockMvc.perform(get("/api/customers").param("size", "5000").header(HttpHeaders.AUTHORIZATION, officer))
                .andExpect(jsonPath("$.size").value(100));
        mockMvc.perform(get("/api/customers").param("sort", "passwordHash,asc").header(HttpHeaders.AUTHORIZATION, officer))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CSV export is scoped (officers: own customers) and neutralises spreadsheet formulas")
    void csvExport() throws Exception {
        String officer = bearer(OFFICER_MTJ);
        create(officer, payload("=HYPERLINK(\"http://evil.example\",\"Click\")", uniquePhone(), null));

        String csv = mockMvc.perform(get("/api/customers/export").header(HttpHeaders.AUTHORIZATION, officer))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("text/csv")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, matchesPattern("attachment; filename=\"bankpro-customers-.*\\.csv\"")))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(csv).contains("\"'=HYPERLINK(\"\"http://evil.example\"\",\"\"Click\"\")\"");
        assertThat(csv).doesNotContain(",=HYPERLINK");
        assertThat(csv).contains("Nusrat Jahan").doesNotContain("Tanvir Ahmed").doesNotContain("Imran Hossain");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE action = 'CUSTOMERS_EXPORTED'", Integer.class))
                .isGreaterThan(0);

        String managerCsv = mockMvc.perform(get("/api/customers/export").header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_MTJ)))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(managerCsv).contains("Tanvir Ahmed").doesNotContain("Gulshan Branch");
    }

    @Test
    @DisplayName("assignable staff list is limited to the caller's branch")
    void assignableStaff() throws Exception {
        mockMvc.perform(get("/api/users/assignable").header(HttpHeaders.AUTHORIZATION, bearer(OFFICER_MTJ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].branch.code", everyItem(is("DHK-MTJ"))))
                .andExpect(jsonPath("$[*].email", not(hasItem("former.officer@bankpro.demo"))));
    }
}
