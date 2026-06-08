package com.federation.common.util;

import com.federation.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Nightly housekeeping job — purges expired and revoked refresh tokens.
 *
 * Without this, the refresh_tokens table grows unboundedly.
 * Runs at 02:00 AM server time every day.
 * Logs the number of rows deleted for auditability.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupTask {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeExpiredAndRevokedTokens() {
        log.info("Token cleanup starting…");
        int deleted = refreshTokenRepository.deleteExpiredAndRevoked(Instant.now());
        log.info("Token cleanup complete — {} row(s) removed.", deleted);
    }
}
