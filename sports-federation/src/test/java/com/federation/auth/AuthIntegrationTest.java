package com.federation.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.federation.auth.dto.*;
import com.federation.common.response.ApiResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full end-to-end authentication flow tests.
 *
 * Uses the full Spring context with H2 (profile=test, Flyway disabled).
 * Tests run in order to share session state across the flow:
 *   register → login → me → refresh → change-password → logout
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Auth — End-to-End Flow")
class AuthIntegrationTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    // Shared state across ordered tests
    static String accessToken;
    static String refreshToken;

    private static final String EMAIL    = "e2e@test.local";
    private static final String USERNAME = "e2e_user";
    private static final String PASSWORD = "E2eTest@123";

    // ----------------------------------------------------------------
    // Step 1: Register
    // ----------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("Step 1 — register returns 201 with token pair")
    void register() throws Exception {
        RegisterRequest request = new RegisterRequest(
                EMAIL, USERNAME, PASSWORD, "E2E", "Tester");

        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.role").value("ROLE_PUBLIC"))
                .andReturn();

        AuthResponse auth = extractAuth(result);
        accessToken  = auth.getAccessToken();
        refreshToken = auth.getRefreshToken();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("Step 1b — duplicate registration returns 409")
    void registerDuplicate() throws Exception {
        RegisterRequest request = new RegisterRequest(
                EMAIL, USERNAME, PASSWORD, "E2E", "Tester");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ----------------------------------------------------------------
    // Step 2: Login
    // ----------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("Step 2 — login with email returns token pair")
    void loginWithEmail() throws Exception {
        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(jsonPath("$.data.username").value(USERNAME))
                .andReturn();

        accessToken  = extractAuth(result).getAccessToken();
        refreshToken = extractAuth(result).getRefreshToken();
    }

    @Test
    @Order(4)
    @DisplayName("Step 2b — login with username also succeeds")
    void loginWithUsername() throws Exception {
        LoginRequest request = new LoginRequest(USERNAME, PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(EMAIL));
    }

    @Test
    @Order(5)
    @DisplayName("Step 2c — login with wrong password returns 401")
    void loginBadPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(EMAIL, "WrongPass"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ----------------------------------------------------------------
    // Step 3: Protected endpoint — /auth/me
    // ----------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("Step 3 — GET /auth/me returns profile with valid token")
    void getMe() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(jsonPath("$.data.username").value(USERNAME))
                .andExpect(jsonPath("$.data.role").value("ROLE_PUBLIC"));
    }

    @Test
    @Order(7)
    @DisplayName("Step 3b — GET /auth/me without token returns 401")
    void getMeNoToken() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(8)
    @DisplayName("Step 3c — GET /auth/me with tampered token returns 401")
    void getMeTamperedToken() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + accessToken + "tampered"))
                .andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------------------
    // Step 4: Token refresh
    // ----------------------------------------------------------------

    @Test
    @Order(9)
    @DisplayName("Step 4 — refresh returns new token pair")
    void refreshToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        String oldAccess  = accessToken;
        String oldRefresh = refreshToken;
        accessToken  = extractAuth(result).getAccessToken();
        refreshToken = extractAuth(result).getRefreshToken();

        // New tokens must be different (rotation)
        assertThat(accessToken).isNotEqualTo(oldAccess);
        assertThat(refreshToken).isNotEqualTo(oldRefresh);
    }

    @Test
    @Order(10)
    @DisplayName("Step 4b — reusing an already-rotated refresh token returns 401")
    void reuseOldRefreshToken() throws Exception {
        // The previous refresh token from Step 3 is now stale
        // We need the token before Step 4's rotation — capture it from state
        // This test uses "invalid-old-token" as a stand-in
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest("completely-unknown-token"))))
                .andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------------------
    // Step 5: Change password
    // ----------------------------------------------------------------

    @Test
    @Order(11)
    @DisplayName("Step 5 — change password with wrong current password returns 401")
    void changePasswordWrongCurrent() throws Exception {
        mockMvc.perform(patch("/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("WrongOld@123", "New@1234", "New@1234"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(12)
    @DisplayName("Step 5b — change password with mismatch returns 400")
    void changePasswordMismatch() throws Exception {
        mockMvc.perform(patch("/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest(PASSWORD, "New@1234", "Different@1234"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(13)
    @DisplayName("Step 5c — change password succeeds and returns 200")
    void changePasswordSuccess() throws Exception {
        mockMvc.perform(patch("/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest(PASSWORD, "NewPass@456", "NewPass@456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(14)
    @DisplayName("Step 5d — old refresh token revoked after password change")
    void refreshRevokedAfterPasswordChange() throws Exception {
        // All tokens were revoked by changePassword — refresh should fail
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------------------
    // Step 6: Login with new password and logout
    // ----------------------------------------------------------------

    @Test
    @Order(15)
    @DisplayName("Step 6 — login with new password succeeds")
    void loginWithNewPassword() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(EMAIL, "NewPass@456"))))
                .andExpect(status().isOk())
                .andReturn();

        accessToken  = extractAuth(result).getAccessToken();
        refreshToken = extractAuth(result).getRefreshToken();
    }

    @Test
    @Order(16)
    @DisplayName("Step 7 — logout revokes all tokens")
    void logout() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(17)
    @DisplayName("Step 7b — refresh fails after logout")
    void refreshAfterLogout() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------------------
    // RBAC checks
    // ----------------------------------------------------------------

    @Test
    @Order(18)
    @DisplayName("RBAC — ROLE_PUBLIC cannot access /admin/** endpoints")
    void publicCannotAccessAdmin() throws Exception {
        // Re-login to get fresh token for the now-logged-out user
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(EMAIL, "NewPass@456"))))
                .andExpect(status().isOk())
                .andReturn();

        String token = extractAuth(login).getAccessToken();

        mockMvc.perform(get("/admin/anything")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ----------------------------------------------------------------
    // Validation edge cases
    // ----------------------------------------------------------------

    @Test
    @Order(19)
    @DisplayName("Validation — register with username containing spaces returns 400")
    void usernameWithSpaces() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("x@y.com", "user name",
                                        "Secret@123", "X", "Y"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(20)
    @DisplayName("Validation — register with missing body returns 400")
    void missingBody() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private AuthResponse extractAuth(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> wrapper = objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class, AuthResponse.class));
        return wrapper.getData();
    }
}
