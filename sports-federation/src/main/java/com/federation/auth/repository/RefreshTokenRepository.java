package com.federation.auth.repository;

import com.federation.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    /** Revoke every active token for a user — called on logout. */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true " +
           "WHERE rt.user.id = :userId AND rt.revoked = false")
    int revokeAllUserTokens(@Param("userId") UUID userId);

    /** Revoke a specific token by its value — called on token rotation. */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.token = :token")
    void revokeByToken(@Param("token") String token);

    /** Housekeeping — called nightly by {@link com.federation.common.util.TokenCleanupTask}. */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.revoked = true")
    int deleteExpiredAndRevoked(@Param("now") Instant now);

    /** Count active (non-revoked, non-expired) sessions for a user. */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt " +
           "WHERE rt.user.id = :userId AND rt.revoked = false AND rt.expiresAt > :now")
    long countActiveSessions(@Param("userId") UUID userId, @Param("now") Instant now);
}
