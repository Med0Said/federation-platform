package com.federation.auth.entity;

import com.federation.users.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent refresh token — one row per active session per user.
 *
 * Rotation strategy: on every refresh, old token is revoked and a new pair issued.
 * Re-use of an already-revoked token triggers full session revocation (theft detection).
 * Nightly cleanup of expired/revoked rows via {@link com.federation.common.util.TokenCleanupTask}.
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
        @Index(name = "idx_refresh_tokens_token",   columnList = "token")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The opaque JWT string — stored for lookup and revocation checks. */
    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ----------------------------------------------------------------
    // Domain logic
    // ----------------------------------------------------------------

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /** A token is valid only when it is neither revoked nor past its expiry. */
    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
