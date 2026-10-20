package com.bankpro.auth;

import com.bankpro.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Role-based access and branch isolation, tested through real HTTP requests and real tokens. */
class AuthorizationIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("ADMIN sees every employee across all branches")
    void adminSeesAllUsers() throws Exception {
        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)));
    }

    @Test
    @DisplayName("a BRANCH_MANAGER only sees their own branch's team")
    void managerSeesOnlyOwnBranch() throws Exception {
        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_MTJ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].branch.code", everyItem(is("DHK-MTJ"))))
                .andExpect(jsonPath("$[*].fullName",
                        containsInAnyOrder("Kamal Hossain", "Nusrat Jahan", "Tanvir Ahmed", "Sabbir Khan")));

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_GUL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].branch.code", everyItem(is("DHK-GUL"))))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("a RELATIONSHIP_OFFICER is forbidden from team management")
    void officerForbiddenFromTeam() throws Exception {
        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(OFFICER_MTJ)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to perform this action."));
    }

    @Test
    @DisplayName("branch list: ADMIN gets all branches, others only their own")
    void branchVisibility() throws Exception {
        mockMvc.perform(get("/api/branches").header(HttpHeaders.AUTHORIZATION, bearer(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        mockMvc.perform(get("/api/branches").header(HttpHeaders.AUTHORIZATION, bearer(OFFICER_MTJ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code").value("DHK-MTJ"));
    }

    @Test
    @DisplayName("CORS: the configured frontend origin is allowed, other origins are refused")
    void corsAllowList() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));

        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }
}
