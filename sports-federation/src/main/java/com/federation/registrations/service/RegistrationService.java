package com.federation.registrations.service;

import com.federation.athletes.entity.Athlete;
import com.federation.athletes.entity.AthleteStatus;
import com.federation.athletes.repository.AthleteRepository;
import com.federation.clubs.entity.Club;
import com.federation.common.exception.BadRequestException;
import com.federation.common.exception.BusinessRuleViolationException;
import com.federation.common.exception.ForbiddenException;
import com.federation.common.exception.ResourceAlreadyExistsException;
import com.federation.common.exception.ResourceNotFoundException;
import com.federation.common.response.PagedResponse;
import com.federation.common.security.AccessControl;
import com.federation.common.util.SecurityUtils;
import com.federation.common.util.Specs;
import com.federation.competitions.entity.Competition;
import com.federation.competitions.entity.CompetitionEvent;
import com.federation.competitions.entity.CompetitionStatus;
import com.federation.competitions.repository.CompetitionEventRepository;
import com.federation.competitions.repository.CompetitionRepository;
import com.federation.registrations.dto.RegistrationRequest;
import com.federation.registrations.dto.RegistrationResponse;
import com.federation.registrations.dto.RegistrationStatusRequest;
import com.federation.registrations.dto.RegistrationUpdateRequest;
import com.federation.registrations.entity.CompetitionRegistration;
import com.federation.registrations.entity.RegistrationStatus;
import com.federation.registrations.mapper.RegistrationMapper;
import com.federation.registrations.repository.CompetitionRegistrationRepository;
import com.federation.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Competition registration workflow.
 *
 * <ul>
 *   <li>Athletes register themselves; club managers register athletes of their club;
 *       admin/staff can register anyone.</li>
 *   <li>PENDING and CONFIRMED entries occupy a start slot. When the event or the
 *       competition is full, new entries are WAITLISTED with a queue position.</li>
 *   <li>When a slot is released (cancel / disqualify / move to waitlist) the head of
 *       the event's waiting list is promoted to PENDING automatically.</li>
 *   <li>{@code max_per_club} is a hard quota: entries beyond it are rejected.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    /** Allowed status transitions; anything else is rejected with 422. */
    private static final Map<RegistrationStatus, Set<RegistrationStatus>> TRANSITIONS = Map.of(
            RegistrationStatus.PENDING,      EnumSet.of(RegistrationStatus.CONFIRMED, RegistrationStatus.WAITLISTED,
                                                        RegistrationStatus.CANCELLED, RegistrationStatus.DISQUALIFIED),
            RegistrationStatus.WAITLISTED,   EnumSet.of(RegistrationStatus.PENDING, RegistrationStatus.CONFIRMED,
                                                        RegistrationStatus.CANCELLED),
            RegistrationStatus.CONFIRMED,    EnumSet.of(RegistrationStatus.PENDING, RegistrationStatus.CANCELLED,
                                                        RegistrationStatus.DISQUALIFIED),
            RegistrationStatus.CANCELLED,    EnumSet.noneOf(RegistrationStatus.class),
            RegistrationStatus.DISQUALIFIED, EnumSet.noneOf(RegistrationStatus.class));

    private static final Set<CompetitionStatus> STAFF_REGISTRATION_WINDOW = EnumSet.of(
            CompetitionStatus.PUBLISHED, CompetitionStatus.REGISTRATION_OPEN, CompetitionStatus.REGISTRATION_CLOSED);

    private final CompetitionRegistrationRepository registrationRepository;
    private final CompetitionRepository             competitionRepository;
    private final CompetitionEventRepository        eventRepository;
    private final AthleteRepository                 athleteRepository;
    private final UserRepository                    userRepository;
    private final RegistrationMapper                registrationMapper;
    private final AccessControl                     access;

    // ── Read ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF','ROLE_CLUB_MANAGER')")
    public PagedResponse<RegistrationResponse> findByCompetition(UUID competitionId, UUID eventId, UUID clubId,
                                                                 String status, Pageable pageable) {
        if (!competitionRepository.existsById(competitionId)) {
            throw new ResourceNotFoundException("Competition", "id", competitionId);
        }
        // Club managers only ever see their own club's entries
        if (!access.isStaff()) {
            if (clubId == null || !access.managesClub(clubId)) {
                throw new ForbiddenException("Club managers must filter by a club they manage (clubId).");
            }
        }
        Specification<CompetitionRegistration> spec = Specification
                .where(Specs.<CompetitionRegistration>eq("competition.id", competitionId))
                .and(Specs.eq("event.id", eventId))
                .and(Specs.eq("club.id", clubId))
                .and(Specs.eq("status", Specs.parseEnum(RegistrationStatus.class, status)));
        return PagedResponse.of(registrationRepository.findAll(spec, pageable).map(registrationMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PagedResponse<RegistrationResponse> findByAthlete(UUID athleteId, String status, Pageable pageable) {
        Athlete athlete = athleteRepository.findById(athleteId)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete", "id", athleteId));
        access.requireAthleteAccess(athlete);
        Specification<CompetitionRegistration> spec = Specification
                .where(Specs.<CompetitionRegistration>eq("athlete.id", athleteId))
                .and(Specs.eq("status", Specs.parseEnum(RegistrationStatus.class, status)));
        return PagedResponse.of(registrationRepository.findAll(spec, pageable).map(registrationMapper::toResponse));
    }

    /** Registrations of the athlete linked to the current account. */
    @Transactional(readOnly = true)
    public PagedResponse<RegistrationResponse> findMine(String status, Pageable pageable) {
        Athlete athlete = access.currentAthlete()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No athlete profile is linked to your account. Contact the federation."));
        return findByAthlete(athlete.getId(), status, pageable);
    }

    @Transactional(readOnly = true)
    public RegistrationResponse findById(UUID id) {
        CompetitionRegistration reg = getOrThrow(id);
        requireReadAccess(reg);
        return registrationMapper.toResponse(reg);
    }

    // ── Register ──────────────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF','ROLE_CLUB_MANAGER','ROLE_ATHLETE')")
    public RegistrationResponse register(UUID competitionId, RegistrationRequest request) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Competition", "id", competitionId));
        CompetitionEvent event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("CompetitionEvent", "id", request.getEventId()));
        if (!event.getCompetition().getId().equals(competitionId)) {
            throw new BadRequestException("Event does not belong to this competition.");
        }

        Athlete athlete = resolveAthleteToRegister(request.getAthleteId());
        checkRegistrationWindow(competition);
        checkEligibility(event, athlete);

        if (registrationRepository.existsByAthleteIdAndEventId(athlete.getId(), event.getId())) {
            throw new ResourceAlreadyExistsException(
                    athlete.getFullName() + " is already registered for " + event.getName() + ".");
        }

        Club club = athlete.getClub();
        checkClubQuota(competition, club);

        CompetitionRegistration reg = CompetitionRegistration.builder()
                .competition(competition)
                .event(event)
                .athlete(athlete)
                .club(club)
                .registeredBy(SecurityUtils.currentUserId().flatMap(userRepository::findById).orElse(null))
                .registrationNumber(nextRegistrationNumber())
                .seedValue(request.getSeedValue())
                .seedUnit(request.getSeedUnit() != null ? request.getSeedUnit() : event.getScoringUnit())
                .medicalWaiver(request.isMedicalWaiver())
                .notes(request.getNotes())
                .feeAmount(competition.getEntryFee())
                .feeCurrency(competition.getCurrency() != null ? competition.getCurrency() : "TND")
                .build();

        if (hasCapacity(event, competition)) {
            reg.setStatus(RegistrationStatus.PENDING);
        } else {
            reg.setStatus(RegistrationStatus.WAITLISTED);
            reg.setWaitlistPosition(registrationRepository.maxWaitlistPosition(event.getId()) + 1);
        }

        CompetitionRegistration saved = registrationRepository.save(reg);
        log.info("Registration {} created: athlete={} event={} status={}",
                saved.getRegistrationNumber(), athlete.getId(), event.getId(), saved.getStatus());
        return registrationMapper.toResponse(saved);
    }

    // ── Status workflow ───────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public RegistrationResponse changeStatus(UUID id, RegistrationStatusRequest request) {
        CompetitionRegistration reg = getOrThrow(id);
        RegistrationStatus from   = reg.getStatus();
        RegistrationStatus target = request.getStatus();

        // Owners (athlete / their club manager) may only withdraw; everything else is staff-only
        if (!access.isStaff()) {
            if (target != RegistrationStatus.CANCELLED || !access.canAccessAthlete(reg.getAthlete())) {
                throw new ForbiddenException("Only federation staff can change registration status.");
            }
        }
        if (from == target) {
            return registrationMapper.toResponse(reg);
        }
        if (!TRANSITIONS.get(from).contains(target)) {
            throw new BusinessRuleViolationException(
                    "Cannot change registration status from " + from + " to " + target + ".");
        }
        if (target == RegistrationStatus.DISQUALIFIED && isBlank(request.getReason())) {
            throw new BadRequestException("A reason is required when disqualifying a registration.");
        }
        // Moving into an active slot must respect capacity
        if (!from.isActive() && target.isActive() && !hasCapacity(reg.getEvent(), reg.getCompetition())) {
            throw new BusinessRuleViolationException("The event is full — no start slot available.");
        }

        reg.setStatus(target);
        switch (target) {
            case CONFIRMED -> {
                reg.setWaitlistPosition(null);
                reg.setConfirmedAt(Instant.now());
            }
            case PENDING -> {
                reg.setWaitlistPosition(null);
                reg.setConfirmedAt(null);
            }
            case WAITLISTED -> {
                reg.setConfirmedAt(null);
                reg.setWaitlistPosition(registrationRepository.maxWaitlistPosition(reg.getEvent().getId()) + 1);
            }
            case CANCELLED, DISQUALIFIED -> {
                reg.setWaitlistPosition(null);
                reg.setCancelledAt(Instant.now());
                reg.setCancellationReason(request.getReason());
            }
        }
        registrationRepository.saveAndFlush(reg);

        if (from == RegistrationStatus.WAITLISTED || from.isActive()) {
            compactWaitlist(reg.getEvent().getId());
        }
        if (from.isActive() && !target.isActive()) {
            promoteFromWaitlist(reg.getEvent(), reg.getCompetition());
        }
        log.info("Registration {} status {} -> {}", reg.getRegistrationNumber(), from, target);
        return registrationMapper.toResponse(reg);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public RegistrationResponse update(UUID id, RegistrationUpdateRequest request) {
        CompetitionRegistration reg = getOrThrow(id);
        if (request.getBibNumber()     != null) reg.setBibNumber(request.getBibNumber());
        if (request.getSeedValue()     != null) reg.setSeedValue(request.getSeedValue());
        if (request.getSeedUnit()      != null) reg.setSeedUnit(request.getSeedUnit());
        if (request.getFeePaid()       != null) reg.setFeePaid(request.getFeePaid());
        if (request.getMedicalWaiver() != null) reg.setMedicalWaiver(request.getMedicalWaiver());
        if (request.getNotes()         != null) reg.setNotes(request.getNotes());
        return registrationMapper.toResponse(registrationRepository.save(reg));
    }

    /**
     * Assigns sequential bib numbers to every CONFIRMED registration of a competition
     * that does not have one yet (ordered by registration time).
     *
     * @return number of bibs assigned
     */
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public int assignBibNumbers(UUID competitionId) {
        if (!competitionRepository.existsById(competitionId)) {
            throw new ResourceNotFoundException("Competition", "id", competitionId);
        }
        int next = registrationRepository.maxBibNumber(competitionId) + 1;
        int assigned = 0;
        for (CompetitionRegistration reg : registrationRepository
                .findByCompetitionIdAndStatusOrderByCreatedAtAsc(competitionId, RegistrationStatus.CONFIRMED)) {
            if (reg.getBibNumber() == null) {
                reg.setBibNumber(next++);
                assigned++;
            }
        }
        log.info("Assigned {} bib number(s) for competition {}", assigned, competitionId);
        return assigned;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private CompetitionRegistration getOrThrow(UUID id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registration", "id", id));
    }

    private void requireReadAccess(CompetitionRegistration reg) {
        if (access.isStaff()) return;
        if (reg.getClub() != null && access.managesClub(reg.getClub().getId())) return;
        access.requireAthleteAccess(reg.getAthlete());
    }

    /**
     * Athletes always register themselves; club managers may register athletes of
     * their clubs; staff may register anyone.
     */
    private Athlete resolveAthleteToRegister(UUID requestedAthleteId) {
        if (access.isStaff() || access.isClubManager()) {
            if (requestedAthleteId == null) {
                throw new BadRequestException("athleteId is required.");
            }
            Athlete athlete = athleteRepository.findById(requestedAthleteId)
                    .orElseThrow(() -> new ResourceNotFoundException("Athlete", "id", requestedAthleteId));
            if (!access.isStaff()) {
                UUID clubId = athlete.getClub() != null ? athlete.getClub().getId() : null;
                access.requireStaffOrClubManagerOf(clubId, "register athletes");
            }
            return athlete;
        }
        Athlete own = access.currentAthlete()
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "No athlete profile is linked to your account. Contact the federation."));
        if (requestedAthleteId != null && !requestedAthleteId.equals(own.getId())) {
            throw new ForbiddenException("Athletes can only register themselves.");
        }
        return own;
    }

    private void checkRegistrationWindow(Competition competition) {
        if (access.isStaff()) {
            if (!STAFF_REGISTRATION_WINDOW.contains(competition.getStatus())) {
                throw new BusinessRuleViolationException(
                        "Registrations are not possible for a competition in status " + competition.getStatus() + ".");
            }
            return;
        }
        if (competition.getStatus() != CompetitionStatus.REGISTRATION_OPEN) {
            throw new BusinessRuleViolationException("Registration is not open for this competition.");
        }
        LocalDate deadline = competition.getRegistrationDeadline();
        if (deadline != null && LocalDate.now().isAfter(deadline)) {
            throw new BusinessRuleViolationException("The registration deadline (" + deadline + ") has passed.");
        }
    }

    private void checkEligibility(CompetitionEvent event, Athlete athlete) {
        if (event.getStatus() == CompetitionStatus.CANCELLED) {
            throw new BusinessRuleViolationException("This event has been cancelled.");
        }
        if (athlete.getStatus() != AthleteStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "Only ACTIVE athletes can register (current status: " + athlete.getStatus() + ").");
        }
        if (event.getGenderCategory() != null && event.getGenderCategory() != athlete.getGender()) {
            throw new BusinessRuleViolationException(
                    "This event is restricted to " + event.getGenderCategory() + " athletes.");
        }
        if (event.getAgeCategory() != null && event.getAgeCategory() != athlete.getCategory()) {
            throw new BusinessRuleViolationException(
                    "This event is restricted to the " + event.getAgeCategory() + " category.");
        }
    }

    private void checkClubQuota(Competition competition, Club club) {
        Integer quota = competition.getMaxPerClub();
        if (quota == null || club == null) return;
        long used = registrationRepository.countByCompetitionIdAndClubIdAndStatusIn(
                competition.getId(), club.getId(), RegistrationStatus.ACTIVE);
        if (used >= quota) {
            throw new BusinessRuleViolationException(
                    club.getName() + " has reached its quota of " + quota + " entries for this competition.");
        }
    }

    /** True when both the event and the competition still have a free start slot. */
    private boolean hasCapacity(CompetitionEvent event, Competition competition) {
        if (event.getMaxParticipants() != null
                && registrationRepository.countByEventIdAndStatusIn(event.getId(), RegistrationStatus.ACTIVE)
                   >= event.getMaxParticipants()) {
            return false;
        }
        return competition.getMaxParticipants() == null
                || registrationRepository.countByCompetitionIdAndStatusIn(competition.getId(), RegistrationStatus.ACTIVE)
                   < competition.getMaxParticipants();
    }

    /** Moves the head of the waiting list into a free slot (as PENDING), if any. */
    private void promoteFromWaitlist(CompetitionEvent event, Competition competition) {
        List<CompetitionRegistration> queue = registrationRepository
                .findByEventIdAndStatusOrderByWaitlistPositionAsc(event.getId(), RegistrationStatus.WAITLISTED);
        if (queue.isEmpty() || !hasCapacity(event, competition)) return;

        CompetitionRegistration head = queue.get(0);
        head.setStatus(RegistrationStatus.PENDING);
        head.setWaitlistPosition(null);
        registrationRepository.saveAndFlush(head);
        log.info("Registration {} promoted from waiting list", head.getRegistrationNumber());
        compactWaitlist(event.getId());
    }

    /** Renumbers the waiting list 1..n, preserving order. */
    private void compactWaitlist(UUID eventId) {
        int position = 1;
        for (CompetitionRegistration r : registrationRepository
                .findByEventIdAndStatusOrderByWaitlistPositionAsc(eventId, RegistrationStatus.WAITLISTED)) {
            r.setWaitlistPosition(position++);
        }
    }

    private String nextRegistrationNumber() {
        long seq = registrationRepository.nextRegistrationSequence();
        return String.format("REG-%d-%05d", Year.now().getValue(), seq);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
