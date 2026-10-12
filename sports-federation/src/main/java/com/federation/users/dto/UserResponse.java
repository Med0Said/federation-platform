package com.federation.users.dto;

import com.federation.users.entity.UserRole;
import com.federation.users.entity.UserStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Full user view returned to administrators. Never contains the password hash. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {
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
    private Instant    updatedAt;
    /** Athlete record linked to this account, if any. */
    private UUID       athleteId;
    /** Temporary password — only present in the response of an admin password reset. */
    private String     temporaryPassword;
}
