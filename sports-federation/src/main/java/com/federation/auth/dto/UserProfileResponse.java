package com.federation.auth.dto;

import com.federation.users.entity.UserRole;
import com.federation.users.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only view of the currently authenticated user's profile.
 * Returned by {@code GET /auth/me}.
 * Never contains the password hash.
 */
@Schema(description = "Authenticated user's profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private UUID       id;
    private String     email;
    private String     username;
    private String     firstName;
    private String     lastName;
    private String     fullName;
    private UserRole   role;
    private UserStatus status;
    private String     phone;
    private String     avatarUrl;
    private Instant    lastLogin;
    private Instant    createdAt;
}
