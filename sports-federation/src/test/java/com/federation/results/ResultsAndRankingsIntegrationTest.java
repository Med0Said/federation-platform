package com.federation.results;

import com.fasterxml.jackson.databind.JsonNode;
import com.federation.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Results — automatic ranking recalculation")
class ResultsAndRankingsIntegrationTest extends ApiTestSupport {

    private JsonNode record(String comp, String event, String athlete, String value, String status) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("competitionId", comp);
        body.put("eventId", event);
        body.put("athleteId", athlete);
        if (value != null) body.put("performanceValue", new BigDecimal(value));
        body.put("performanceText", value != null ? value : status);
        body.put("status", status);
        MvcResult res = mockMvc.perform(as(STAFF, jsonBody(post("/results"), body))).andReturn();
        assertThat(res.getResponse().getStatus()).as(res.getResponse().getContentAsString()).isEqualTo(201);
        return data(res);
    }

    private List<String> leaderboard(String event) throws Exception {
        JsonNode rows = data(mockMvc.perform(get("/results/event/" + event + "/rankings")).andReturn());
        return rows.findValuesAsText("athleteId");
    }

    @Test
    @DisplayName("time events rank ascending, medals go to the top three, DNS is not ranked")
    void timeEvent() throws Exception {
        String comp  = createCompetition("ONGOING", null, null);
        String event = createEvent(comp, null, null, true);
        String a = createAthlete(EST_CLUB, "MALE");
        String b = createAthlete(CSS_CLUB, "MALE");
        String c = createAthlete(EST_CLUB, "MALE");
        String d = createAthlete(CSS_CLUB, "MALE");
        String e = createAthlete(CSS_CLUB, "MALE");

        record(comp, event, a, "10.42", "OFFICIAL");
        record(comp, event, b, "10.05", "OFFICIAL");
        JsonNode slow = record(comp, event, c, "10.98", "UNOFFICIAL");
        record(comp, event, d, "10.61", "OFFICIAL");
        record(comp, event, e, null, "DNS");

        assertThat(leaderboard(event)).containsExactly(b, a, d, c);

        JsonNode board = data(mockMvc.perform(get("/results/event/" + event + "/rankings")).andReturn());
        assertThat(board.get(0).path("medal").asText()).isEqualTo("GOLD");
        assertThat(board.get(1).path("medal").asText()).isEqualTo("SILVER");
        assertThat(board.get(2).path("medal").asText()).isEqualTo("BRONZE");
        assertThat(board.get(3).has("medal")).isFalse();
        assertThat(board.get(0).path("official").asBoolean()).isTrue();
        assertThat(board.get(3).path("official").asBoolean()).isFalse();
        assertThat(board.get(0).path("clubId").asText()).isEqualTo(CSS_CLUB);

        // Event results: ranked first, DNS last; results carry rank and medal
        JsonNode results = data(mockMvc.perform(get("/results/event/" + event)).andReturn());
        assertThat(results).hasSize(5);
        assertThat(results.get(0).path("rankPosition").asInt()).isEqualTo(1);
        assertThat(results.get(4).path("status").asText()).isEqualTo("DNS");
        assertThat(results.get(4).has("rankPosition")).isFalse();

        // Updating the slowest athlete to a winning time reshuffles the podium
        Map<String, Object> update = new HashMap<>();
        update.put("competitionId", comp);
        update.put("eventId", event);
        update.put("athleteId", c);
        update.put("performanceValue", new BigDecimal("9.99"));
        update.put("status", "OFFICIAL");
        mockMvc.perform(as(STAFF, jsonBody(put("/results/" + slow.path("id").asText()), update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rankPosition").value(1))
                .andExpect(jsonPath("$.data.medal").value("GOLD"));
        assertThat(leaderboard(event)).containsExactly(c, b, a, d);

        // Deleting the winner promotes everyone
        mockMvc.perform(as(STAFF, delete("/results/" + slow.path("id").asText()))).andExpect(status().isOk());
        assertThat(leaderboard(event)).containsExactly(b, a, d);
    }

    @Test
    @DisplayName("distance events rank descending")
    void distanceEvent() throws Exception {
        String comp  = createCompetition("ONGOING", null, null);
        String event = createEvent(comp, "FEMALE", null, false);
        String a = createAthlete(EST_CLUB, "FEMALE");
        String b = createAthlete(CSS_CLUB, "FEMALE");
        record(comp, event, a, "6.12", "OFFICIAL");
        record(comp, event, b, "6.48", "OFFICIAL");
        assertThat(leaderboard(event)).containsExactly(b, a);
    }

    @Test
    @DisplayName("official results record their verifier; duplicates and mismatched events are rejected")
    void validation() throws Exception {
        String comp  = createCompetition("ONGOING", null, null);
        String event = createEvent(comp, null, null, true);
        String a = createAthlete(EST_CLUB, "MALE");

        JsonNode official = record(comp, event, a, "11.00", "OFFICIAL");
        JsonNode stored = data(mockMvc.perform(get("/results/" + official.path("id").asText())).andReturn());
        assertThat(stored.path("performanceUnit").asText()).isEqualTo("seconds");

        // Same athlete, event and round → 409
        mockMvc.perform(as(STAFF, jsonBody(post("/results"), Map.of(
                "competitionId", comp, "eventId", event, "athleteId", a, "performanceValue", 10.9))))
                .andExpect(status().isConflict());

        // Event of another competition → 400
        String other = createCompetition("ONGOING", null, null);
        mockMvc.perform(as(STAFF, jsonBody(post("/results"), Map.of(
                "competitionId", other, "eventId", event, "athleteId", createAthlete(EST_CLUB, "MALE")))))
                .andExpect(status().isBadRequest());

        // Club managers and athletes cannot record results
        mockMvc.perform(as(MANAGER_EST, jsonBody(post("/results"), Map.of(
                "competitionId", comp, "eventId", event, "athleteId", createAthlete(EST_CLUB, "MALE")))))
                .andExpect(status().isForbidden());

        // Manual recalculation endpoint is staff-only
        mockMvc.perform(as(STAFF, post("/results/event/" + event + "/rankings/recalculate")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].rankPosition").value(1));
        mockMvc.perform(as(ATHLETE, post("/results/event/" + event + "/rankings/recalculate")))
                .andExpect(status().isForbidden());

        // Results filter by athlete
        JsonNode filtered = data(mockMvc.perform(get("/results?athleteId=" + a)).andReturn()).path("content");
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).path("rankPosition").asInt()).isEqualTo(1);
    }
}
