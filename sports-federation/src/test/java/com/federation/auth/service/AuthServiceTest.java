package com.federation.auth.service;

import com.federation.auth.dto.*;
import com.federation.auth.entity.RefreshToken;
import com.federation.auth.mapper.AuthMapper;
import com.federation.auth.repository.RefreshTokenRepository;
import com.federation.auth.security.FederationUserDetails;
import com.federation.common.config.JwtProperties;
import com.federation.common.exception.BadRequestException;
import com.federation.common.exception.ResourceAlreadyExistsException;
import com.federation.common.exception.UnauthorizedException;
import com.federation.common.util.JwtTokenUtil;
import com.federation.users.entity.User;
import com.federation.users.entity.UserRole;
import com.federation.users.entity.UserStatus;
import com.federation.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock UserRepository         userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder        passwordEncoder;
    @Mock JwtTokenUtil           jwtTokenUtil;
    @Mock JwtProperties          jwtProperties;
    @Mock AuthenticationManager  authManager;
    @Mock UserDetailsService     userDetailsService;
    @Mock AuthMapper             authMapper;

    @InjectMocks AuthService authService;

    private User activeUser;
    private FederationUserDetails userDetails;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .email("user@test.local")
                .username("testuser")
                .password("$2a$12$hashed")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.ROLE_PUBLIC)
                .status(UserStatus.ACTIVE)
                .build();
        // Inject a UUID via reflection workaround — BaseEntity uses @GeneratedValue
        // so we simulate a persisted state by using a real UUID in tests
        userDetails = new FederationUserDetails(activeUser);

        given(jwtProperties.getExpirationMs()).willReturn(3_600_000L);
        given(jwtProperties.getRefreshExpirationMs()).willReturn(86_400_000L);
    }

    // ================================================================
    // Register
    // ================================================================

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        private RegisterRequest validRequest() {
            return new RegisterRequest(
                    "new@test.local", "newuser", "Secret@123", "New", "User");
        }

        @Test
        @DisplayName("saves user and returns token pair")
        void happyPath() {
            given(userRepository.existsByEmail("new@test.local")).willReturn(false);
            given(userRepository.existsByUsername("newuser")).willReturn(false);
            given(passwordEncoder.encode("Secret@123")).willReturn("$hashed$");
            given(userRepository.save(any(User.class))).willReturn(activeUser);
            given(userDetailsService.loadUserByUsername(anyString())).willReturn(userDetails);
            given(jwtTokenUtil.generateAccessToken(any())).willReturn("access-token");
            given(jwtTokenUtil.generateRefreshToken(any())).willReturn("refresh-token");
            given(refreshTokenRepository.save(any())).willReturn(mock(RefreshToken.class));

            AuthResponse response = authService.register(validRequest());

            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo(UserRole.ROLE_PUBLIC);
            assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("throws ResourceAlreadyExistsException when email taken")
        void emailAlreadyTaken() {
            given(userRepository.existsByEmail("new@test.local")).willReturn(true);

            assertThatThrownBy(() -> authService.register(validRequest()))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("new@test.local");
        }

        @Test
        @DisplayName("throws ResourceAlreadyExistsException when username taken")
        void usernameAlreadyTaken() {
            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(userRepository.existsByUsername("newuser")).willReturn(true);

            assertThatThrownBy(() -> authService.register(validRequest()))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("newuser");
        }

        @Test
        @DisplayName("email is normalised to lowercase")
        void emailNormalised() {
            given(userRepository.existsByEmail("new@test.local")).willReturn(false);
            given(userRepository.existsByUsername(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("$hashed$");
            given(userRepository.save(any(User.class))).willReturn(activeUser);
            given(userDetailsService.loadUserByUsername(anyString())).willReturn(userDetails);
            given(jwtTokenUtil.generateAccessToken(any())).willReturn("t");
            given(jwtTokenUtil.generateRefreshToken(any())).willReturn("r");
            given(refreshTokenRepository.save(any())).willReturn(mock(RefreshToken.class));

            RegisterRequest req = new RegisterRequest(
                    "NEW@TEST.LOCAL", "newuser", "Secret@123", "New", "User");
            authService.register(req);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo("new@test.local");
        }
    }

    // ================================================================
    // Login
    // ================================================================

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("returns token pair on valid credentials")
        void happyPath() {
            var authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            given(authManager.authenticate(any())).willReturn(authToken);
            given(userRepository.findByEmailOrUsername("user@test.local"))
                    .willReturn(Optional.of(activeUser));
            given(userDetailsService.loadUserByUsername(anyString())).willReturn(userDetails);
            given(jwtTokenUtil.generateAccessToken(any())).willReturn("access");
            given(jwtTokenUtil.generateRefreshToken(any())).willReturn("refresh");
            given(refreshTokenRepository.save(any())).willReturn(mock(RefreshToken.class));

            AuthResponse response = authService.login(new LoginRequest("user@test.local", "Secret@123"));

            assertThat(response.getAccessToken()).isEqualTo("access");
            verify(userRepository).updateLastLogin(any(), any(Instant.class));
        }

        @Test
        @DisplayName("throws UnauthorizedException on bad credentials")
        void badCredentials() {
            given(authManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("bad"));

            assertThatThrownBy(() -> authService.login(new LoginRequest("x", "y")))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid email/username or password");
        }
    }

    // ================================================================
    // Refresh
    // ================================================================

    @Nested
    @DisplayName("refresh()")
    class RefreshTests {

        @Test
        @DisplayName("rotates token pair for valid refresh token")
        void happyPath() {
            RefreshToken stored = RefreshToken.builder()
                    .user(activeUser)
                    .token("old-refresh")
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revoked(false)
                    .build();

            given(refreshTokenRepository.findByToken("old-refresh"))
                    .willReturn(Optional.of(stored));
            given(refreshTokenRepository.save(any())).willReturn(stored);
            given(userDetailsService.loadUserByUsername(anyString())).willReturn(userDetails);
            given(jwtTokenUtil.generateAccessToken(any())).willReturn("new-access");
            given(jwtTokenUtil.generateRefreshToken(any())).willReturn("new-refresh");

            AuthResponse response = authService.refresh(new RefreshTokenRequest("old-refresh"));

            assertThat(response.getAccessToken()).isEqualTo("new-access");
            // old token must have been marked as revoked
            assertThat(stored.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("throws UnauthorizedException for unknown token")
        void unknownToken() {
            given(refreshTokenRepository.findByToken(anyString())).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("unknown")))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("detects reuse and revokes all sessions")
        void tokenReuseDetected() {
            RefreshToken revoked = RefreshToken.builder()
                    .user(activeUser)
                    .token("reused-token")
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revoked(true)   // already revoked
                    .build();

            given(refreshTokenRepository.findByToken("reused-token"))
                    .willReturn(Optional.of(revoked));

            assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("reused-token")))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("already been used");

            verify(refreshTokenRepository).revokeAllUserTokens(any());
        }

        @Test
        @DisplayName("throws UnauthorizedException for expired token")
        void expiredToken() {
            RefreshToken expired = RefreshToken.builder()
                    .user(activeUser)
                    .token("expired-token")
                    .expiresAt(Instant.now().minusSeconds(60))  // in the past
                    .revoked(false)
                    .build();

            given(refreshTokenRepository.findByToken("expired-token"))
                    .willReturn(Optional.of(expired));

            assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("expired-token")))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("expired");
        }
    }

    // ================================================================
    // Logout
    // ================================================================

    @Nested
    @DisplayName("logout()")
    class LogoutTests {

        @Test
        @DisplayName("revokes all tokens for the user")
        void revokesAll() {
            UUID userId = UUID.randomUUID();
            given(refreshTokenRepository.revokeAllUserTokens(userId)).willReturn(2);

            authService.logout(userId);

            verify(refreshTokenRepository).revokeAllUserTokens(userId);
        }
    }

    // ================================================================
    // Change password
    // ================================================================

    @Nested
    @DisplayName("changePassword()")
    class ChangePasswordTests {

        @Test
        @DisplayName("updates password and revokes all sessions")
        void happyPath() {
            UUID userId = UUID.randomUUID();
            given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));
            given(passwordEncoder.matches("Old@1234", activeUser.getPassword())).willReturn(true);
            given(passwordEncoder.encode("New@1234")).willReturn("$new$hashed$");

            authService.changePassword(userId,
                    new ChangePasswordRequest("Old@1234", "New@1234", "New@1234"));

            verify(userRepository).updatePassword(userId, "$new$hashed$");
            verify(refreshTokenRepository).revokeAllUserTokens(userId);
        }

        @Test
        @DisplayName("throws BadRequestException when passwords do not match")
        void passwordMismatch() {
            UUID userId = UUID.randomUUID();

            assertThatThrownBy(() -> authService.changePassword(userId,
                    new ChangePasswordRequest("Old@1234", "New@1234", "Different@1234")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("do not match");
        }

        @Test
        @DisplayName("throws UnauthorizedException when current password is wrong")
        void wrongCurrentPassword() {
            UUID userId = UUID.randomUUID();
            given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));
            given(passwordEncoder.matches("WrongPass", activeUser.getPassword())).willReturn(false);

            assertThatThrownBy(() -> authService.changePassword(userId,
                    new ChangePasswordRequest("WrongPass", "New@1234", "New@1234")))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("incorrect");
        }
    }
}
