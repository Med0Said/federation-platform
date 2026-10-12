package com.federation.results.service;

import com.federation.athletes.entity.Athlete;
import com.federation.athletes.repository.AthleteRepository;
import com.federation.common.exception.BadRequestException;
import com.federation.common.exception.ResourceAlreadyExistsException;
import com.federation.common.exception.ResourceNotFoundException;
import com.federation.common.response.PagedResponse;
import com.federation.common.util.SecurityUtils;
import com.federation.common.util.Specs;
import com.federation.competitions.entity.Competition;
import com.federation.competitions.entity.CompetitionEvent;
import com.federation.competitions.repository.CompetitionEventRepository;
import com.federation.competitions.repository.CompetitionRepository;
import com.federation.results.dto.ResultRequest;
import com.federation.results.dto.ResultResponse;
import com.federation.results.entity.Ranking;
import com.federation.results.entity.Result;
import com.federation.results.entity.ResultStatus;
import com.federation.results.mapper.ResultMapper;
import com.federation.results.repository.RankingRepository;
import com.federation.results.repository.ResultRepository;
import com.federation.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Results recording. Every write recalculates the rankings of the affected
 * event round(s) through {@link RankingService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResultService {

    private final ResultRepository           resultRepository;
    private final RankingRepository          rankingRepository;
    private final CompetitionRepository      competitionRepository;
    private final CompetitionEventRepository eventRepository;
    private final AthleteRepository          athleteRepository;
    private final UserRepository             userRepository;
    private final RankingService             rankingService;
    private final ResultMapper               resultMapper;

    // ── Read ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<ResultResponse> findAll(UUID competitionId, UUID eventId,
                                                  UUID athleteId, String status,
                                                  Pageable pageable) {
        Specification<Result> spec = Specification
                .where(Specs.<Result>eq("competition.id", competitionId))
                .and(Specs.eq("event.id",   eventId))
                .and(Specs.eq("athlete.id", athleteId))
                .and(Specs.eq("status",     Specs.parseEnum(ResultStatus.class, status)));
        Page<Result> page = resultRepository.findAll(spec, pageable);

        Page<ResultResponse> mapped = page.map(result -> {
            ResultResponse r = resultMapper.toResponse(result);
            enrichWithRanking(r, result);
            return r;
        });
        return PagedResponse.of(mapped);
    }

    @Transactional(readOnly = true)
    public ResultResponse findById(UUID id) {
        Result result = getOrThrow(id);
        ResultResponse r = resultMapper.toResponse(result);
        enrichWithRanking(r, result);
        return r;
    }

    /** All results of an event round, best rank first; unranked (DNS/DNF/DSQ) results last. */
    @Transactional(readOnly = true)
    public List<ResultResponse> findByEventAndRound(UUID eventId, String round) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("CompetitionEvent", "id", eventId);
        }
        String r = RankingService.normaliseRound(round);
        Map<UUID, Ranking> rankMap = rankingRepository.findByEventIdAndRoundOrderByRankPositionAsc(eventId, r)
                .stream()
                .collect(Collectors.toMap(rk -> rk.getResult().getId(), Function.identity()));

        return resultRepository.findByEventIdAndRound(eventId, r).stream()
                .map(result -> {
                    ResultResponse resp = resultMapper.toResponse(result);
                    Ranking rk = rankMap.get(result.getId());
                    if (rk != null) {
                        resp.setRankPosition(rk.getRankPosition());
                        if (rk.getMedal() != null) resp.setMedal(rk.getMedal().name());
                    }
                    return resp;
                })
                .sorted((a, b) -> {
                    if (a.getRankPosition() == null && b.getRankPosition() == null) return 0;
                    if (a.getRankPosition() == null) return 1;
                    if (b.getRankPosition() == null) return -1;
                    return Integer.compare(a.getRankPosition(), b.getRankPosition());
                })
                .toList();
    }

    // ── Write ─────────────────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public ResultResponse create(ResultRequest request) {
        String round = RankingService.normaliseRound(request.getRound());
        if (resultRepository.existsByAthleteIdAndEventIdAndRound(request.getAthleteId(), request.getEventId(), round)) {
            throw new ResourceAlreadyExistsException(
                    "A result already exists for this athlete in this event and round. Update it instead.");
        }
        Result result = resultMapper.toEntity(request);
        resolveRelations(result, request);
        result.setRound(round);
        if (result.getStatus() == null) result.setStatus(ResultStatus.UNOFFICIAL);
        SecurityUtils.currentUserId().flatMap(userRepository::findById).ifPresent(result::setRecordedBy);
        applyVerification(result);

        Result saved = resultRepository.saveAndFlush(result);
        rankingService.recalculate(saved.getEvent().getId(), round);
        log.info("Result recorded for athlete {} in event {}", request.getAthleteId(), request.getEventId());
        return findById(saved.getId());
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public ResultResponse update(UUID id, ResultRequest request) {
        Result result = getOrThrow(id);
        UUID   oldEventId = result.getEvent().getId();
        String oldRound   = result.getRound();

        resultMapper.updateEntity(request, result);
        resolveRelations(result, request);
        result.setRound(RankingService.normaliseRound(request.getRound() != null ? request.getRound() : oldRound));

        if (!Objects.equals(oldEventId, result.getEvent().getId()) || !Objects.equals(oldRound, result.getRound())) {
            if (resultRepository.existsByAthleteIdAndEventIdAndRound(
                    result.getAthlete().getId(), result.getEvent().getId(), result.getRound())) {
                throw new ResourceAlreadyExistsException(
                        "A result already exists for this athlete in the target event and round.");
            }
        }
        applyVerification(result);
        Result saved = resultRepository.saveAndFlush(result);

        rankingService.recalculate(saved.getEvent().getId(), saved.getRound());
        if (!Objects.equals(oldEventId, saved.getEvent().getId()) || !Objects.equals(oldRound, saved.getRound())) {
            rankingService.recalculate(oldEventId, oldRound);
        }
        log.info("Result updated: {}", id);
        return findById(saved.getId());
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public void delete(UUID id) {
        Result result = getOrThrow(id);
        UUID   eventId = result.getEvent().getId();
        String round   = result.getRound();
        rankingRepository.deleteByResult(id);
        resultRepository.delete(result);
        resultRepository.flush();
        rankingService.recalculate(eventId, round);
        log.info("Result deleted: {}", id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Result getOrThrow(UUID id) {
        return resultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Result", "id", id));
    }

    private void resolveRelations(Result result, ResultRequest request) {
        Competition competition = competitionRepository.findById(request.getCompetitionId())
                .orElseThrow(() -> new ResourceNotFoundException("Competition", "id", request.getCompetitionId()));
        CompetitionEvent event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("CompetitionEvent", "id", request.getEventId()));
        if (!event.getCompetition().getId().equals(competition.getId())) {
            throw new BadRequestException("Event does not belong to the given competition.");
        }
        Athlete athlete = athleteRepository.findById(request.getAthleteId())
                .orElseThrow(() -> new ResourceNotFoundException("Athlete", "id", request.getAthleteId()));

        result.setCompetition(competition);
        result.setEvent(event);
        result.setAthlete(athlete);
        if (result.getPerformanceUnit() == null) result.setPerformanceUnit(event.getScoringUnit());
    }

    /** OFFICIAL results carry who verified them and when (DB check constraint keeps both in sync). */
    private void applyVerification(Result result) {
        if (result.getStatus() == ResultStatus.OFFICIAL) {
            if (result.getVerifiedAt() == null) {
                var verifier = SecurityUtils.currentUserId().flatMap(userRepository::findById).orElse(null);
                if (verifier != null) {
                    result.setVerifiedBy(verifier);
                    result.setVerifiedAt(Instant.now());
                }
            }
        } else {
            result.setVerifiedBy(null);
            result.setVerifiedAt(null);
        }
    }

    private void enrichWithRanking(ResultResponse resp, Result result) {
        rankingRepository
                .findByEventIdAndRoundOrderByRankPositionAsc(result.getEvent().getId(), result.getRound())
                .stream()
                .filter(rk -> rk.getResult().getId().equals(result.getId()))
                .findFirst()
                .ifPresent(rk -> {
                    resp.setRankPosition(rk.getRankPosition());
                    if (rk.getMedal() != null) resp.setMedal(rk.getMedal().name());
                });
    }
}
