package com.federation.competitions.service;

import com.federation.clubs.entity.Club;
import com.federation.clubs.repository.ClubRepository;
import com.federation.common.exception.BadRequestException;
import com.federation.common.exception.ResourceNotFoundException;
import com.federation.common.response.PagedResponse;
import com.federation.competitions.dto.CompetitionRequest;
import com.federation.competitions.dto.CompetitionResponse;
import com.federation.competitions.entity.Competition;
import com.federation.competitions.entity.CompetitionStatus;
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
import java.time.LocalDate;
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

    // ── Read ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<CompetitionResponse> findAll(String search, String status,
                                                       String from, String to,
                                                       Pageable pageable) {
        CompetitionStatus cs = parseEnum(CompetitionStatus.class, status);
        LocalDate fromDate   = parseDate(from);
        LocalDate toDate     = parseDate(to);
        String s = blank(search) ? null : search;

        Page<Competition> page = competitionRepository.findAllFiltered(s, cs, fromDate, toDate, pageable);

        Page<CompetitionResponse> mapped = page.map(c -> {
            CompetitionResponse r = competitionMapper.toResponse(c);
            r.setEventCount(eventRepository.countByCompetitionId(c.getId()));
            return r;
        });

        return PagedResponse.of(mapped);
    }

    @Transactional(readOnly = true)
    public CompetitionResponse findById(UUID id) {
        Competition c = getOrThrow(id);
        CompetitionResponse r = competitionMapper.toResponse(c);
        r.setEventCount(eventRepository.countByCompetitionId(id));
        return r;
    }

    @Transactional(readOnly = true)
    public CompetitionResponse findBySlug(String slug) {
        Competition c = competitionRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Competition", "slug", slug));
        CompetitionResponse r = competitionMapper.toResponse(c);
        r.setEventCount(eventRepository.countByCompetitionId(c.getId()));
        return r;
    }

    // ── Write ─────────────────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public CompetitionResponse create(CompetitionRequest request) {
        validateDates(request);
        Competition competition = competitionMapper.toEntity(request);
        competition.setSlug(generateUniqueSlug(request.getName()));
        if (competition.getStatus() == null) competition.setStatus(CompetitionStatus.DRAFT);
        resolveRelations(competition, request);
        Competition saved = competitionRepository.save(competition);
        log.info("Competition created: {} ({})", saved.getName(), saved.getId());
        return competitionMapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public CompetitionResponse update(UUID id, CompetitionRequest request) {
        validateDates(request);
        Competition competition = getOrThrow(id);
        competitionMapper.updateEntity(request, competition);
        resolveRelations(competition, request);
        Competition saved = competitionRepository.save(competition);
        log.info("Competition updated: {}", id);
        return competitionMapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void delete(UUID id) {
        if (!competitionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Competition", "id", id);
        }
        competitionRepository.deleteById(id);
        log.info("Competition deleted: {}", id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

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

    private <E extends Enum<E>> E parseEnum(Class<E> clazz, String value) {
        if (blank(value)) return null;
        try { return Enum.valueOf(clazz, value.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private LocalDate parseDate(String value) {
        if (blank(value)) return null;
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException e) { return null; }
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }
}
