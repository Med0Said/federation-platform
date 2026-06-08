package com.federation.results.service;

import com.federation.athletes.entity.Athlete;
import com.federation.athletes.repository.AthleteRepository;
import com.federation.common.exception.ResourceNotFoundException;
import com.federation.common.response.PagedResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultService {

    private final ResultRepository      resultRepository;
    private final RankingRepository     rankingRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionEventRepository eventRepository;
    private final AthleteRepository     athleteRepository;
    private final ResultMapper          resultMapper;

    // ── Read ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<ResultResponse> findAll(UUID competitionId, UUID eventId,
                                                  UUID athleteId, String status,
                                                  Pageable pageable) {
        ResultStatus rs = parseEnum(ResultStatus.class, status);
        Page<Result> page = resultRepository.findAllFiltered(competitionId, eventId, athleteId, rs, pageable);

        // Enrich with ranking positions
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

    @Transactional(readOnly = true)
    public List<ResultResponse> findByEventAndRound(UUID eventId, String round) {
        String r = (round == null || round.isBlank()) ? "FINAL" : round.toUpperCase();
        List<Ranking> rankings = rankingRepository.findByEventIdAndRoundOrderByRankPositionAsc(eventId, r);

        Map<UUID, Ranking> rankMap = rankings.stream()
                .collect(Collectors.toMap(rk -> rk.getResult().getId(), rk -> rk));

        return resultRepository.findByCompetitionIdAndEventIdAndRound(null, eventId, r)
                .stream()
                .map(result -> {
                    ResultResponse resp = resultMapper.toResponse(result);
                    Ranking rk = rankMap.get(result.getId());
                    if (rk != null) {
                        resp.setRankPosition(rk.getRankPosition());
                        if (rk.getMedal() != null) resp.setMedal(rk.getMedal().name());
                    }
                    return resp;
                })
                .collect(Collectors.toList());
    }

    // ── Write ─────────────────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public ResultResponse create(ResultRequest request) {
        Result result = resultMapper.toEntity(request);
        resolveRelations(result, request);
        if (result.getStatus() == null) result.setStatus(ResultStatus.UNOFFICIAL);
        if (result.getRound() == null)  result.setRound("FINAL");
        Result saved = resultRepository.save(result);
        log.info("Result recorded for athlete {} in event {}", request.getAthleteId(), request.getEventId());
        return resultMapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public ResultResponse update(UUID id, ResultRequest request) {
        Result result = getOrThrow(id);
        resultMapper.updateEntity(request, result);
        resolveRelations(result, request);
        Result saved = resultRepository.save(result);
        log.info("Result updated: {}", id);
        return resultMapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public void delete(UUID id) {
        if (!resultRepository.existsById(id))
            throw new ResourceNotFoundException("Result", "id", id);
        resultRepository.deleteById(id);
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
        result.setCompetition(competition);

        CompetitionEvent event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("CompetitionEvent", "id", request.getEventId()));
        result.setEvent(event);

        Athlete athlete = athleteRepository.findById(request.getAthleteId())
                .orElseThrow(() -> new ResourceNotFoundException("Athlete", "id", request.getAthleteId()));
        result.setAthlete(athlete);
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

    private <E extends Enum<E>> E parseEnum(Class<E> clazz, String value) {
        if (value == null || value.isBlank()) return null;
        try { return Enum.valueOf(clazz, value.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
