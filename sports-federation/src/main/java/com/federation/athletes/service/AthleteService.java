package com.federation.athletes.service;

import com.federation.athletes.dto.AthleteRequest;
import com.federation.athletes.dto.AthleteResponse;
import com.federation.athletes.entity.Athlete;
import com.federation.athletes.entity.AthleteCategory;
import com.federation.athletes.entity.AthleteStatus;
import com.federation.athletes.mapper.AthleteMapper;
import com.federation.athletes.repository.AthleteRepository;
import com.federation.clubs.entity.Club;
import com.federation.clubs.repository.ClubRepository;
import com.federation.common.exception.BusinessRuleViolationException;
import com.federation.common.exception.ForbiddenException;
import com.federation.common.exception.ResourceAlreadyExistsException;
import com.federation.common.exception.ResourceNotFoundException;
import com.federation.common.response.PagedResponse;
import com.federation.common.security.AccessControl;
import com.federation.common.util.Gender;
import com.federation.common.util.Specs;
import com.federation.users.entity.User;
import com.federation.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AthleteService {

    private final AthleteRepository athleteRepository;
    private final ClubRepository    clubRepository;
    private final UserRepository    userRepository;
    private final AthleteMapper     athleteMapper;
    private final AccessControl     access;

    // ── Read ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<AthleteResponse> findAll(String search, String status,
                                                  String gender, String category,
                                                  UUID clubId, Pageable pageable) {
        Specification<Athlete> spec = Specification
                .where(Specs.<Athlete>search(search, "firstName", "lastName", "licenseNumber"))
                .and(Specs.eq("status",   Specs.parseEnum(AthleteStatus.class, status)))
                .and(Specs.eq("gender",   Specs.parseEnum(Gender.class, gender)))
                .and(Specs.eq("category", Specs.parseEnum(AthleteCategory.class, category)))
                .and(Specs.eq("club.id",  clubId));

        return PagedResponse.of(athleteRepository.findAll(spec, pageable).map(this::toVisibleResponse));
    }

    @Transactional(readOnly = true)
    public AthleteResponse findById(UUID id) {
        return toVisibleResponse(getOrThrow(id));
    }

    /** The athlete profile linked to the current account (athlete portal). */
    @Transactional(readOnly = true)
    public AthleteResponse findMine() {
        return athleteMapper.toResponse(access.currentAthlete()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No athlete profile is linked to your account. Contact the federation.")));
    }

    // ── Write ─────────────────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF','ROLE_CLUB_MANAGER')")
    public AthleteResponse create(AthleteRequest request) {
        if (athleteRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new ResourceAlreadyExistsException(
                    "License number '" + request.getLicenseNumber() + "' is already registered.");
        }
        access.requireStaffOrClubManagerOf(request.getClubId(), "create athletes");

        Athlete athlete = athleteMapper.toEntity(request);
        resolveClub(athlete, request.getClubId());
        resolveUser(athlete, request.getUserId());
        computeCategory(athlete);
        applyDefaults(athlete);
        Athlete saved = athleteRepository.save(athlete);
        log.info("Athlete created: {} {}", saved.getFirstName(), saved.getLastName());
        return athleteMapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF','ROLE_CLUB_MANAGER')")
    public AthleteResponse update(UUID id, AthleteRequest request) {
        Athlete athlete = getOrThrow(id);
        UUID currentClubId = athlete.getClub() != null ? athlete.getClub().getId() : null;
        access.requireStaffOrClubManagerOf(currentClubId, "edit athletes");
        if (!access.isStaff() && request.getClubId() != null && !request.getClubId().equals(currentClubId)) {
            throw new ForbiddenException("Club transfers must be processed by federation staff.");
        }
        if (!athlete.getLicenseNumber().equals(request.getLicenseNumber())
                && athleteRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new ResourceAlreadyExistsException(
                    "License number '" + request.getLicenseNumber() + "' is already registered.");
        }

        athleteMapper.updateEntity(request, athlete);
        resolveClub(athlete, request.getClubId());
        resolveUser(athlete, request.getUserId());
        computeCategory(athlete);
        applyDefaults(athlete);
        Athlete saved = athleteRepository.save(athlete);
        log.info("Athlete updated: {}", id);
        return athleteMapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public void delete(UUID id) {
        if (!athleteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Athlete", "id", id);
        }
        athleteRepository.deleteById(id);
        athleteRepository.flush();
        log.info("Athlete deleted: {}", id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Athlete getOrThrow(UUID id) {
        return athleteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete", "id", id));
    }

    private void resolveClub(Athlete athlete, UUID clubId) {
        if (clubId != null) {
            Club club = clubRepository.findById(clubId)
                    .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));
            athlete.setClub(club);
        }
    }

    /** Links a portal account to the athlete (staff only; one account per athlete). */
    private void resolveUser(Athlete athlete, UUID userId) {
        UUID current = athlete.getUser() != null ? athlete.getUser().getId() : null;
        if (userId == null || userId.equals(current)) return;
        if (!access.isStaff()) {
            throw new ForbiddenException("Only federation staff can link user accounts to athletes.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        athleteRepository.findByUserId(userId)
                .filter(other -> !Objects.equals(other.getId(), athlete.getId()))
                .ifPresent(other -> {
                    throw new BusinessRuleViolationException(
                            "This account is already linked to athlete " + other.getFullName() + ".");
                });
        athlete.setUser(user);
    }

    /** NOT NULL columns with DB defaults — the mapper would otherwise write explicit nulls. */
    private void applyDefaults(Athlete athlete) {
        if (athlete.getNationality() == null || athlete.getNationality().isBlank()) athlete.setNationality("Tunisian");
        if (athlete.getCountryCode() == null || athlete.getCountryCode().isBlank()) athlete.setCountryCode("TUN");
        if (athlete.getStatus() == null) athlete.setStatus(AthleteStatus.ACTIVE);
    }

    private void computeCategory(Athlete athlete) {
        if (athlete.getDateOfBirth() != null) {
            int age = Period.between(athlete.getDateOfBirth(), LocalDate.now()).getYears();
            athlete.setCategory(AthleteCategory.fromAge(age));
        }
    }

    /**
     * Public visitors and unrelated users get the sporting profile only;
     * contact, emergency, medical and internal fields are hidden.
     */
    private AthleteResponse toVisibleResponse(Athlete athlete) {
        AthleteResponse r = athleteMapper.toResponse(athlete);
        if (!access.canAccessAthlete(athlete)) {
            r.setEmail(null);
            r.setPhone(null);
            r.setEmergencyContactName(null);
            r.setEmergencyContactPhone(null);
            r.setMedicalClearanceDate(null);
            r.setMedicalNotes(null);
            r.setNotes(null);
            r.setUserId(null);
            r.setLicenseExpiry(null);
            r.setRestricted(true);
        }
        return r;
    }
}
