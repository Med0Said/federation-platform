package com.federation.auth.repository;

import com.federation.auth.entity.RefreshToken;
import com.federation.users.entity.User;
import com.federation.users.entity.UserRole;
import com.federation.users.entity.UserStatus;
import com.federation.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("RefreshTokenRepository — @DataJpaTest")
class RefreshTokenRepositoryTest {

    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired UserRepository         userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = userRepository.save(User.builder()
                .email("repo@test.local")
                .username("repo_user")
                .password("$hashed$")
                .firstName("Repo")
                .lastName("User")
                .role(UserRole.ROLE_PUBLIC)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private RefreshToken saveToken(String value, boolean revoked, Instant expiresAt) {
        return refreshTokenRepository.save(RefreshToken.builder()
                .user(savedUser)
                .token(value)
                .revoked(revoked)
                .expiresAt(expiresAt)
                .build());
    }

    @Test
    @DisplayName("findByToken — returns token when it exists")
    void findByToken_found() {
        saveToken("my-token", false, Instant.now().plusSeconds(3600));
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("my-token");
        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("my-token");
    }

    @Test
    @DisplayName("findByToken — returns empty for unknown token")
    void findByToken_notFound() {
        assertThat(refreshTokenRepository.findByToken("unknown")).isEmpty();
    }

    @Test
    @DisplayName("revokeAllUserTokens — marks all active tokens as revoked")
    void revokeAll() {
        saveToken("token-a", false, Instant.now().plusSeconds(3600));
        saveToken("token-b", false, Instant.now().plusSeconds(3600));
        saveToken("token-c", true,  Instant.now().plusSeconds(3600)); // already revoked

        int count = refreshTokenRepository.revokeAllUserTokens(savedUser.getId());

        assertThat(count).isEqualTo(2); // only 2 active ones affected

        refreshTokenRepository.findAll().forEach(t ->
                assertThat(t.isRevoked()).isTrue());
    }

    @Test
    @DisplayName("revokeByToken — revokes only the specified token")
    void revokeByToken() {
        saveToken("revoke-me", false, Instant.now().plusSeconds(3600));
        saveToken("keep-me",   false, Instant.now().plusSeconds(3600));

        refreshTokenRepository.revokeByToken("revoke-me");

        assertThat(refreshTokenRepository.findByToken("revoke-me").get().isRevoked()).isTrue();
        assertThat(refreshTokenRepository.findByToken("keep-me").get().isRevoked()).isFalse();
    }

    @Test
    @DisplayName("deleteExpiredAndRevoked — removes only eligible rows")
    void deleteExpiredAndRevoked() {
        saveToken("expired",  false, Instant.now().minusSeconds(60));  // expired
        saveToken("revoked",  true,  Instant.now().plusSeconds(3600)); // revoked
        saveToken("active",   false, Instant.now().plusSeconds(3600)); // valid

        int deleted = refreshTokenRepository.deleteExpiredAndRevoked(Instant.now());

        assertThat(deleted).isEqualTo(2);
        assertThat(refreshTokenRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.findByToken("active")).isPresent();
    }

    @Test
    @DisplayName("countActiveSessions — returns only non-revoked, non-expired count")
    void countActiveSessions() {
        saveToken("s1", false, Instant.now().plusSeconds(3600)); // active
        saveToken("s2", false, Instant.now().plusSeconds(3600)); // active
        saveToken("s3", true,  Instant.now().plusSeconds(3600)); // revoked
        saveToken("s4", false, Instant.now().minusSeconds(60));  // expired

        long count = refreshTokenRepository.countActiveSessions(
                savedUser.getId(), Instant.now());

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("RefreshToken.isValid() returns false when revoked")
    void isValidRevoked() {
        RefreshToken token = saveToken("t", true, Instant.now().plusSeconds(3600));
        assertThat(token.isValid()).isFalse();
    }

    @Test
    @DisplayName("RefreshToken.isValid() returns false when expired")
    void isValidExpired() {
        RefreshToken token = saveToken("t", false, Instant.now().minusSeconds(1));
        assertThat(token.isValid()).isFalse();
    }

    @Test
    @DisplayName("RefreshToken.isValid() returns true when active")
    void isValidActive() {
        RefreshToken token = saveToken("t", false, Instant.now().plusSeconds(3600));
        assertThat(token.isValid()).isTrue();
    }
}
