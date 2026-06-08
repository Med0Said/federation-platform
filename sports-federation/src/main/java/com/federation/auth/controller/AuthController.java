package com.federation.auth.controller;

import com.federation.auth.dto.*;
import com.federation.auth.security.FederationUserDetails;
import com.federation.auth.service.AuthService;
import com.federation.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication REST API.
 *
 * Public endpoints (no token required):
 *   POST /auth/register
 *   POST /auth/login
 *   POST /auth/refresh
 *
 * Authenticated endpoints:
 *   POST   /auth/logout
 *   GET    /auth/me
 *   PATCH  /auth/change-password
 */
@Tag(name = "Authentication", description = "Registration, login, token management and profile")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ----------------------------------------------------------------
    // POST /auth/register
    // ----------------------------------------------------------------

    @Operation(
        summary     = "Register a new account",
        description = "Creates a new ROLE_PUBLIC account and returns a JWT pair. "
                    + "The user is immediately authenticated after registration."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Account created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email or username already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse body = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(body));
    }

    // ----------------------------------------------------------------
    // POST /auth/login
    // ----------------------------------------------------------------

    @Operation(
        summary     = "Login",
        description = "Accepts email or username + password. Returns access token (short-lived) "
                    + "and refresh token (long-lived). Store access token in memory only."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials or account disabled")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse body = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(body, "Login successful."));
    }

    // ----------------------------------------------------------------
    // POST /auth/refresh
    // ----------------------------------------------------------------

    @Operation(
        summary     = "Refresh access token",
        description = "Exchanges a valid refresh token for a new access + refresh pair. "
                    + "The old refresh token is immediately revoked (rotation). "
                    + "Reuse of an already-revoked token triggers full session revocation."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "New token pair issued"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token expired, revoked or unknown")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        AuthResponse body = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok(body, "Token refreshed."));
    }

    // ----------------------------------------------------------------
    // POST /auth/logout
    // ----------------------------------------------------------------

    @Operation(
        summary   = "Logout",
        description = "Revokes all refresh tokens for the authenticated user. "
                    + "Access tokens remain valid until natural expiry — keep TTL short.",
        security  = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged out"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal FederationUserDetails principal) {

        authService.logout(principal.getId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    // ----------------------------------------------------------------
    // GET /auth/me
    // ----------------------------------------------------------------

    @Operation(
        summary  = "Get my profile",
        description = "Returns the authenticated user's profile without sensitive fields.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponse>> me(
            @AuthenticationPrincipal FederationUserDetails principal) {

        UserProfileResponse profile = authService.getProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    // ----------------------------------------------------------------
    // PATCH /auth/change-password
    // ----------------------------------------------------------------

    @Operation(
        summary  = "Change password",
        description = "Verifies the current password, applies the new one and revokes "
                    + "all refresh tokens (forces re-login on other devices).",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password changed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or passwords do not match"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Current password incorrect")
    })
    @PatchMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal FederationUserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request) {

        authService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Password updated. Please log in again."));
    }
}
