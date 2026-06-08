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
import com.federation.common.exception.ResourceAlreadyExistsException;
import com.federation.common.exception.ResourceNotFoundException;
import com.federation.common.response.PagedResponse;
import com.federation.common.util.Gender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;

@Slf4j
@Service
@RequiredArgsConstructor
public class AthleteService {

    private final AthleteRepository athleteRepository;
    private final ClubRepository    clubRepository;
    private final AthleteMapper     athleteMapper;

    // ── Read ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<AthleteResponse> findAll(String search, String status,
                                               String gender, String category,
                                               UUID clubId, Pageable pageable) {
    AthleteStatus as = parseEnum(AthleteStatus.class, status);
    Gender        g  = parseEnum(Gender.class, gender);
    AthleteCategory ac = parseEnum(AthleteCategory.class, category);

    // Load all then filter — safe for current data volumes
    List<Athlete> all = athleteRepository.findAll();
    List<AthleteResponse> filtered = all.stream()
        .filter(a -> search == null || search.isBlank() ||
                a.getFirstName().toLowerCase().contains(search.toLowerCase()) ||
                a.getLastName().toLowerCase().contains(search.toLowerCase()) ||
                a.getLicenseNumber().toLowerCase().contains(search.toLowerCase()))
        .filter(a -> as == null || a.getStatus() == as)
        .filter(a -> g  == null || a.getGender() == g)
        .filter(a -> ac == null || a.getCategory() == ac)
        .filter(a -> clubId == null || (a.getClub() != null && a.getClub().getId().equals(clubId)))
        .map(athleteMapper::toResponse)
        .collect(Collectors.toList());

    // Manual pagination
    int pageNum  = pageable.getPageNumber();
    int pageSize = pageable.getPageSize();
    int from = Math.min(pageNum * pageSize, filtered.size());
    int to   = Math.min(from + pageSize, filtered.size());
    List<AthleteResponse> pageContent = filtered.subList(from, to);

    return PagedResponse.of(new PageImpl<>(pageContent, pageable, filtered.size()));
}

    @Transactional(readOnly = true)
    public AthleteResponse findById(UUID id) {
        return athleteMapper.toResponse(getOrThrow(id));
    }

    // ── Write ─────────────────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF','ROLE_CLUB_MANAGER')")
    public AthleteResponse create(AthleteRequest request) {
        if (athleteRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new ResourceAlreadyExistsException(
                    "License number '" + request.getLicenseNumber() + "' is already registered.");
        }
        Athlete athlete = athleteMapper.toEntity(request);
        resolveClub(athlete, request.getClubId());
        computeCategory(athlete);
        if (athlete.getStatus() == null) athlete.setStatus(AthleteStatus.ACTIVE);
        Athlete saved = athleteRepository.save(athlete);
        log.info("Athlete created: {} {}", saved.getFirstName(), saved.getLastName());
        return athleteMapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF','ROLE_CLUB_MANAGER')")
    public AthleteResponse update(UUID id, AthleteRequest request) {
        Athlete athlete = getOrThrow(id);
        athleteMapper.updateEntity(request, athlete);
        resolveClub(athlete, request.getClubId());
        computeCategory(athlete);
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

    private void computeCategory(Athlete athlete) {
        if (athlete.getDateOfBirth() != null) {
            int age = Period.between(athlete.getDateOfBirth(), LocalDate.now()).getYears();
            athlete.setCategory(AthleteCategory.fromAge(age));
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> clazz, String value) {
        if (value == null || value.isBlank()) return null;
        try { return Enum.valueOf(clazz, value.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
