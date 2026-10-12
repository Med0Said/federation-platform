package com.federation.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.federation.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Helpers for API integration tests: logs in as the seeded accounts
 * (V2 / V12 migrations), caches their access tokens and creates fixtures
 * through the public API so every test exercises the real stack.
 *
 * Tests share one database, so fixtures always use unique names.
 */
public abstract class ApiTestSupport extends BaseIntegrationTest {

    public static final String ADMIN        = "admin@federation.local";
    public static final String STAFF        = "staff@federation.local";
    /** Manages club EST ({@link #EST_CLUB}). */
    public static final String MANAGER_EST  = "manager.esperance@federation.local";
    /** Manages club CSS ({@link #CSS_CLUB}). */
    public static final String MANAGER_CSS  = "manager.sfax@federation.local";
    /** Linked to athlete {@link #FERJANI} (male, senior, club EST). */
    public static final String ATHLETE      = "athlete.ferjani@federation.local";
    public static final String PASSWORD     = "Test@1234";

    public static final String EST_CLUB = "b1000000-0000-0000-0000-000000000001";
    public static final String CSS_CLUB = "b1000000-0000-0000-0000-000000000002";
    public static final String FERJANI  = "c1000000-0000-0000-0000-000000000001";

    private static final Map<String, String> TOKENS = new ConcurrentHashMap<>();

    @Autowired protected MockMvc      mockMvc;
    @Autowired protected ObjectMapper objectMapper;

    // ── Auth ──────────────────────────────────────────────────────────────

    protected String token(String email) throws Exception {
        String cached = TOKENS.get(email);
        if (cached != null) return cached;
        String t = login(email, ADMIN.equals(email) ? "Admin@1234" : PASSWORD);
        TOKENS.put(email, t);
        return t;
    }

    protected String login(String email, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("usernameOrEmail", email, "password", password))))
                .andReturn();
        if (res.getResponse().getStatus() != 200) {
            throw new IllegalStateException("Login failed for " + email + ": "
                    + res.getResponse().getContentAsString());
        }
        return data(res).path("accessToken").asText();
    }

    protected MockHttpServletRequestBuilder as(String email, MockHttpServletRequestBuilder req) throws Exception {
        return withToken(token(email), req);
    }

    protected MockHttpServletRequestBuilder withToken(String token, MockHttpServletRequestBuilder req) {
        return req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    protected MockHttpServletRequestBuilder jsonBody(MockHttpServletRequestBuilder req, Object payload) throws Exception {
        return req.contentType(MediaType.APPLICATION_JSON).content(json(payload));
    }

    // ── JSON ──────────────────────────────────────────────────────────────

    protected String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    protected JsonNode body(MvcResult res) throws Exception {
        return objectMapper.readTree(res.getResponse().getContentAsString());
    }

    protected JsonNode data(MvcResult res) throws Exception {
        return body(res).path("data");
    }

    protected static String unique(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    // ── Fixtures ──────────────────────────────────────────────────────────

    protected String createCompetition(String status, Integer maxParticipants, Integer maxPerClub) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", unique("Test Meeting"));
        body.put("sport", "Athletics");
        body.put("startDate", LocalDate.now().plusMonths(2).toString());
        body.put("endDate", LocalDate.now().plusMonths(2).plusDays(1).toString());
        body.put("registrationDeadline", LocalDate.now().plusMonths(1).toString());
        body.put("status", status);
        body.put("entryFee", 25);
        if (maxParticipants != null) body.put("maxParticipants", maxParticipants);
        if (maxPerClub != null) body.put("maxPerClub", maxPerClub);
        MvcResult res = mockMvc.perform(as(ADMIN, jsonBody(post("/competitions"), body))).andReturn();
        assertCreated(res);
        return data(res).path("id").asText();
    }

    protected String createEvent(String competitionId, String gender, Integer maxParticipants,
                                 boolean lowerIsBetter) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", unique("Event"));
        body.put("discipline", lowerIsBetter ? "100m Sprint" : "Long Jump");
        body.put("scoringUnit", lowerIsBetter ? "seconds" : "meters");
        body.put("lowerIsBetter", lowerIsBetter);
        body.put("status", "REGISTRATION_OPEN");
        if (gender != null) body.put("genderCategory", gender);
        if (maxParticipants != null) body.put("maxParticipants", maxParticipants);
        MvcResult res = mockMvc.perform(as(ADMIN,
                jsonBody(post("/competitions/" + competitionId + "/events"), body))).andReturn();
        assertCreated(res);
        return data(res).path("id").asText();
    }

    protected String createAthlete(String clubId, String gender) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("licenseNumber", "TST-" + UUID.randomUUID().toString().substring(0, 12));
        body.put("firstName", "Test");
        body.put("lastName", unique("Athlete"));
        body.put("dateOfBirth", "1999-05-05");
        body.put("gender", gender);
        body.put("email", "private@example.com");
        body.put("phone", "+216 00 000 000");
        body.put("medicalNotes", "confidential");
        if (clubId != null) body.put("clubId", clubId);
        MvcResult res = mockMvc.perform(as(ADMIN, jsonBody(post("/athletes"), body))).andReturn();
        assertCreated(res);
        return data(res).path("id").asText();
    }

    private void assertCreated(MvcResult res) throws Exception {
        if (res.getResponse().getStatus() != 201) {
            throw new IllegalStateException("Fixture creation failed: " + res.getResponse().getStatus()
                    + " " + res.getResponse().getContentAsString());
        }
    }
}
