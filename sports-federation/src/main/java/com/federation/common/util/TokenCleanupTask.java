package com.federation.common.util;

import com.federation.auth.repository.PasswordResetTokenRepository;
import com.federation.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Nightly housekeeping job — purges expired and revoked refresh tokens and
 * expired or used password reset tokens.
 *
 * Without this, the token tables grow unboundedly.
 * Runs at 02:00 AM server time every day.
 * Logs the number of rows deleted for auditability.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupTask {

    private final RefreshTokenRepository       refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeExpiredAndRevokedTokens() {
        log.info("Token cleanup starting…");
        Instant now = Instant.now();
        int refresh = refreshTokenRepository.deleteExpiredAndRevoked(now);
        int reset   = passwordResetTokenRepository.deleteExpiredAndUsed(now);
        log.info("Token cleanup complete — {} refresh and {} reset token row(s) removed.", refresh, reset);
    }
}
