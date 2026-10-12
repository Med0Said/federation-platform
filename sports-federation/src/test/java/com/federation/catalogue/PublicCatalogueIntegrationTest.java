package com.federation.catalogue;

import com.fasterxml.jackson.databind.JsonNode;
import com.federation.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Public (anonymous) read access to the catalogues, filtering against real
 * PostgreSQL enums, privacy masking and error mapping.
 *
 * Regression: before the Specification rewrite, /competitions, /results and /news
 * returned 500 on PostgreSQL ("could not determine data type of parameter",
 * "function lower(bytea) does not exist", "news_status = character varying").
 */
@DisplayName("Public catalogue — anonymous reads")
class PublicCatalogueIntegrationTest extends ApiTestSupport {

    @Test
    @DisplayName("every list endpoint answers 200 anonymously, with and without filters")
    void listEndpointsArePublic() throws Exception {
        for (String url : new String[] {
                "/clubs", "/clubs?search=sfax&status=ACTIVE&sort=name,desc",
                "/athletes", "/athletes?gender=FEMALE&status=ACTIVE&category=SENIOR&sort=lastName,desc",
                "/competitions", "/competitions?status=COMPLETED", "/competitions?search=open&from=2020-01-01&to=2030-12-31",
                "/results", "/results?status=OFFICIAL&competitionId=d1000000-0000-0000-0000-000000000001",
                "/news", "/news?category=ANNOUNCEMENT&search=a", "/news/tags",
                "/competitions/d1000000-0000-0000-0000-000000000001/events",
                "/results/event/e1000000-0000-0000-0000-000000000001",
                "/results/event/e1000000-0000-0000-0000-000000000001/rankings"}) {
            mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Test
    @DisplayName("filters actually filter")
    void filtersApply() throws Exception {
        JsonNode clubs = data(mockMvc.perform(get("/clubs?search=sfax")).andReturn()).path("content");
        assertThat(clubs).hasSize(1);
        assertThat(clubs.get(0).path("city").asText()).isEqualTo("Sfax");

        JsonNode women = data(mockMvc.perform(get("/athletes?gender=FEMALE&size=100")).andReturn()).path("content");
        assertThat(women).isNotEmpty();
        women.forEach(a -> assertThat(a.path("gender").asText()).isEqualTo("FEMALE"));

        JsonNode completed = data(mockMvc.perform(get("/competitions?status=COMPLETED")).andReturn()).path("content");
        completed.forEach(c -> assertThat(c.path("status").asText()).isEqualTo("COMPLETED"));
    }

    @Test
    @DisplayName("club list reports the real number of active athletes")
    void clubAthleteCount() throws Exception {
        JsonNode club = data(mockMvc.perform(get("/clubs/" + EST_CLUB)).andReturn());
        assertThat(club.path("activeAthletes").asLong()).isPositive();
    }

    @Test
    @DisplayName("athlete contact and medical data are hidden from the public but visible to staff")
    void athletePrivacy() throws Exception {
        String id = createAthlete(EST_CLUB, "MALE");

        JsonNode anonymous = data(mockMvc.perform(get("/athletes/" + id)).andReturn());
        assertThat(anonymous.has("email")).isFalse();
        assertThat(anonymous.has("phone")).isFalse();
        assertThat(anonymous.has("medicalNotes")).isFalse();
        assertThat(anonymous.path("lastName").asText()).startsWith("Athlete");

        JsonNode staff = data(mockMvc.perform(as(STAFF, get("/athletes/" + id))).andReturn());
        assertThat(staff.path("email").asText()).isEqualTo("private@example.com");
        assertThat(staff.path("medicalNotes").asText()).isEqualTo("confidential");

        // The manager of the athlete's club sees it too, another club's manager does not
        assertThat(data(mockMvc.perform(as(MANAGER_EST, get("/athletes/" + id))).andReturn()).has("email")).isTrue();
        assertThat(data(mockMvc.perform(as(MANAGER_CSS, get("/athletes/" + id))).andReturn()).has("email")).isFalse();
    }

    @Test
    @DisplayName("draft news is invisible to the public and listed for editors")
    void draftNewsHidden() throws Exception {
        MvcResult created = mockMvc.perform(as(STAFF, jsonBody(post("/news"), Map.of(
                "title", unique("Draft article"),
                "content", "Not yet public",
                "status", "DRAFT")))).andExpect(status().isCreated()).andReturn();
        String id = data(created).path("id").asText();
        String slug = data(created).path("slug").asText();

        mockMvc.perform(get("/news/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(get("/news/slug/" + slug)).andExpect(status().isNotFound());
        JsonNode publicList = data(mockMvc.perform(get("/news?status=DRAFT&size=100")).andReturn()).path("content");
        publicList.forEach(n -> assertThat(n.path("status").asText()).isEqualTo("PUBLISHED"));

        mockMvc.perform(as(STAFF, get("/news/" + id))).andExpect(status().isOk());
        JsonNode staffList = data(mockMvc.perform(as(STAFF, get("/news?status=DRAFT&size=100"))).andReturn()).path("content");
        assertThat(staffList.findValuesAsText("id")).contains(id);

        // Publishing makes it public and the slug endpoint counts views
        mockMvc.perform(as(STAFF, org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/news/" + id + "/publish"))).andExpect(status().isOk());
        mockMvc.perform(get("/news/slug/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(1));
    }

    @Test
    @DisplayName("bad input maps to 4xx instead of 500")
    void errorMapping() throws Exception {
        mockMvc.perform(get("/clubs?sort=doesNotExist,asc")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/clubs?sort=name,sideways&size=-5")).andExpect(status().isOk());
        mockMvc.perform(get("/athletes/not-a-uuid")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/athletes/00000000-0000-0000-0000-000000000000")).andExpect(status().isNotFound());
        mockMvc.perform(get("/no/such/endpoint")).andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("registrations, users and dashboard are never public")
    void privateAreas() throws Exception {
        mockMvc.perform(get("/competitions/d1000000-0000-0000-0000-000000000001/registrations"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/athletes/" + FERJANI + "/registrations")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/registrations/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/dashboard/stats")).andExpect(status().isUnauthorized());
    }
}
