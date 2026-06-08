package com.federation.auth;

import com.federation.common.config.JwtProperties;
import com.federation.common.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenUtil")
class JwtTokenUtilTest {

    // A 256-bit base64-encoded secret for tests
    private static final String TEST_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private JwtTokenUtil jwtTokenUtil;
    private UserDetails  userDetails;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(TEST_SECRET);
        props.setExpirationMs(3_600_000L);         // 1 hour
        props.setRefreshExpirationMs(86_400_000L); // 24 hours

        jwtTokenUtil = new JwtTokenUtil(props);

        userDetails = User.builder()
                .username("test@federation.local")
                .password("irrelevant")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_PUBLIC")))
                .build();
    }

    // ----------------------------------------------------------------
    // Access token
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("Access token")
    class AccessTokenTests {

        @Test
        @DisplayName("generates a non-blank token")
        void generates() {
            String token = jwtTokenUtil.generateAccessToken(userDetails);
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("subject equals user email")
        void subjectIsEmail() {
            String token = jwtTokenUtil.generateAccessToken(userDetails);
            assertThat(jwtTokenUtil.extractUsername(token))
                    .isEqualTo("test@federation.local");
        }

        @Test
        @DisplayName("contains roles claim")
        void containsRoles() {
            String token = jwtTokenUtil.generateAccessToken(userDetails);
            List<String> roles = jwtTokenUtil.extractRoles(token);
            assertThat(roles).containsExactly("ROLE_PUBLIC");
        }

        @Test
        @DisplayName("isTokenValid returns true for matching principal")
        void validForMatchingPrincipal() {
            String token = jwtTokenUtil.generateAccessToken(userDetails);
            assertThat(jwtTokenUtil.isTokenValid(token, userDetails)).isTrue();
        }

        @Test
        @DisplayName("isTokenValid returns false for different principal")
        void invalidForDifferentPrincipal() {
            String token = jwtTokenUtil.generateAccessToken(userDetails);
            UserDetails other = User.builder()
                    .username("other@federation.local")
                    .password("x")
                    .authorities(List.of())
                    .build();
            assertThat(jwtTokenUtil.isTokenValid(token, other)).isFalse();
        }

        @Test
        @DisplayName("validateToken returns true for fresh token")
        void validateFreshToken() {
            String token = jwtTokenUtil.generateAccessToken(userDetails);
            assertThat(jwtTokenUtil.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("validateToken returns false for garbage string")
        void validateGarbage() {
            assertThat(jwtTokenUtil.validateToken("not.a.jwt")).isFalse();
        }

        @Test
        @DisplayName("validateToken returns false for empty string")
        void validateEmpty() {
            assertThat(jwtTokenUtil.validateToken("")).isFalse();
        }

        @Test
        @DisplayName("isAccessToken returns true")
        void isAccessToken() {
            String token = jwtTokenUtil.generateAccessToken(userDetails);
            assertThat(jwtTokenUtil.isAccessToken(token)).isTrue();
            assertThat(jwtTokenUtil.isRefreshToken(token)).isFalse();
        }

        @Test
        @DisplayName("getRemainingTtlSeconds is positive for fresh token")
        void ttlPositive() {
            String token = jwtTokenUtil.generateAccessToken(userDetails);
            assertThat(jwtTokenUtil.getRemainingTtlSeconds(token)).isPositive();
        }
    }

    // ----------------------------------------------------------------
    // Refresh token
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("Refresh token")
    class RefreshTokenTests {

        @Test
        @DisplayName("generates a non-blank refresh token")
        void generates() {
            String token = jwtTokenUtil.generateRefreshToken(userDetails);
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("isRefreshToken returns true")
        void isRefreshToken() {
            String token = jwtTokenUtil.generateRefreshToken(userDetails);
            assertThat(jwtTokenUtil.isRefreshToken(token)).isTrue();
            assertThat(jwtTokenUtil.isAccessToken(token)).isFalse();
        }

        @Test
        @DisplayName("access and refresh tokens are different strings")
        void tokensAreDifferent() {
            String access  = jwtTokenUtil.generateAccessToken(userDetails);
            String refresh = jwtTokenUtil.generateRefreshToken(userDetails);
            assertThat(access).isNotEqualTo(refresh);
        }

        @Test
        @DisplayName("subject equals user email")
        void subjectIsEmail() {
            String token = jwtTokenUtil.generateRefreshToken(userDetails);
            assertThat(jwtTokenUtil.extractUsername(token))
                    .isEqualTo("test@federation.local");
        }
    }

    // ----------------------------------------------------------------
    // Expired token
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("Expired token")
    class ExpiredTokenTests {

        @Test
        @DisplayName("validateToken returns false for expired token")
        void expiredTokenFails() throws Exception {
            // Build a util with 1ms TTL so the token expires instantly
            JwtProperties shortProps = new JwtProperties();
            shortProps.setSecret(TEST_SECRET);
            shortProps.setExpirationMs(1L);
            shortProps.setRefreshExpirationMs(1L);

            JwtTokenUtil shortUtil = new JwtTokenUtil(shortProps);
            String token = shortUtil.generateAccessToken(userDetails);

            Thread.sleep(10); // ensure expiry

            assertThat(shortUtil.validateToken(token)).isFalse();
        }
    }
}
