package com.federation.auth.dto;

import com.federation.users.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

/**
 * Returned on successful login, registration and token refresh.
 *
 * Consumers must:
 *   1. Store {@code accessToken} in memory (not localStorage).
 *   2. Store {@code refreshToken} in an httpOnly cookie or secure storage.
 *   3. Re-use {@code accessToken} for all API calls until {@code expiresIn} elapses.
 *   4. Call {@code POST /auth/refresh} with the refresh token to get a new pair.
 */
@Schema(description = "Authentication token pair and principal summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    @Schema(description = "Short-lived JWT access token", example = "eyJhbGci...")
    private String accessToken;

    @Schema(description = "Long-lived refresh token for silent renewal")
    private String refreshToken;

    @Schema(description = "Always 'Bearer'", example = "Bearer")
    private String tokenType;

    @Schema(description = "Access token lifetime in seconds", example = "86400")
    private long expiresIn;

    @Schema(description = "Authenticated user UUID")
    private UUID userId;

    @Schema(description = "Email of the authenticated user")
    private String email;

    @Schema(description = "Username of the authenticated user")
    private String username;

    @Schema(description = "Assigned application role")
    private UserRole role;

    // ----------------------------------------------------------------
    // Factory
    // ----------------------------------------------------------------

    public static AuthResponse of(String accessToken, String refreshToken, long expiresInSeconds,
                                  UUID userId, String email, String username, UserRole role) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresInSeconds)
                .userId(userId)
                .email(email)
                .username(username)
                .role(role)
                .build();
    }
}
