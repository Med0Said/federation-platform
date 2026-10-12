package com.federation.registrations;

import com.fasterxml.jackson.databind.JsonNode;
import com.federation.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Registrations — capacity, waiting list and permissions")
class RegistrationWorkflowIntegrationTest extends ApiTestSupport {

    private MvcResult register(String who, String competitionId, String eventId, String athleteId) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("eventId", eventId);
        if (athleteId != null) body.put("athleteId", athleteId);
        body.put("seedValue", 10.85);
        return mockMvc.perform(as(who, jsonBody(post("/competitions/" + competitionId + "/registrations"), body)))
                .andReturn();
    }

    private JsonNode setStatus(String who, String registrationId, String status, int expectedHttp) throws Exception {
        MvcResult res = mockMvc.perform(as(who, jsonBody(patch("/registrations/" + registrationId + "/status"),
                Map.of("status", status, "reason", "test")))).andReturn();
        assertThat(res.getResponse().getStatus()).as(res.getResponse().getContentAsString()).isEqualTo(expectedHttp);
        return data(res);
    }

    @Test
    @DisplayName("athlete self-registers; full event waitlists; cancellation promotes the head of the queue")
    void capacityAndWaitlist() throws Exception {
        String comp  = createCompetition("REGISTRATION_OPEN", null, null);
        String event = createEvent(comp, "MALE", 2, true);
        String a2 = createAthlete(EST_CLUB, "MALE");
        String a3 = createAthlete(EST_CLUB, "MALE");
        String a4 = createAthlete(CSS_CLUB, "MALE");

        // 1. Athlete registers themself — athleteId is inferred from the account
        MvcResult self = register(ATHLETE, comp, event, null);
        assertThat(self.getResponse().getStatus()).as(self.getResponse().getContentAsString()).isEqualTo(201);
        JsonNode r1 = data(self);
        assertThat(r1.path("athleteId").asText()).isEqualTo(FERJANI);
        assertThat(r1.path("status").asText()).isEqualTo("PENDING");
        assertThat(r1.path("registrationNumber").asText()).matches("REG-\\d{4}-\\d{5}");
        assertThat(r1.path("feeAmount").decimalValue()).isEqualByComparingTo("25");
        assertThat(r1.path("clubId").asText()).isEqualTo(EST_CLUB);

        // 2. Second slot taken by the club manager registering their athlete
        JsonNode r2 = data(register(MANAGER_EST, comp, event, a2));
        assertThat(r2.path("status").asText()).isEqualTo("PENDING");

        // 3./4. Event is full → waiting list positions 1 and 2
        MvcResult third = register(STAFF, comp, event, a3);
        assertThat(third.getResponse().getStatus()).isEqualTo(201);
        assertThat(body(third).path("message").asText()).contains("waiting list");
        JsonNode r3 = data(third);
        assertThat(r3.path("status").asText()).isEqualTo("WAITLISTED");
        assertThat(r3.path("waitlistPosition").asInt()).isEqualTo(1);
        JsonNode r4 = data(register(STAFF, comp, event, a4));
        assertThat(r4.path("waitlistPosition").asInt()).isEqualTo(2);

        // Event list shows the counters
        JsonNode events = data(mockMvc.perform(get("/competitions/" + comp + "/events")).andReturn());
        assertThat(events.get(0).path("registrationCount").asInt()).isEqualTo(2);
        assertThat(events.get(0).path("waitlistCount").asInt()).isEqualTo(2);

        // 5. Waitlisted entry cannot jump into a full event
        setStatus(STAFF, r3.path("id").asText(), "CONFIRMED", 422);

        // 6. The athlete withdraws → head of the queue is promoted, queue renumbered
        JsonNode cancelled = setStatus(ATHLETE, r1.path("id").asText(), "CANCELLED", 200);
        assertThat(cancelled.path("status").asText()).isEqualTo("CANCELLED");
        assertThat(cancelled.path("cancelledAt").isMissingNode()).isFalse();

        JsonNode promoted = data(mockMvc.perform(as(STAFF, get("/registrations/" + r3.path("id").asText()))).andReturn());
        assertThat(promoted.path("status").asText()).isEqualTo("PENDING");
        assertThat(promoted.has("waitlistPosition")).isFalse();
        JsonNode last = data(mockMvc.perform(as(STAFF, get("/registrations/" + r4.path("id").asText()))).andReturn());
        assertThat(last.path("waitlistPosition").asInt()).isEqualTo(1);

        // 7. Cancelled is terminal
        setStatus(STAFF, r1.path("id").asText(), "PENDING", 422);
    }

    @Test
    @DisplayName("duplicates, eligibility, club quota and registration window are enforced")
    void businessRules() throws Exception {
        String comp  = createCompetition("REGISTRATION_OPEN", null, 2);
        String men   = createEvent(comp, "MALE", null, true);
        String women = createEvent(comp, "FEMALE", null, true);

        assertThat(register(ATHLETE, comp, men, null).getResponse().getStatus()).isEqualTo(201);
        // Same athlete, same event → 409
        assertThat(register(ATHLETE, comp, men, null).getResponse().getStatus()).isEqualTo(409);
        // Male athlete in a women's event → 422
        assertThat(register(ATHLETE, comp, women, null).getResponse().getStatus()).isEqualTo(422);

        // Club quota (maxPerClub = 2): second EST entry ok, third rejected
        assertThat(register(STAFF, comp, men, createAthlete(EST_CLUB, "MALE")).getResponse().getStatus()).isEqualTo(201);
        MvcResult overQuota = register(STAFF, comp, men, createAthlete(EST_CLUB, "MALE"));
        assertThat(overQuota.getResponse().getStatus()).isEqualTo(422);
        assertThat(body(overQuota).path("message").asText()).contains("quota");

        // Event from another competition → 400
        String other = createCompetition("REGISTRATION_OPEN", null, null);
        String otherEvent = createEvent(other, null, null, true);
        assertThat(register(STAFF, comp, otherEvent, createAthlete(CSS_CLUB, "MALE")).getResponse().getStatus())
                .isEqualTo(400);

        // Registration closed for athletes, still possible for staff (late entries)
        String closed = createCompetition("REGISTRATION_CLOSED", null, null);
        String closedEvent = createEvent(closed, null, null, true);
        assertThat(register(ATHLETE, closed, closedEvent, null).getResponse().getStatus()).isEqualTo(422);
        assertThat(register(STAFF, closed, closedEvent, FERJANI).getResponse().getStatus()).isEqualTo(201);

        // Draft competition: nobody can register
        String draft = createCompetition("DRAFT", null, null);
        String draftEvent = createEvent(draft, null, null, true);
        assertThat(register(STAFF, draft, draftEvent, FERJANI).getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    @DisplayName("ownership: managers only handle their club, athletes only themselves, public cannot register")
    void permissions() throws Exception {
        String comp  = createCompetition("REGISTRATION_OPEN", null, null);
        String event = createEvent(comp, null, null, true);
        String cssAthlete = createAthlete(CSS_CLUB, "MALE");

        assertThat(register(MANAGER_EST, comp, event, cssAthlete).getResponse().getStatus()).isEqualTo(403);
        assertThat(register(ATHLETE, comp, event, cssAthlete).getResponse().getStatus()).isEqualTo(403);

        JsonNode reg = data(register(MANAGER_CSS, comp, event, cssAthlete));
        String regId = reg.path("id").asText();

        // Other club's manager and unrelated athlete cannot read it
        mockMvc.perform(as(MANAGER_EST, get("/registrations/" + regId))).andExpect(status().isForbidden());
        mockMvc.perform(as(ATHLETE, get("/registrations/" + regId))).andExpect(status().isForbidden());
        mockMvc.perform(as(MANAGER_CSS, get("/registrations/" + regId))).andExpect(status().isOk());

        // Only staff can confirm
        setStatus(MANAGER_CSS, regId, "CONFIRMED", 403);
        setStatus(STAFF, regId, "CONFIRMED", 200);

        // Competition listing: staff see all, managers must scope to their club
        mockMvc.perform(as(STAFF, get("/competitions/" + comp + "/registrations")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(as(MANAGER_EST, get("/competitions/" + comp + "/registrations"))).andExpect(status().isForbidden());
        mockMvc.perform(as(MANAGER_CSS, get("/competitions/" + comp + "/registrations?clubId=" + CSS_CLUB)))
                .andExpect(status().isOk());
        mockMvc.perform(as(ATHLETE, get("/competitions/" + comp + "/registrations"))).andExpect(status().isForbidden());

        // A freshly registered public account cannot register at all
        MvcResult signup = mockMvc.perform(jsonBody(post("/auth/register"), Map.of(
                "email", "fan" + System.nanoTime() + "@test.local", "username", "fan" + System.nanoTime(),
                "password", "Secret@123", "firstName", "Fan", "lastName", "User"))).andReturn();
        String publicToken = data(signup).path("accessToken").asText();
        mockMvc.perform(withToken(publicToken, jsonBody(post("/competitions/" + comp + "/registrations"),
                Map.of("eventId", event, "athleteId", FERJANI)))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("athlete portal lists own registrations; staff assign bibs and record payment")
    void portalAndAdministration() throws Exception {
        String comp  = createCompetition("REGISTRATION_OPEN", null, null);
        String event = createEvent(comp, null, null, true);
        String regId = data(register(ATHLETE, comp, event, null)).path("id").asText();
        String other = data(register(STAFF, comp, event, createAthlete(EST_CLUB, "MALE"))).path("id").asText();

        JsonNode mine = data(mockMvc.perform(as(ATHLETE, get("/registrations/me?size=100"))).andReturn()).path("content");
        assertThat(mine.findValuesAsText("id")).contains(regId).doesNotContain(other);
        mockMvc.perform(as(ATHLETE, get("/athletes/" + FERJANI + "/registrations"))).andExpect(status().isOk());

        setStatus(STAFF, regId, "CONFIRMED", 200);
        setStatus(STAFF, other, "CONFIRMED", 200);
        mockMvc.perform(as(STAFF, post("/competitions/" + comp + "/registrations/bibs")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assigned").value(2));
        // Idempotent: nothing left to assign
        mockMvc.perform(as(STAFF, post("/competitions/" + comp + "/registrations/bibs")))
                .andExpect(jsonPath("$.data.assigned").value(0));

        mockMvc.perform(as(STAFF, jsonBody(patch("/registrations/" + regId), Map.of("feePaid", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feePaid").value(true))
                .andExpect(jsonPath("$.data.bibNumber").value(1));
        mockMvc.perform(as(ATHLETE, jsonBody(patch("/registrations/" + regId), Map.of("feePaid", true))))
                .andExpect(status().isForbidden());

        // Disqualification requires a reason
        mockMvc.perform(as(STAFF, jsonBody(patch("/registrations/" + other + "/status"),
                Map.of("status", "DISQUALIFIED")))).andExpect(status().isBadRequest());

        // Competitions report confirmed registrations; competitions with entries cannot be deleted
        mockMvc.perform(get("/competitions/" + comp)).andExpect(jsonPath("$.data.confirmedRegistrations").value(2));
        mockMvc.perform(as(ADMIN, delete("/competitions/" + comp))).andExpect(status().isUnprocessableEntity());
        mockMvc.perform(as(ADMIN, delete("/competitions/" + comp + "/events/" + event)))
                .andExpect(status().isUnprocessableEntity());
    }
}
