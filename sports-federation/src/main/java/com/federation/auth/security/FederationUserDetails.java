package com.federation.auth.security;

import com.federation.users.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Custom {@link UserDetails} wrapping the {@link User} domain entity.
 *
 * Advantages over Spring's default User object:
 *  - Carries the UUID and display fields so controllers never hit the DB again.
 *  - Account locking/disabling maps directly from {@link com.federation.users.entity.UserStatus}.
 *  - Email is the principal identifier (used as "username" in Spring Security).
 */
@Getter
public class FederationUserDetails implements UserDetails {

    private final UUID   id;
    private final String email;
    private final String usernameValue;
    private final String password;
    private final boolean active;
    private final boolean nonLocked;
    private final Collection<? extends GrantedAuthority> authorities;

    public FederationUserDetails(User user) {
        this.id            = user.getId();
        this.email         = user.getEmail();
        this.usernameValue = user.getUsername();
        this.password      = user.getPassword();
        this.active        = user.isActive();
        this.nonLocked     = user.isAccountNonLocked();
        this.authorities   = List.of(new SimpleGrantedAuthority(user.getRole().name()));
    }

    // Spring Security contract — email used as unique principal name
    @Override public String getUsername()                                  { return email; }
    @Override public String getPassword()                                  { return password; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public boolean isEnabled()                                   { return active; }
    @Override public boolean isAccountNonLocked()                          { return nonLocked; }
    @Override public boolean isAccountNonExpired()                         { return true; }
    @Override public boolean isCredentialsNonExpired()                     { return true; }

    /** The display username (different from the Spring Security principal name which is email). */
    public String getDisplayUsername() { return usernameValue; }
}
