package com.federation.competitions.service;

import com.federation.common.exception.BusinessRuleViolationException;
import com.federation.common.exception.ResourceAlreadyExistsException;
import com.federation.common.exception.ResourceNotFoundException;
import com.federation.competitions.dto.CompetitionEventRequest;
import com.federation.competitions.dto.CompetitionEventResponse;
import com.federation.competitions.entity.Competition;
import com.federation.competitions.entity.CompetitionEvent;
import com.federation.competitions.entity.CompetitionStatus;
import com.federation.competitions.mapper.CompetitionEventMapper;
import com.federation.competitions.repository.CompetitionEventRepository;
import com.federation.competitions.repository.CompetitionRepository;
import com.federation.registrations.entity.RegistrationStatus;
import com.federation.registrations.repository.CompetitionRegistrationRepository;
import com.federation.results.repository.ResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** CRUD for the discipline events inside a competition (e.g. "100m Men Senior"). */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionEventService {

    private final CompetitionRepository             competitionRepository;
    private final CompetitionEventRepository        eventRepository;
    private final CompetitionRegistrationRepository registrationRepository;
    private final ResultRepository                  resultRepository;
    private final CompetitionEventMapper            eventMapper;
    private final CompetitionService                competitionService;

    @Transactional(readOnly = true)
    public List<CompetitionEventResponse> findByCompetition(UUID competitionId) {
        requireCompetition(competitionId);
        return eventRepository.findByCompetitionIdOrderByScheduledAtAsc(competitionId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompetitionEventResponse findById(UUID competitionId, UUID eventId) {
        return toResponse(getOrThrow(competitionId, eventId));
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public CompetitionEventResponse create(UUID competitionId, CompetitionEventRequest request) {
        Competition competition = requireCompetition(competitionId);
        ensureCompetitionEditable(competition);
        ensureUniqueCode(competitionId, request.getCode(), null);

        CompetitionEvent event = eventMapper.toEntity(request);
        event.setCompetition(competition);
        event.setLowerIsBetter(request.getLowerIsBetter() == null || request.getLowerIsBetter());
        if (event.getStatus() == null) event.setStatus(CompetitionStatus.DRAFT);

        CompetitionEvent saved = eventRepository.save(event);
        log.info("Event '{}' created in competition {}", saved.getName(), competitionId);
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public CompetitionEventResponse update(UUID competitionId, UUID eventId, CompetitionEventRequest request) {
        CompetitionEvent event = getOrThrow(competitionId, eventId);
        ensureUniqueCode(competitionId, request.getCode(), eventId);
        eventMapper.updateEntity(request, event);
        if (request.getLowerIsBetter() != null) event.setLowerIsBetter(request.getLowerIsBetter());
        log.info("Event {} updated", eventId);
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public void delete(UUID competitionId, UUID eventId) {
        CompetitionEvent event = getOrThrow(competitionId, eventId);
        if (registrationRepository.existsByEventId(eventId) || resultRepository.existsByEventId(eventId)) {
            throw new BusinessRuleViolationException(
                    "This event already has registrations or results. Cancel it instead of deleting it.");
        }
        eventRepository.delete(event);
        log.info("Event {} deleted", eventId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Competition requireCompetition(UUID competitionId) {
        return competitionService.visibleOrThrow(competitionRepository.findById(competitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Competition", "id", competitionId)));
    }

    private CompetitionEvent getOrThrow(UUID competitionId, UUID eventId) {
        CompetitionEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("CompetitionEvent", "id", eventId));
        competitionService.visibleOrThrow(event.getCompetition());
        if (!event.getCompetition().getId().equals(competitionId)) {
            throw new ResourceNotFoundException("CompetitionEvent", "id", eventId);
        }
        return event;
    }

    private void ensureCompetitionEditable(Competition competition) {
        if (EnumSet.of(CompetitionStatus.COMPLETED, CompetitionStatus.CANCELLED).contains(competition.getStatus())) {
            throw new BusinessRuleViolationException(
                    "Events cannot be added to a " + competition.getStatus() + " competition.");
        }
    }

    private void ensureUniqueCode(UUID competitionId, String code, UUID currentEventId) {
        if (code == null || code.isBlank()) return;
        boolean clash = eventRepository.findByCompetitionIdOrderByScheduledAtAsc(competitionId).stream()
                .anyMatch(e -> code.equalsIgnoreCase(e.getCode()) && !Objects.equals(e.getId(), currentEventId));
        if (clash) {
            throw new ResourceAlreadyExistsException("An event with code '" + code + "' already exists in this competition.");
        }
    }

    private CompetitionEventResponse toResponse(CompetitionEvent event) {
        CompetitionEventResponse r = eventMapper.toResponse(event);
        r.setRegistrationCount(registrationRepository.countByEventIdAndStatusIn(event.getId(), RegistrationStatus.ACTIVE));
        r.setWaitlistCount(registrationRepository.countByEventIdAndStatusIn(
                event.getId(), EnumSet.of(RegistrationStatus.WAITLISTED)));
        return r;
    }
}
