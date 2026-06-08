package com.federation.users.entity;

import com.federation.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Core user account entity.
 *
 * Mapped to the {@code users} table (schema defined in V1__init_schema.sql).
 * Spring Security integration is handled by {@code UserDetailsServiceImpl}.
 *
 * Note: role and status use EnumType.STRING so both PostgreSQL (custom enum columns)
 * and H2 (VARCHAR fallback in tests) work without DDL differences.
 */
@Entity
@Table(
    name   = "users",
    schema = "public",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_users_email",    columnNames = "email"),
        @UniqueConstraint(name = "uq_users_username", columnNames = "username")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    // ----------------------------------------------------------------
    // Identity
    // ----------------------------------------------------------------

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    /** BCrypt-hashed — never serialised into DTOs. */
    @Column(nullable = false, length = 255)
    private String password;

    // ----------------------------------------------------------------
    // Profile
    // ----------------------------------------------------------------

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    // ----------------------------------------------------------------
    // Security & lifecycle
    // ----------------------------------------------------------------

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(nullable = false, length = 40, columnDefinition = "user_role")
    @Builder.Default
    private UserRole role = UserRole.ROLE_PUBLIC;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(nullable = false, length = 30, columnDefinition = "user_status")
    @Builder.Default
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Column(name = "last_login")
    private Instant lastLogin;

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isActive() {
        return UserStatus.ACTIVE.equals(status);
    }

    public boolean isAccountNonLocked() {
        return status != UserStatus.SUSPENDED;
    }
}
