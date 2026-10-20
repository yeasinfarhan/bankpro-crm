package com.bankpro.followup;

import com.bankpro.service.OverdueFollowUpNotifier;
import com.bankpro.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FollowUpIntegrationTest extends IntegrationTest {

    private static final ZoneOffset BST = ZoneOffset.ofHours(6);

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OverdueFollowUpNotifier overdueNotifier;

    private long newCustomer(String token) throws Exception {
        Map<String, Object> body = Map.of("fullName", "Follow-up Test Customer", "phone", uniquePhone(), "segment", "MASS");
        return json(mockMvc.perform(post("/api/customers").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))).get("id").asLong();
    }

    private org.springframework.test.web.servlet.ResultActions schedule(String token, long customerId, OffsetDateTime when)
            throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("customerId", customerId, "activityType", "PHONE_CALL",
                "scheduledAt", when.toString(), "notes", "Discuss savings"));
        return mockMvc.perform(post("/api/follow-ups").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)));
    }

    @Test
    @DisplayName("schedule, then complete: the outcome is written to the customer's history in BST-aware views")
    void scheduleAndComplete() throws Exception {
        String officer = bearer(OFFICER_MTJ);
        long customerId = newCustomer(officer);
        OffsetDateTime inTwoDays = OffsetDateTime.now(BST).plusDays(2).withHour(11).truncatedTo(ChronoUnit.HOURS);
        JsonNode f = json(schedule(officer, customerId, inTwoDays).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.overdue").value(false)));
        long id = f.get("id").asLong();

        mockMvc.perform(get("/api/follow-ups").param("view", "UPCOMING").param("customerId", String.valueOf(customerId))
                        .header(HttpHeaders.AUTHORIZATION, officer))
                .andExpect(jsonPath("$.content[*].id", hasItem((int) id)));
        mockMvc.perform(get("/api/follow-ups").param("view", "TODAY").param("customerId", String.valueOf(customerId))
                        .header(HttpHeaders.AUTHORIZATION, officer))
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(patch("/api/follow-ups/" + id + "/complete").header(HttpHeaders.AUTHORIZATION, officer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"outcome\":\"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/follow-ups/" + id + "/complete").header(HttpHeaders.AUTHORIZATION, officer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"outcome\":\"Interested in DPS\",\"notes\":\"Call back next month\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
        mockMvc.perform(patch("/api/follow-ups/" + id + "/complete").header(HttpHeaders.AUTHORIZATION, officer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"outcome\":\"again\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/customers/" + customerId + "/timeline").header(HttpHeaders.AUTHORIZATION, officer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kind").value("INTERACTION"))
                .andExpect(jsonPath("$[0].detail").value("Interested in DPS — Call back next month"));
        assertThat(jdbc.queryForObject("SELECT last_interaction_at IS NOT NULL FROM customers WHERE id = ?", Boolean.class, customerId))
                .isTrue();
    }

    @Test
    @DisplayName("scheduling in the past is rejected; rescheduling moves the activity")
    void pastAndReschedule() throws Exception {
        String officer = bearer(OFFICER_MTJ);
        long customerId = newCustomer(officer);
        schedule(officer, customerId, OffsetDateTime.now(BST).minusDays(1)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("scheduledAt"));

        long id = json(schedule(officer, customerId, OffsetDateTime.now(BST).plusDays(1))).get("id").asLong();
        OffsetDateTime later = OffsetDateTime.now(BST).plusDays(7).withHour(15).truncatedTo(ChronoUnit.HOURS);
        mockMvc.perform(patch("/api/follow-ups/" + id + "/reschedule").header(HttpHeaders.AUTHORIZATION, officer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"scheduledAt\":\"" + later + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduledAt").value(later.toInstant().toString()));
    }

    @Test
    @DisplayName("officers cannot see or change a colleague's follow-ups; managers can manage their branch")
    void ownership() throws Exception {
        String tanvir = bearer(OFFICER2_MTJ);
        long customerId = newCustomer(tanvir);
        long id = json(schedule(tanvir, customerId, OffsetDateTime.now(BST).plusDays(1))).get("id").asLong();

        String nusrat = bearer(OFFICER_MTJ);
        mockMvc.perform(get("/api/follow-ups").param("view", "UPCOMING").param("size", "200").header(HttpHeaders.AUTHORIZATION, nusrat))
                .andExpect(jsonPath("$.content[*].id", not(hasItem((int) id))))
                .andExpect(jsonPath("$.content[*].assignedOfficer.fullName", everyItem(is("Nusrat Jahan"))));
        mockMvc.perform(patch("/api/follow-ups/" + id + "/complete").header(HttpHeaders.AUTHORIZATION, nusrat)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"outcome\":\"hijack\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/follow-ups/" + id + "/complete").header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_GUL))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"outcome\":\"hijack\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/follow-ups/" + id + "/complete").header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_MTJ))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"outcome\":\"Done by manager\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/follow-ups").header(HttpHeaders.AUTHORIZATION, bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of(
                                "customerId", customerId, "activityType", "EMAIL",
                                "scheduledAt", OffsetDateTime.now(BST).plusDays(1).toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("overdue follow-ups are counted and notified exactly once")
    void overdueNotifications() throws Exception {
        String officer = bearer(OFFICER_MTJ);
        long customerId = newCustomer(officer);
        long id = json(schedule(officer, customerId, OffsetDateTime.now(BST).plusHours(1))).get("id").asLong();
        jdbc.update("UPDATE follow_up_activities SET scheduled_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 HOUR) WHERE id = ?", id);

        mockMvc.perform(get("/api/follow-ups").param("view", "OVERDUE").param("size", "200").header(HttpHeaders.AUTHORIZATION, officer))
                .andExpect(jsonPath("$.content[?(@.id == " + id + ")].overdue").value(hasItem(true)));
        JsonNode counts = json(mockMvc.perform(get("/api/follow-ups/counts").header(HttpHeaders.AUTHORIZATION, officer)));
        assertThat(counts.get("overdue").asLong()).isGreaterThanOrEqualTo(1);

        overdueNotifier.notifyOverdue();
        overdueNotifier.notifyOverdue();
        long nusratId = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, OFFICER_MTJ);
        Integer forThis = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND dedupe_key LIKE ?", Integer.class,
                nusratId, "FOLLOW_UP_OVERDUE:" + id + ":%");
        assertThat(forThis).as("deduplicated").isEqualTo(1);

        JsonNode inbox = json(mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, officer)));
        assertThat(inbox.get("unreadCount").asLong()).isGreaterThanOrEqualTo(1);
        mockMvc.perform(post("/api/notifications/read-all").header(HttpHeaders.AUTHORIZATION, officer))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, officer))
                .andExpect(jsonPath("$.unreadCount").value(0));

        long someoneElses = jdbc.queryForObject("SELECT id FROM notifications WHERE user_id <> ? LIMIT 1", Long.class, nusratId);
        mockMvc.perform(patch("/api/notifications/" + someoneElses + "/read").header(HttpHeaders.AUTHORIZATION, officer))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("interactions: logged by branch staff, never in the future, shown in the timeline with leads and sales")
    void interactionsAndTimeline() throws Exception {
        String officer = bearer(OFFICER_MTJ);
        long customerId = newCustomer(officer);
        mockMvc.perform(post("/api/customers/" + customerId + "/interactions").header(HttpHeaders.AUTHORIZATION, officer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interactionType\":\"NOTE\",\"summary\":\"Prefers calls after 5pm\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.actor").value("Nusrat Jahan"));
        mockMvc.perform(post("/api/customers/" + customerId + "/interactions").header(HttpHeaders.AUTHORIZATION, officer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interactionType\":\"NOTE\",\"summary\":\"x\",\"occurredAt\":\"" + OffsetDateTime.now(BST).plusDays(2) + "\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/customers/" + customerId + "/interactions").header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_GUL))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"interactionType\":\"NOTE\",\"summary\":\"x\"}"))
                .andExpect(status().isNotFound());

        // Seeded customer with a converted lead: the timeline shows lead, sale and history in date order
        long seeded = jdbc.queryForObject("SELECT customer_id FROM sales s JOIN users u ON u.id = s.officer_id WHERE u.email = ? AND s.status = 'COMPLETED' LIMIT 1",
                Long.class, OFFICER_MTJ);
        JsonNode timeline = json(mockMvc.perform(get("/api/customers/" + seeded + "/timeline").header(HttpHeaders.AUTHORIZATION, officer))
                .andExpect(status().isOk()));
        assertThat(timeline).extracting(n -> n.get("kind").asText()).contains("LEAD_CREATED", "SALE");
        for (int i = 1; i < timeline.size(); i++) {
            assertThat(timeline.get(i - 1).get("at").asText()).isGreaterThanOrEqualTo(timeline.get(i).get("at").asText());
        }
    }
}
