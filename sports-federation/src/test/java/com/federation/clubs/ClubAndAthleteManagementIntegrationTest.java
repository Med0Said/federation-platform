package com.federation.clubs;

import com.fasterxml.jackson.databind.JsonNode;
import com.federation.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Clubs, athletes and events — management permissions")
class ClubAndAthleteManagementIntegrationTest extends ApiTestSupport {

    private Map<String, Object> clubBody(String name, String license) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("shortName", "tst");
        body.put("licenseNumber", license);
        body.put("city", "Bizerte");
        return body;
    }

    @Test
    @DisplayName("federation staff can create clubs (was blocked by a URL rule before)")
    void staffCreatesClub() throws Exception {
        String name = unique("Club Athlétique");
        MvcResult res = mockMvc.perform(as(STAFF, jsonBody(post("/clubs"),
                clubBody(name, "CLB-" + UUID.randomUUID().toString().substring(0, 8)))))
                .andExpect(status().isCreated()).andReturn();
        JsonNode club = data(res);
        assertThat(club.path("slug").asText()).startsWith("club-athletique-");
        assertThat(club.path("shortName").asText()).isEqualTo("TST");
        assertThat(club.path("country").asText()).isEqualTo("TN");
        assertThat(club.path("status").asText()).isEqualTo("ACTIVE");

        mockMvc.perform(as(MANAGER_EST, jsonBody(post("/clubs"), clubBody(unique("X"), "CLB-X-" + System.nanoTime()))))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(STAFF, delete("/clubs/" + club.path("id").asText()))).andExpect(status().isForbidden());
        mockMvc.perform(as(ADMIN, delete("/clubs/" + club.path("id").asText()))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("club managers edit their own club profile only, never federation-controlled fields")
    void managerEditsOwnClub() throws Exception {
        JsonNode est = data(mockMvc.perform(get("/clubs/" + EST_CLUB)).andReturn());
        Map<String, Object> body = new HashMap<>();
        body.put("name", est.path("name").asText());
        body.put("licenseNumber", est.path("licenseNumber").asText());
        body.put("website", "https://est.example");

        mockMvc.perform(as(MANAGER_EST, jsonBody(put("/clubs/" + EST_CLUB), body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.website").value("https://est.example"));
        mockMvc.perform(as(MANAGER_CSS, jsonBody(put("/clubs/" + EST_CLUB), body))).andExpect(status().isForbidden());

        body.put("status", "SUSPENDED");
        mockMvc.perform(as(MANAGER_EST, jsonBody(put("/clubs/" + EST_CLUB), body))).andExpect(status().isForbidden());

        JsonNode mine = data(mockMvc.perform(as(MANAGER_EST, get("/clubs/mine"))).andReturn());
        assertThat(mine.findValuesAsText("id")).containsExactly(EST_CLUB);
    }

    @Test
    @DisplayName("club managers manage athletes of their club; transfers are staff-only")
    void managerManagesOwnAthletes() throws Exception {
        Map<String, Object> athlete = new HashMap<>();
        athlete.put("licenseNumber", "MGR-" + UUID.randomUUID().toString().substring(0, 10));
        athlete.put("firstName", "Sami");
        athlete.put("lastName", unique("Manager"));
        athlete.put("dateOfBirth", "2010-01-01");
        athlete.put("gender", "MALE");
        athlete.put("clubId", CSS_CLUB);

        mockMvc.perform(as(MANAGER_EST, jsonBody(post("/athletes"), athlete))).andExpect(status().isForbidden());

        athlete.put("clubId", EST_CLUB);
        JsonNode created = data(mockMvc.perform(as(MANAGER_EST, jsonBody(post("/athletes"), athlete)))
                .andExpect(status().isCreated()).andReturn());
        assertThat(created.path("category").asText()).isEqualTo("YOUTH");
        assertThat(created.path("nationality").asText()).isEqualTo("Tunisian");
        String id = created.path("id").asText();

        athlete.put("firstName", "Samir");
        mockMvc.perform(as(MANAGER_EST, jsonBody(put("/athletes/" + id), athlete))).andExpect(status().isOk());
        mockMvc.perform(as(MANAGER_CSS, jsonBody(put("/athletes/" + id), athlete))).andExpect(status().isForbidden());

        athlete.put("clubId", CSS_CLUB);
        mockMvc.perform(as(MANAGER_EST, jsonBody(put("/athletes/" + id), athlete))).andExpect(status().isForbidden());
        mockMvc.perform(as(STAFF, jsonBody(put("/athletes/" + id), athlete)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clubId").value(CSS_CLUB));

        mockMvc.perform(as(MANAGER_CSS, delete("/athletes/" + id))).andExpect(status().isForbidden());
        mockMvc.perform(as(STAFF, delete("/athletes/" + id))).andExpect(status().isOk());

        // Athlete portal: own profile
        mockMvc.perform(as(ATHLETE, get("/athletes/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(FERJANI));
    }

    @Test
    @DisplayName("competition events CRUD with unique codes")
    void competitionEvents() throws Exception {
        String comp = createCompetition("DRAFT", null, null);
        JsonNode competition = data(mockMvc.perform(get("/competitions/" + comp)).andReturn());
        assertThat(competition.path("venueCountry").asText()).isEqualTo("TN");
        assertThat(competition.path("level").asText()).isEqualTo("NATIONAL");

        Map<String, Object> body = new HashMap<>();
        body.put("name", "200m Women");
        body.put("code", "F-200");
        body.put("discipline", "200m");
        body.put("genderCategory", "FEMALE");
        body.put("ageCategory", "SENIOR");
        body.put("scoringUnit", "seconds");
        String eventId = data(mockMvc.perform(as(STAFF, jsonBody(post("/competitions/" + comp + "/events"), body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.lowerIsBetter").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn()).path("id").asText();

        mockMvc.perform(as(STAFF, jsonBody(post("/competitions/" + comp + "/events"), body)))
                .andExpect(status().isConflict());
        mockMvc.perform(as(MANAGER_EST, jsonBody(post("/competitions/" + comp + "/events"), body)))
                .andExpect(status().isForbidden());

        body.put("name", "200m Women Senior");
        body.put("lowerIsBetter", true);
        mockMvc.perform(as(STAFF, jsonBody(put("/competitions/" + comp + "/events/" + eventId), body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("200m Women Senior"));

        mockMvc.perform(get("/competitions/" + comp + "/events/" + eventId))
                .andExpect(jsonPath("$.data.ageCategory").value("SENIOR"));
        mockMvc.perform(get("/competitions/" + comp)).andExpect(jsonPath("$.data.eventCount").value(1));

        // Event id under the wrong competition → 404
        String other = createCompetition("DRAFT", null, null);
        mockMvc.perform(get("/competitions/" + other + "/events/" + eventId)).andExpect(status().isNotFound());

        mockMvc.perform(as(STAFF, delete("/competitions/" + comp + "/events/" + eventId))).andExpect(status().isOk());
        mockMvc.perform(as(ADMIN, delete("/competitions/" + comp))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("competition status changes stamp publication and cancellation dates")
    void competitionLifecycle() throws Exception {
        String comp = createCompetition("DRAFT", null, null);
        JsonNode c = data(mockMvc.perform(get("/competitions/" + comp)).andReturn());
        assertThat(c.has("publishedAt")).isFalse();

        Map<String, Object> body = new HashMap<>();
        body.put("name", c.path("name").asText());
        body.put("sport", "Athletics");
        body.put("startDate", c.path("startDate").asText());
        body.put("endDate", c.path("endDate").asText());
        body.put("status", "PUBLISHED");
        mockMvc.perform(as(STAFF, jsonBody(put("/competitions/" + comp), body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publishedAt").exists());

        body.put("endDate", "2000-01-01");
        mockMvc.perform(as(STAFF, jsonBody(put("/competitions/" + comp), body))).andExpect(status().isBadRequest());
    }
}
