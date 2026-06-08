package com.federation.users.service;

import com.federation.auth.security.FederationUserDetails;
import com.federation.users.entity.User;
import com.federation.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridges the users module and Spring Security.
 *
 * Accepts both email and username — the {@code findByEmailOrUsername} query
 * in {@link UserRepository} handles the OR condition in a single DB round-trip.
 * Returns a {@link FederationUserDetails} wrapper so the SecurityContext holds
 * the full UUID and display fields without extra lookups.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByEmailOrUsername(usernameOrEmail)
                .orElseThrow(() -> {
                    log.warn("No user found for identifier: {}", usernameOrEmail);
                    return new UsernameNotFoundException(
                            "No account found for: " + usernameOrEmail);
                });

        log.debug("Loaded principal: {} [role={}, status={}]",
                user.getEmail(), user.getRole(), user.getStatus());

        return new FederationUserDetails(user);
    }
}
