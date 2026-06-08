package com.federation.common.util;

import com.federation.common.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Stateless JWT utility — encapsulates all JJWT interactions.
 *
 * Access token payload:
 *   sub   → user email (Spring Security username)
 *   roles → list of GrantedAuthority names
 *   iat   → issued-at
 *   exp   → expiry
 *
 * Refresh token payload:
 *   sub   → user email
 *   type  → "REFRESH"
 *   iat / exp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenUtil {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE  = "type";
    private static final String TYPE_ACCESS  = "ACCESS";
    private static final String TYPE_REFRESH = "REFRESH";

    private final JwtProperties jwtProperties;

    // ----------------------------------------------------------------
    // Generation
    // ----------------------------------------------------------------

    public String generateAccessToken(UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_ROLES, roles);
        claims.put(CLAIM_TYPE, TYPE_ACCESS);

        return buildToken(claims, userDetails.getUsername(), jwtProperties.getExpirationMs());
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TYPE, TYPE_REFRESH);
        return buildToken(claims, userDetails.getUsername(), jwtProperties.getRefreshExpirationMs());
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long ttlMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMs))
                .signWith(getSigningKey())
                .compact();
    }

    // ----------------------------------------------------------------
    // Validation
    // ----------------------------------------------------------------

    /**
     * Full validation — signature, expiry, subject match.
     * Use this when you already have the UserDetails in hand.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String subject = extractUsername(token);
            return subject.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Structural validation — signature and expiry only.
     * Used in the filter before UserDetails are loaded.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SignatureException e) {
            log.warn("JWT: invalid signature");
        } catch (MalformedJwtException e) {
            log.warn("JWT: malformed token");
        } catch (ExpiredJwtException e) {
            log.warn("JWT: token expired");
        } catch (UnsupportedJwtException e) {
            log.warn("JWT: unsupported token");
        } catch (IllegalArgumentException e) {
            log.warn("JWT: empty claims");
        }
        return false;
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(extractClaim(token, c -> c.get(CLAIM_TYPE, String.class)));
    }

    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(extractClaim(token, c -> c.get(CLAIM_TYPE, String.class)));
    }

    // ----------------------------------------------------------------
    // Claims extraction
    // ----------------------------------------------------------------

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, c -> c.get(CLAIM_ROLES, List.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(parseClaims(token));
    }

    /** Returns remaining TTL in seconds; 0 if already expired. */
    public long getRemainingTtlSeconds(String token) {
        try {
            Date expiry = extractExpiration(token);
            long diff = expiry.getTime() - System.currentTimeMillis();
            return diff > 0 ? diff / 1000 : 0L;
        } catch (JwtException e) {
            return 0L;
        }
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
