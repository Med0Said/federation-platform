package com.federation.auth.service;

import com.federation.auth.dto.ResetPasswordRequest;
import com.federation.auth.entity.PasswordResetToken;
import com.federation.auth.repository.PasswordResetTokenRepository;
import com.federation.auth.repository.RefreshTokenRepository;
import com.federation.common.exception.BadRequestException;
import com.federation.common.mail.MailService;
import com.federation.users.entity.User;
import com.federation.users.entity.UserStatus;
import com.federation.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/**
 * "Forgot password" flow:
 * <ol>
 *   <li>{@link #requestReset(String)} — always succeeds from the caller's point of view
 *       (no account enumeration); when the account exists a single-use token valid for
 *       {@code app.password-reset.ttl} is e-mailed as a link to the frontend.</li>
 *   <li>{@link #resetPassword(ResetPasswordRequest)} — consumes the token, sets the new
 *       password and revokes every refresh token of the user.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository               userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenRepository       refreshTokenRepository;
    private final PasswordEncoder              passwordEncoder;
    private final MailService                  mailService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${app.password-reset.ttl:PT1H}")
    private Duration ttl;

    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(email.strip().toLowerCase(Locale.ROOT))
                .filter(u -> u.getStatus() != UserStatus.SUSPENDED)
                .ifPresentOrElse(this::issueToken,
                        () -> log.info("Password reset requested for unknown or suspended account"));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirmation do not match.");
        }
        PasswordResetToken token = tokenRepository.findByTokenHash(hash(request.getToken()))
                .filter(PasswordResetToken::isUsable)
                .orElseThrow(() -> new BadRequestException(
                        "This password reset link is invalid or has expired. Please request a new one."));

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        tokenRepository.invalidateAllForUser(user.getId(), Instant.now());
        refreshTokenRepository.revokeAllUserTokens(user.getId());
        log.info("Password reset completed for user {}", user.getId());
    }

    private void issueToken(User user) {
        tokenRepository.invalidateAllForUser(user.getId(), Instant.now());

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        tokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(hash(raw))
                .expiresAt(Instant.now().plus(ttl))
                .build());

        String link = frontendUrl.replaceAll("/+$", "") + "/auth/reset-password?token=" + raw;
        mailService.send(user.getEmail(), "Reset your Sports Federation password",
                "Hello " + user.getFirstName() + ",\n\n"
              + "We received a request to reset your password. Open the link below to choose a new one:\n\n"
              + link + "\n\n"
              + "The link expires in " + ttl.toMinutes() + " minutes and can be used once.\n"
              + "If you did not request this, you can ignore this e-mail.\n");
        log.info("Password reset token issued for user {}", user.getId());
    }

    static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
