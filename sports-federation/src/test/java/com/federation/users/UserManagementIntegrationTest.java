package com.federation.users;

import com.fasterxml.jackson.databind.JsonNode;
import com.federation.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Users — administration, profile and dashboard")
class UserManagementIntegrationTest extends ApiTestSupport {

    private JsonNode createUser(String role) throws Exception {
        String suffix = Long.toString(System.nanoTime());
        MvcResult res = mockMvc.perform(as(ADMIN, jsonBody(post("/users"), Map.of(
                "email", "user" + suffix + "@test.local",
                "username", "user" + suffix,
                "password", "Initial@123",
                "firstName", "Created",
                "lastName", "ByAdmin",
                "role", role)))).andExpect(status().isCreated()).andReturn();
        return data(res);
    }

    @Test
    @DisplayName("admin lists, filters, creates and edits users; others get 403")
    void adminCrud() throws Exception {
        mockMvc.perform(as(ADMIN, get("/users?role=ROLE_CLUB_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3));
        mockMvc.perform(as(ADMIN, get("/users?search=ferjani")))
                .andExpect(jsonPath("$.data.content[0].athleteId").value(FERJANI));

        JsonNode user = createUser("ROLE_CLUB_MANAGER");
        assertThat(user.path("status").asText()).isEqualTo("ACTIVE");
        assertThat(user.has("password")).isFalse();
        String id = user.path("id").asText();

        mockMvc.perform(as(ADMIN, jsonBody(put("/users/" + id), Map.of("phone", "+216 11 222 333"))))
                .andExpect(jsonPath("$.data.phone").value("+216 11 222 333"));

        mockMvc.perform(as(STAFF, get("/users"))).andExpect(status().isForbidden());
        mockMvc.perform(as(MANAGER_EST, get("/users/" + id))).andExpect(status().isForbidden());

        // Duplicate email → 409; weak password → 400
        mockMvc.perform(as(ADMIN, jsonBody(post("/users"), Map.of(
                "email", user.path("email").asText(), "username", "another" + System.nanoTime(),
                "password", "Initial@123", "firstName", "A", "lastName", "B", "role", "ROLE_PUBLIC"))))
                .andExpect(status().isConflict());
        mockMvc.perform(as(ADMIN, jsonBody(post("/users"), Map.of(
                "email", "weak" + System.nanoTime() + "@test.local", "username", "weak" + System.nanoTime(),
                "password", "weak", "firstName", "A", "lastName", "B", "role", "ROLE_PUBLIC"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("role change, suspension and password reset revoke sessions")
    void roleStatusAndReset() throws Exception {
        JsonNode user = createUser("ROLE_PUBLIC");
        String id = user.path("id").asText();
        String email = user.path("email").asText();

        MvcResult login = mockMvc.perform(jsonBody(post("/auth/login"),
                Map.of("usernameOrEmail", email, "password", "Initial@123"))).andReturn();
        String refresh = data(login).path("refreshToken").asText();

        mockMvc.perform(as(ADMIN, jsonBody(patch("/users/" + id + "/role"), Map.of("role", "ROLE_ATHLETE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ROLE_ATHLETE"));
        // Old refresh token no longer works after the role change
        mockMvc.perform(jsonBody(post("/auth/refresh"), Map.of("refreshToken", refresh)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(as(ADMIN, jsonBody(patch("/users/" + id + "/status"), Map.of("status", "SUSPENDED"))))
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
        mockMvc.perform(jsonBody(post("/auth/login"), Map.of("usernameOrEmail", email, "password", "Initial@123")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(as(ADMIN, jsonBody(patch("/users/" + id + "/status"), Map.of("status", "ACTIVE"))))
                .andExpect(status().isOk());

        String temporary = data(mockMvc.perform(as(ADMIN, post("/users/" + id + "/reset-password")))
                .andExpect(status().isOk()).andReturn()).path("temporaryPassword").asText();
        assertThat(temporary).startsWith("Tmp@").hasSizeGreaterThanOrEqualTo(12);
        assertThat(login(email, temporary)).isNotBlank();

        mockMvc.perform(as(ADMIN, delete("/users/" + id))).andExpect(status().isOk());
        mockMvc.perform(as(ADMIN, get("/users/" + id))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("an admin cannot lock themselves out")
    void selfProtection() throws Exception {
        String adminId = data(mockMvc.perform(as(ADMIN, get("/auth/me"))).andReturn()).path("id").asText();
        mockMvc.perform(as(ADMIN, jsonBody(patch("/users/" + adminId + "/role"), Map.of("role", "ROLE_PUBLIC"))))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(as(ADMIN, jsonBody(patch("/users/" + adminId + "/status"), Map.of("status", "SUSPENDED"))))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(as(ADMIN, delete("/users/" + adminId))).andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("users update their own profile via PATCH /auth/me")
    void ownProfile() throws Exception {
        JsonNode user = createUser("ROLE_PUBLIC");
        String token = login(user.path("email").asText(), "Initial@123");
        mockMvc.perform(withToken(token, jsonBody(patch("/auth/me"),
                        Map.of("firstName", "Renamed", "phone", "+216 99 888 777"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Renamed"))
                .andExpect(jsonPath("$.data.lastName").value("ByAdmin"))
                .andExpect(jsonPath("$.data.fullName").value("Renamed ByAdmin"));
        // Role cannot be escalated through the profile endpoint
        mockMvc.perform(withToken(token, jsonBody(patch("/auth/me"), Map.of("role", "ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ROLE_PUBLIC"));
    }

    @Test
    @DisplayName("dashboard statistics for back-office roles")
    void dashboard() throws Exception {
        mockMvc.perform(as(STAFF, get("/dashboard/stats")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAthletes").isNumber())
                .andExpect(jsonPath("$.data.activeClubs").isNumber())
                .andExpect(jsonPath("$.data.publishedNews").isNumber());
        mockMvc.perform(as(ATHLETE, get("/dashboard/stats"))).andExpect(status().isForbidden());
    }
}
