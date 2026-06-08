package com.federation.auth.security;

import com.federation.users.entity.User;
import com.federation.users.entity.UserRole;
import com.federation.users.entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FederationUserDetails")
class FederationUserDetailsTest {

    private User buildUser(UserStatus status, UserRole role) {
        return User.builder()
                .email("user@test.local")
                .username("testuser")
                .password("$2a$12$hash")
                .firstName("Test")
                .lastName("User")
                .role(role)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("email is used as Spring Security username")
    void emailIsUsername() {
        var details = new FederationUserDetails(buildUser(UserStatus.ACTIVE, UserRole.ROLE_PUBLIC));
        assertThat(details.getUsername()).isEqualTo("user@test.local");
    }

    @Test
    @DisplayName("display username comes from entity username field")
    void displayUsername() {
        var details = new FederationUserDetails(buildUser(UserStatus.ACTIVE, UserRole.ROLE_PUBLIC));
        assertThat(details.getDisplayUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("ACTIVE user is enabled and not locked")
    void activeUserIsEnabled() {
        var details = new FederationUserDetails(buildUser(UserStatus.ACTIVE, UserRole.ROLE_PUBLIC));
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("SUSPENDED user is locked")
    void suspendedUserIsLocked() {
        var details = new FederationUserDetails(buildUser(UserStatus.SUSPENDED, UserRole.ROLE_PUBLIC));
        assertThat(details.isAccountNonLocked()).isFalse();
    }

    @Test
    @DisplayName("INACTIVE user is disabled")
    void inactiveUserIsDisabled() {
        var details = new FederationUserDetails(buildUser(UserStatus.INACTIVE, UserRole.ROLE_PUBLIC));
        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("authority matches role name")
    void authorityMatchesRole() {
        var details = new FederationUserDetails(buildUser(UserStatus.ACTIVE, UserRole.ROLE_ADMIN));
        assertThat(details.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("account is never expired and credentials never expired")
    void neverExpires() {
        var details = new FederationUserDetails(buildUser(UserStatus.ACTIVE, UserRole.ROLE_PUBLIC));
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
    }
}
