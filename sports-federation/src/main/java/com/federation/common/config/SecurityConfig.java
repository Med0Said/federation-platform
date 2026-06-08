package com.federation.common.config;

import com.federation.users.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Central Spring Security 6 configuration.
 *
 * Authorization model:
 *  - PUBLIC_URLS  → no token required
 *  - GET on /clubs/** and /competitions/** → public read
 *  - /admin/**  → ROLE_ADMIN only
 *  - Everything else → authenticated (any role)
 *
 * Fine-grained method-level authorization uses @PreAuthorize on services/controllers.
 * Session policy is STATELESS — every request is authenticated via JWT.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService      userDetailsService;
    private final CorsProperties          corsProperties;
    private final JwtAuthEntryPoint       authEntryPoint;
    private final JwtAccessDeniedHandler  accessDeniedHandler;

    // ----------------------------------------------------------------
    // URL-level access rules
    // ----------------------------------------------------------------

    private static final String[] PUBLIC_URLS = {
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/actuator/info"
    };

    private static final String[] ADMIN_URLS = {
            "/admin/**",
            "/users/**"
    };

    // ----------------------------------------------------------------
    // Filter chain
    // ----------------------------------------------------------------

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — stateless JWT; CSRF tokens are irrelevant
            .csrf(AbstractHttpConfigurer::disable)

            // CORS — origins configured via app.cors.* properties
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // No HTTP sessions
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Custom JSON error responses
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(authEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth

                    // Open endpoints — no token required
                    .requestMatchers(PUBLIC_URLS).permitAll()
                    .requestMatchers("/auth/logout", "/auth/me", "/auth/change-password").authenticated()

                    // Public read for catalogues
                    .requestMatchers(HttpMethod.GET, "/clubs/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/competitions/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/news/**").permitAll()

                    // Admin-only
                    .requestMatchers(ADMIN_URLS).hasAuthority(UserRole.ADMIN)

                    // Club management — admin or club manager
                    .requestMatchers(HttpMethod.POST, "/clubs/**")
                            .hasAnyAuthority(UserRole.ADMIN, UserRole.CLUB_MANAGER)
                    .requestMatchers(HttpMethod.PUT, "/clubs/**")
                            .hasAnyAuthority(UserRole.ADMIN, UserRole.CLUB_MANAGER)
                    .requestMatchers(HttpMethod.PATCH, "/clubs/**")
                            .hasAnyAuthority(UserRole.ADMIN, UserRole.CLUB_MANAGER)
                    .requestMatchers(HttpMethod.DELETE, "/clubs/**")
                            .hasAuthority(UserRole.ADMIN)

                    // Any authenticated user for everything else
                    .anyRequest().authenticated()
            )

            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ----------------------------------------------------------------
    // Authentication infrastructure
    // ----------------------------------------------------------------

    @Bean
    public AuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Cost factor 12 — good balance of security vs. login latency (~250ms on modern HW)
        return new BCryptPasswordEncoder(12);
    }

    // ----------------------------------------------------------------
    // CORS
    // ----------------------------------------------------------------

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.getAllowedOrigins());
        config.setAllowedMethods(Arrays.asList(corsProperties.getAllowedMethods().split(",")));
        config.setAllowedHeaders(List.of(corsProperties.getAllowedHeaders()));
        config.setAllowCredentials(true);
        config.setMaxAge(corsProperties.getMaxAge());

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
