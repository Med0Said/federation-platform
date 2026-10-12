package com.federation.common.util;

import com.federation.auth.security.FederationUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/** Static accessors for the authenticated principal held in the SecurityContext. */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<FederationUserDetails> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FederationUserDetails details) {
            return Optional.of(details);
        }
        return Optional.empty();
    }

    public static Optional<UUID> currentUserId() {
        return currentUser().map(FederationUserDetails::getId);
    }

    public static boolean hasAnyAuthority(String... authorities) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> Arrays.asList(authorities).contains(a));
    }
}
