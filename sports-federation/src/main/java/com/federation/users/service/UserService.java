package com.federation.users.service;

import com.federation.athletes.repository.AthleteRepository;
import com.federation.auth.repository.RefreshTokenRepository;
import com.federation.common.exception.BusinessRuleViolationException;
import com.federation.common.exception.ResourceAlreadyExistsException;
import com.federation.common.exception.ResourceNotFoundException;
import com.federation.common.response.PagedResponse;
import com.federation.common.util.SecurityUtils;
import com.federation.common.util.Specs;
import com.federation.users.dto.CreateUserRequest;
import com.federation.users.dto.UpdateUserRequest;
import com.federation.users.dto.UserResponse;
import com.federation.users.entity.User;
import com.federation.users.entity.UserRole;
import com.federation.users.entity.UserStatus;
import com.federation.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;

/**
 * Account administration (ADMIN only) plus self-service profile updates.
 *
 * Safety rules: an administrator cannot demote, suspend or delete their own
 * account, and the last active administrator can never be removed.
 * Role / status changes and password resets revoke the user's refresh tokens.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository         userRepository;
    private final AthleteRepository      athleteRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder        passwordEncoder;

    // ── Admin: read ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public PagedResponse<UserResponse> findAll(String search, String role, String status, Pageable pageable) {
        Specification<User> spec = Specification
                .where(Specs.<User>search(search, "email", "username", "firstName", "lastName"))
                .and(Specs.eq("role",   Specs.parseEnum(UserRole.class, role)))
                .and(Specs.eq("status", Specs.parseEnum(UserStatus.class, status)));
        return PagedResponse.of(userRepository.findAll(spec, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public UserResponse findById(UUID id) {
        return toResponse(getOrThrow(id));
    }

    // ── Admin: write ──────────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public UserResponse create(CreateUserRequest request) {
        String email = request.getEmail().strip().toLowerCase(Locale.ROOT);
        String username = request.getUsername().strip();
        if (userRepository.existsByEmail(email)) {
            throw new ResourceAlreadyExistsException("An account with email '" + email + "' already exists.");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ResourceAlreadyExistsException("Username '" + username + "' is already taken.");
        }
        User user = User.builder()
                .email(email)
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().strip())
                .lastName(request.getLastName().strip())
                .phone(request.getPhone())
                .role(request.getRole())
                .status(request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE)
                .build();
        User saved = userRepository.save(user);
        log.info("Admin created user {} with role {}", saved.getEmail(), saved.getRole());
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = getOrThrow(id);
        applyProfile(user, request);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public UserResponse changeRole(UUID id, UserRole role) {
        User user = getOrThrow(id);
        if (user.getRole() == role) return toResponse(user);
        if (isSelf(id)) {
            throw new BusinessRuleViolationException("You cannot change your own role.");
        }
        if (user.getRole() == UserRole.ROLE_ADMIN) ensureAnotherActiveAdmin();
        user.setRole(role);
        userRepository.save(user);
        refreshTokenRepository.revokeAllUserTokens(id);
        log.info("Role of user {} changed to {}", id, role);
        return toResponse(getOrThrow(id));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public UserResponse changeStatus(UUID id, UserStatus status) {
        User user = getOrThrow(id);
        if (user.getStatus() == status) return toResponse(user);
        if (isSelf(id) && status != UserStatus.ACTIVE) {
            throw new BusinessRuleViolationException("You cannot deactivate your own account.");
        }
        if (user.getRole() == UserRole.ROLE_ADMIN && user.getStatus() == UserStatus.ACTIVE) {
            ensureAnotherActiveAdmin();
        }
        user.setStatus(status);
        userRepository.save(user);
        if (status != UserStatus.ACTIVE) refreshTokenRepository.revokeAllUserTokens(id);
        log.info("Status of user {} changed to {}", id, status);
        return toResponse(getOrThrow(id));
    }

    /**
     * Generates a random temporary password, returns it once in the response
     * and logs the user out everywhere.
     */
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public UserResponse resetPassword(UUID id) {
        User user = getOrThrow(id);
        String temporary = generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(temporary));
        userRepository.save(user);
        refreshTokenRepository.revokeAllUserTokens(id);
        log.info("Admin reset the password of user {}", id);
        UserResponse response = toResponse(getOrThrow(id));
        response.setTemporaryPassword(temporary);
        return response;
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void delete(UUID id) {
        User user = getOrThrow(id);
        if (isSelf(id)) {
            throw new BusinessRuleViolationException("You cannot delete your own account.");
        }
        if (user.getRole() == UserRole.ROLE_ADMIN && user.getStatus() == UserStatus.ACTIVE) {
            ensureAnotherActiveAdmin();
        }
        userRepository.delete(user);
        userRepository.flush();
        log.info("User {} deleted", id);
    }

    // ── Self-service ──────────────────────────────────────────────────────

    @Transactional
    public User updateOwnProfile(UUID userId, UpdateUserRequest request) {
        User user = getOrThrow(userId);
        applyProfile(user, request);
        return userRepository.save(user);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private User getOrThrow(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private void applyProfile(User user, UpdateUserRequest request) {
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName().strip());
        if (request.getLastName()  != null) user.setLastName(request.getLastName().strip());
        if (request.getPhone()     != null) user.setPhone(request.getPhone().isBlank() ? null : request.getPhone().strip());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl().isBlank() ? null : request.getAvatarUrl().strip());
    }

    private boolean isSelf(UUID id) {
        return SecurityUtils.currentUserId().map(id::equals).orElse(false);
    }

    private void ensureAnotherActiveAdmin() {
        long activeAdmins = userRepository.countByRoleAndStatus(UserRole.ROLE_ADMIN, UserStatus.ACTIVE);
        if (activeAdmins <= 1) {
            throw new BusinessRuleViolationException("The platform must keep at least one active administrator.");
        }
    }

    private static String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder("Tmp@");
        for (int i = 0; i < 8; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        sb.append(RANDOM.nextInt(10));
        return sb.toString();
    }

    private UserResponse toResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .username(u.getUsername())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .fullName(u.getFullName())
                .role(u.getRole())
                .status(u.getStatus())
                .phone(u.getPhone())
                .avatarUrl(u.getAvatarUrl())
                .lastLogin(u.getLastLogin())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .athleteId(athleteRepository.findByUserId(u.getId()).map(a -> a.getId()).orElse(null))
                .build();
    }
}
