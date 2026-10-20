package com.bankpro.product;

import com.bankpro.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductIntegrationTest extends IntegrationTest {

    private String body(String code, String category, Object rate, Boolean active) throws Exception {
        var map = new java.util.HashMap<String, Object>(Map.of(
                "code", code, "name", "Test product " + code, "category", category,
                "description", "Fictional test product.", "benefits", java.util.List.of("Benefit one")));
        if (rate != null) map.put("illustrativeRate", rate);
        if (active != null) map.put("active", active);
        return objectMapper.writeValueAsString(map);
    }

    @Test
    @DisplayName("every role can browse the seeded active catalogue")
    void browse() throws Exception {
        mockMvc.perform(get("/api/products").header(HttpHeaders.AUTHORIZATION, bearer(OFFICER_MTJ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(8))))
                .andExpect(jsonPath("$[*].active", everyItem(is(true))));

        mockMvc.perform(get("/api/products").param("category", "DEPOSIT").header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_MTJ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].category", everyItem(is("DEPOSIT"))));
    }

    @Test
    @DisplayName("admin creates and deactivates a product; officers no longer see it")
    void adminManagesCatalogue() throws Exception {
        String admin = bearer(ADMIN);
        long id = json(mockMvc.perform(post("/api/products").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body("TST-FDR", "DEPOSIT", 7.25, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TST-FDR"))
                .andExpect(jsonPath("$.illustrativeRate").value(7.25))).get("id").asLong();

        mockMvc.perform(put("/api/products/" + id).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body("IGNORED", "DEPOSIT", 7.25, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.code").value("TST-FDR"));

        String officer = bearer(OFFICER_MTJ);
        mockMvc.perform(get("/api/products").header(HttpHeaders.AUTHORIZATION, officer))
                .andExpect(jsonPath("$[*].code", not(hasItem("TST-FDR"))));
        mockMvc.perform(get("/api/products/" + id).header(HttpHeaders.AUTHORIZATION, officer))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/products").param("active", "false").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$[*].code", hasItem("TST-FDR")));
    }

    @Test
    @DisplayName("product rules: unique code, rates only for deposit/account, admin-only writes, validation")
    void productRules() throws Exception {
        String admin = bearer(ADMIN);
        mockMvc.perform(post("/api/products").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body("SAV-STD", "ACCOUNT", null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("code"));

        mockMvc.perform(post("/api/products").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body("TST-LOAN", "LOAN", 12.5, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("illustrativeRate"));

        mockMvc.perform(post("/api/products").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"bad code\",\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'code')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'category')]").exists());

        mockMvc.perform(post("/api/products").header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_MTJ))
                        .contentType(MediaType.APPLICATION_JSON).content(body("TST-MGR", "ACCOUNT", null, null)))
                .andExpect(status().isForbidden());
    }
}
