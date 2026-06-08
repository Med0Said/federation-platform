package com.federation.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.federation.auth.dto.*;
import com.federation.auth.security.FederationUserDetails;
import com.federation.auth.service.AuthService;
import com.federation.common.exception.ResourceAlreadyExistsException;
import com.federation.common.exception.UnauthorizedException;
import com.federation.users.entity.User;
import com.federation.users.entity.UserRole;
import com.federation.users.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController — MockMvc")
class AuthControllerTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;
    @MockBean  AuthService   authService;

    private AuthResponse sampleAuthResponse;

    @BeforeEach
    void setUp() {
        sampleAuthResponse = AuthResponse.of(
                "access-token", "refresh-token", 3600,
                UUID.randomUUID(), "user@test.local", "testuser", UserRole.ROLE_PUBLIC);
    }

    // ================================================================
    // POST /auth/register
    // ================================================================

    @Nested
    @DisplayName("POST /auth/register")
    class RegisterEndpoint {

        @Test
        @DisplayName("201 with token pair on valid payload")
        void created() throws Exception {
            given(authService.register(any(RegisterRequest.class))).willReturn(sampleAuthResponse);

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RegisterRequest("new@test.local", "newuser",
                                            "Secret@123", "New", "User"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.role").value("ROLE_PUBLIC"));
        }

        @Test
        @DisplayName("400 when email is blank")
        void blankEmail() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RegisterRequest("", "newuser", "Secret@123", "New", "User"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 when password is too weak")
        void weakPassword() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RegisterRequest("x@y.com", "user", "weakpw", "A", "B"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 when email format is invalid")
        void invalidEmail() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RegisterRequest("not-an-email", "user",
                                            "Secret@123", "A", "B"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("409 when email already exists")
        void conflict() throws Exception {
            given(authService.register(any()))
                    .willThrow(new ResourceAlreadyExistsException("Email already exists."));

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RegisterRequest("taken@test.local", "newuser",
                                            "Secret@123", "New", "User"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ================================================================
    // POST /auth/login
    // ================================================================

    @Nested
    @DisplayName("POST /auth/login")
    class LoginEndpoint {

        @Test
        @DisplayName("200 with token pair on valid credentials")
        void ok() throws Exception {
            given(authService.login(any(LoginRequest.class))).willReturn(sampleAuthResponse);

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest("user@test.local", "Secret@123"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.expiresIn").value(3600));
        }

        @Test
        @DisplayName("401 on bad credentials")
        void unauthorized() throws Exception {
            given(authService.login(any()))
                    .willThrow(new UnauthorizedException("Invalid credentials."));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest("user@test.local", "wrong"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid credentials."));
        }

        @Test
        @DisplayName("400 when usernameOrEmail is blank")
        void blankIdentifier() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest("", "Secret@123"))))
                    .andExpect(status().isBadRequest());
        }
    }

    // ================================================================
    // POST /auth/refresh
    // ================================================================

    @Nested
    @DisplayName("POST /auth/refresh")
    class RefreshEndpoint {

        @Test
        @DisplayName("200 with new token pair")
        void ok() throws Exception {
            given(authService.refresh(any(RefreshTokenRequest.class)))
                    .willReturn(sampleAuthResponse);

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RefreshTokenRequest("old-refresh-token"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"));
        }

        @Test
        @DisplayName("401 for expired/revoked token")
        void expiredToken() throws Exception {
            given(authService.refresh(any()))
                    .willThrow(new UnauthorizedException("Refresh token expired."));

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RefreshTokenRequest("dead-token"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("400 when refresh token is blank")
        void blankToken() throws Exception {
            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RefreshTokenRequest(""))))
                    .andExpect(status().isBadRequest());
        }
    }

    // ================================================================
    // POST /auth/logout
    // ================================================================

    @Nested
    @DisplayName("POST /auth/logout")
    class LogoutEndpoint {

        @Test
        @WithMockUser(username = "user@test.local", roles = "PUBLIC")
        @DisplayName("200 when authenticated")
        void ok() throws Exception {
            // AuthService.logout needs a UUID from FederationUserDetails
            // WithMockUser injects a default UserDetails, so we just verify 200
            mockMvc.perform(post("/auth/logout"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("401 when not authenticated")
        void unauthenticated() throws Exception {
            mockMvc.perform(post("/auth/logout"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ================================================================
    // GET /auth/me
    // ================================================================

    @Nested
    @DisplayName("GET /auth/me")
    class MeEndpoint {

        @Test
        @WithMockUser(username = "user@test.local", roles = "PUBLIC")
        @DisplayName("200 when authenticated")
        void ok() throws Exception {
            UserProfileResponse profile = UserProfileResponse.builder()
                    .id(UUID.randomUUID())
                    .email("user@test.local")
                    .username("testuser")
                    .firstName("Test")
                    .lastName("User")
                    .fullName("Test User")
                    .role(UserRole.ROLE_PUBLIC)
                    .status(UserStatus.ACTIVE)
                    .build();

            given(authService.getProfile(any())).willReturn(profile);

            mockMvc.perform(get("/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("401 when not authenticated")
        void unauthenticated() throws Exception {
            mockMvc.perform(get("/auth/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ================================================================
    // PATCH /auth/change-password
    // ================================================================

    @Nested
    @DisplayName("PATCH /auth/change-password")
    class ChangePasswordEndpoint {

        @Test
        @WithMockUser(username = "user@test.local", roles = "PUBLIC")
        @DisplayName("200 on valid request")
        void ok() throws Exception {
            mockMvc.perform(patch("/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ChangePasswordRequest("Old@1234", "New@1234", "New@1234"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("401 when not authenticated")
        void unauthenticated() throws Exception {
            mockMvc.perform(patch("/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ChangePasswordRequest("a", "b", "b"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "user@test.local", roles = "PUBLIC")
        @DisplayName("400 when new password is too weak")
        void weakNewPassword() throws Exception {
            mockMvc.perform(patch("/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ChangePasswordRequest("Old@1234", "weak", "weak"))))
                    .andExpect(status().isBadRequest());
        }
    }
}
