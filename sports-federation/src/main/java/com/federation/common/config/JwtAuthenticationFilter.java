package com.federation.common.config;

import com.federation.common.util.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter — runs once per request, before Spring Security's
 * {@code UsernamePasswordAuthenticationFilter}.
 *
 * Flow:
 *  1. Extract Bearer token from Authorization header.
 *  2. Validate signature and expiry (no DB hit yet).
 *  3. Extract subject (email) from claims.
 *  4. Load {@link UserDetails} from DB only when the SecurityContext is empty.
 *  5. Re-validate token against the loaded principal.
 *  6. Inject {@link UsernamePasswordAuthenticationToken} into the SecurityContext.
 *
 * Deliberately rejects refresh tokens used where an access token is expected.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER  = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenUtil       jwtTokenUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         filterChain) throws ServletException, IOException {

        final String jwt = extractToken(request);

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 1: structural validation (no DB hit)
        if (!jwtTokenUtil.validateToken(jwt)) {
            log.debug("JWT failed structural validation — request: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // Step 2: reject refresh tokens presented to protected endpoints
        if (jwtTokenUtil.isRefreshToken(jwt)) {
            log.warn("Refresh token used as access token for {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        final String email = jwtTokenUtil.extractUsername(jwt);

        // Step 3: only populate context when it's empty (idempotent)
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (jwtTokenUtil.isTokenValid(jwt, userDetails)) {
                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authenticated [{}] for {} {}",
                        email, request.getMethod(), request.getRequestURI());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
