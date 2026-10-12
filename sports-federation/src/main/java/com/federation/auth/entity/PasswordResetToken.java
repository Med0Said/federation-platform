package com.federation.auth.entity;

import com.federation.common.util.BaseEntity;
import com.federation.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Single-use password reset token (only its SHA-256 hash is persisted). */
@Entity
@Table(name = "password_reset_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordResetToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    public boolean isUsable() {
        return usedAt == null && Instant.now().isBefore(expiresAt);
    }
}
