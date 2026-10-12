package com.federation.competitions.service;

import com.federation.common.util.Specs;
import org.springframework.data.jpa.domain.Specification;
import com.federation.clubs.entity.Club;
import com.federation.clubs.repository.ClubRepository;
import com.federation.common.exception.BadRequestException;
import com.federation.common.exception.ResourceNotFoundException;
import com.federation.common.response.PagedResponse;
import com.federation.competitions.dto.CompetitionRequest;
import com.federation.competitions.dto.CompetitionResponse;
import com.federation.competitions.entity.Competition;
import com.federation.common.exception.BusinessRuleViolationException;
import com.federation.competitions.entity.CompetitionFormat;
import com.federation.competitions.entity.CompetitionLevel;
import com.federation.competitions.entity.CompetitionStatus;
import com.federation.registrations.entity.RegistrationStatus;
import com.federation.registrations.repository.CompetitionRegistrationRepository;
import com.federation.results.repository.ResultRepository;
import com.federation.competitions.mapper.CompetitionMapper;
import com.federation.competitions.repository.CompetitionEventRepository;
import com.federation.competitions.repository.CompetitionRepository;
import com.federation.users.entity.User;
import com.federation.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionService {

    private final CompetitionRepository      competitionRepository;
    private final CompetitionEventRepository eventRepository;
    private final ClubRepository             clubRepository;
    private final UserRepository             userRepository;
    private final CompetitionMapper          competitionMapper;
    private final CompetitionRegistrationRepository registrationRepository;
    private final ResultRepository           resultRepository;

    // ── Read ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<CompetitionResponse> findAll(String search, String status,
                                                       String from, String to,
                                                       Pageable pageable) {
        Specification<Competition> spec = Specification
                .where(Specs.<Competition>search(search, "name", "sport", "venueCity"))
                .and(Specs.eq("status", Specs.parseEnum(CompetitionStatus.class, status)))
                .and(Specs.onOrAfter("startDate", parseDate(from)))
                .and(Specs.onOrBefore("endDate", parseDate(to)));

        Page<Competition> page = competitionRepository.findAll(spec, pageable);

        return PagedResponse.of(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public CompetitionResponse findById(UUID id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public CompetitionResponse findBySlug(String slug) {
        Competition c = competitionRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Competition", "slug", slug));
        return toResponse(c);
    }

    // ── Write ─────────────────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public CompetitionResponse create(CompetitionRequest request) {
        validateDates(request);
        Competition competition = competitionMapper.toEntity(request);
        competition.setSlug(generateUniqueSlug(request.getName()));
        applyDefaults(competition);
        resolveRelations(competition, request);
        applyLifecycle(competition, null);
        Competition saved = competitionRepository.save(competition);
        log.info("Competition created: {} ({})", saved.getName(), saved.getId());
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public CompetitionResponse update(UUID id, CompetitionRequest request) {
        validateDates(request);
        Competition competition = getOrThrow(id);
        CompetitionStatus previous = competition.getStatus();
        competitionMapper.updateEntity(request, competition);
        applyDefaults(competition);
        resolveRelations(competition, request);
        applyLifecycle(competition, previous);
        Competition saved = competitionRepository.save(competition);
        log.info("Competition updated: {}", id);
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void delete(UUID id) {
        if (!competitionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Competition", "id", id);
        }
        if (registrationRepository.existsByCompetitionId(id) || resultRepository.existsByCompetitionId(id)) {
            throw new BusinessRuleViolationException(
                    "This competition has registrations or results. Cancel it instead of deleting it.");
        }
        competitionRepository.deleteById(id);
        competitionRepository.flush();
        log.info("Competition deleted: {}", id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private CompetitionResponse toResponse(Competition c) {
        CompetitionResponse r = competitionMapper.toResponse(c);
        r.setEventCount(eventRepository.countByCompetitionId(c.getId()));
        r.setConfirmedRegistrations(registrationRepository.countByCompetitionIdAndStatusIn(
                c.getId(), EnumSet.of(RegistrationStatus.CONFIRMED)));
        return r;
    }

    /** Columns that are NOT NULL in the schema must never be nulled by a partial request. */
    private void applyDefaults(Competition c) {
        if (c.getStatus() == null)       c.setStatus(CompetitionStatus.DRAFT);
        if (c.getLevel() == null)        c.setLevel(CompetitionLevel.NATIONAL);
        if (c.getFormat() == null)       c.setFormat(CompetitionFormat.INDIVIDUAL);
        if (c.getVenueCountry() == null) c.setVenueCountry("TN");
        if (c.getCurrency() == null)     c.setCurrency("TND");
    }

    /** Stamps published / cancelled timestamps when the status changes. */
    private void applyLifecycle(Competition c, CompetitionStatus previous) {
        if (c.getStatus() == previous) return;
        if (c.getStatus() != CompetitionStatus.DRAFT && c.getPublishedAt() == null) {
            c.setPublishedAt(Instant.now());
        }
        if (c.getStatus() == CompetitionStatus.CANCELLED) {
            c.setCancelledAt(Instant.now());
        }
    }

    private Competition getOrThrow(UUID id) {
        return competitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Competition", "id", id));
    }

    private void resolveRelations(Competition competition, CompetitionRequest request) {
        if (request.getHostClubId() != null) {
            Club club = clubRepository.findById(request.getHostClubId())
                    .orElseThrow(() -> new ResourceNotFoundException("Club", "id", request.getHostClubId()));
            competition.setHostClub(club);
        }
        if (request.getOrganizerId() != null) {
            User user = userRepository.findById(request.getOrganizerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getOrganizerId()));
            competition.setOrganizer(user);
        }
    }

    private void validateDates(CompetitionRequest request) {
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be on or after start date.");
        }
        if (request.getRegistrationDeadline() != null && request.getStartDate() != null
                && request.getRegistrationDeadline().isAfter(request.getStartDate())) {
            throw new BadRequestException("Registration deadline must be before start date.");
        }
    }

    private String generateUniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        String slug = base;
        int counter = 1;
        while (competitionRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }


    private LocalDate parseDate(String value) {
        if (blank(value)) return null;
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException e) { return null; }
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }
}
