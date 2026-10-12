package com.federation.clubs.service;

import com.federation.athletes.entity.AthleteStatus;
import com.federation.athletes.repository.AthleteRepository;
import com.federation.clubs.dto.ClubRequest;
import com.federation.clubs.dto.ClubResponse;
import com.federation.clubs.entity.Club;
import com.federation.clubs.entity.ClubStatus;
import com.federation.clubs.mapper.ClubMapper;
import com.federation.clubs.repository.ClubRepository;
import com.federation.common.exception.BusinessRuleViolationException;
import com.federation.common.exception.ForbiddenException;
import com.federation.common.exception.ResourceAlreadyExistsException;
import com.federation.common.exception.ResourceNotFoundException;
import com.federation.common.response.PagedResponse;
import com.federation.common.security.AccessControl;
import com.federation.common.util.SecurityUtils;
import com.federation.common.util.Specs;
import com.federation.users.entity.User;
import com.federation.users.entity.UserRole;
import com.federation.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository    clubRepository;
    private final AthleteRepository athleteRepository;
    private final UserRepository    userRepository;
    private final ClubMapper        clubMapper;
    private final AccessControl     access;

    // ── Read ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<ClubResponse> findAll(String search, String status, Pageable pageable) {
        Specification<Club> spec = Specification
                .where(Specs.<Club>search(search, "name", "shortName", "city"))
                .and(Specs.eq("status", Specs.parseEnum(ClubStatus.class, status)));
        return PagedResponse.of(clubRepository.findAll(spec, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ClubResponse findById(UUID id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public ClubResponse findBySlug(String slug) {
        Club club = clubRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Club", "slug", slug));
        return toResponse(club);
    }

    /** Clubs managed by the current user (club manager dashboard). */
    @Transactional(readOnly = true)
    public List<ClubResponse> findMine() {
        return SecurityUtils.currentUserId()
                .map(uid -> clubRepository.findByManagerId(uid).stream().map(this::toResponse).toList())
                .orElse(List.of());
    }

    // ── Write ─────────────────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public ClubResponse create(ClubRequest request) {
        if (clubRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new ResourceAlreadyExistsException(
                    "A club with license number '" + request.getLicenseNumber() + "' already exists.");
        }
        Club club = clubMapper.toEntity(request);
        club.setSlug(generateUniqueSlug(request.getName()));
        if (club.getShortName() != null) club.setShortName(club.getShortName().toUpperCase(Locale.ROOT));
        if (club.getCountry() == null) club.setCountry("TN");
        if (request.getStatus() == null) club.setStatus(ClubStatus.ACTIVE);
        assignManager(club, request.getManagerId());
        Club saved = clubRepository.save(club);
        log.info("Club created: {} ({})", saved.getName(), saved.getId());
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF','ROLE_CLUB_MANAGER')")
    public ClubResponse update(UUID id, ClubRequest request) {
        Club club = getOrThrow(id);
        access.requireStaffOrClubManagerOf(id, "edit club details");
        if (!access.isStaff()) {
            // Managers may edit their club's profile, not its federation-controlled fields
            if (request.getStatus() != null && request.getStatus() != club.getStatus()) {
                throw new ForbiddenException("Only federation staff can change a club's status.");
            }
            if (!Objects.equals(request.getLicenseNumber(), club.getLicenseNumber())) {
                throw new ForbiddenException("Only federation staff can change a club's license number.");
            }
            UUID currentManager = club.getManager() != null ? club.getManager().getId() : null;
            if (request.getManagerId() != null && !request.getManagerId().equals(currentManager)) {
                throw new ForbiddenException("Only federation staff can reassign a club's manager.");
            }
        }
        if (!club.getLicenseNumber().equals(request.getLicenseNumber())
                && clubRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new ResourceAlreadyExistsException(
                    "A club with license number '" + request.getLicenseNumber() + "' already exists.");
        }
        clubMapper.updateEntity(request, club);
        if (club.getShortName() != null) club.setShortName(club.getShortName().toUpperCase(Locale.ROOT));
        if (access.isStaff()) assignManager(club, request.getManagerId());
        Club saved = clubRepository.save(club);
        log.info("Club updated: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void delete(UUID id) {
        if (!clubRepository.existsById(id)) {
            throw new ResourceNotFoundException("Club", "id", id);
        }
        clubRepository.deleteById(id);
        clubRepository.flush();
        log.info("Club deleted: {}", id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Club getOrThrow(UUID id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Club", "id", id));
    }

    private void assignManager(Club club, UUID managerId) {
        if (managerId == null) return;
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", managerId));
        if (manager.getRole() != UserRole.ROLE_CLUB_MANAGER) {
            throw new BusinessRuleViolationException("The club manager must have the CLUB_MANAGER role.");
        }
        club.setManager(manager);
    }

    private ClubResponse toResponse(Club club) {
        ClubResponse r = clubMapper.toResponse(club);
        r.setActiveAthletes(athleteRepository.countByClubIdAndStatus(club.getId(), AthleteStatus.ACTIVE));
        return r;
    }

    private String generateUniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        String slug = base;
        int counter = 1;
        while (clubRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }
}
