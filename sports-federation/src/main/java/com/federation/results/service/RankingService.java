package com.federation.results.service;

import com.federation.common.exception.ResourceNotFoundException;
import com.federation.competitions.entity.CompetitionEvent;
import com.federation.competitions.repository.CompetitionEventRepository;
import com.federation.results.dto.RankingResponse;
import com.federation.results.entity.MedalType;
import com.federation.results.entity.Ranking;
import com.federation.results.entity.Result;
import com.federation.results.entity.ResultStatus;
import com.federation.results.repository.RankingRepository;
import com.federation.results.repository.ResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Computes the standings of an event round from its results and materialises
 * them in the {@code rankings} table.
 *
 * Rules:
 * <ul>
 *   <li>Only results with a performance value and a rankable status
 *       (UNOFFICIAL, OFFICIAL, PROTESTED) are ranked; DNS / DNF / DSQ are not.</li>
 *   <li>Sorted ascending when the event's {@code lowerIsBetter} flag is set
 *       (times), descending otherwise (distances, points).</li>
 *   <li>Positions are unique (DB constraint); equal marks are separated by entry order.</li>
 *   <li>Medals are awarded to positions 1–3 of the FINAL round.</li>
 *   <li>A ranking row is official when its underlying result is OFFICIAL.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    public static final String FINAL_ROUND = "FINAL";

    private static final Set<ResultStatus> RANKABLE =
            EnumSet.of(ResultStatus.UNOFFICIAL, ResultStatus.OFFICIAL, ResultStatus.PROTESTED);

    private static final MedalType[] MEDALS = { MedalType.GOLD, MedalType.SILVER, MedalType.BRONZE };

    private final RankingRepository          rankingRepository;
    private final ResultRepository           resultRepository;
    private final CompetitionEventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<RankingResponse> findByEvent(UUID eventId, String round) {
        CompetitionEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("CompetitionEvent", "id", eventId));
        return rankingRepository.findByEventIdAndRoundOrderByRankPositionAsc(event.getId(), normaliseRound(round))
                .stream().map(this::toResponse).toList();
    }

    /** Staff can force a recalculation (e.g. after editing results directly in the database). */
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public List<RankingResponse> recalculateAndGet(UUID eventId, String round) {
        recalculate(eventId, round);
        return findByEvent(eventId, round);
    }

    /**
     * Rebuilds the rankings of one event round. Called automatically whenever a
     * result of that round is created, updated or deleted.
     *
     * @return number of ranked athletes
     */
    @Transactional
    public int recalculate(UUID eventId, String round) {
        String r = normaliseRound(round);
        CompetitionEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("CompetitionEvent", "id", eventId));

        rankingRepository.deleteByEventAndRound(eventId, r);

        Comparator<BigDecimal> byMark = event.isLowerIsBetter()
                ? Comparator.naturalOrder()
                : Comparator.reverseOrder();
        List<Result> ranked = resultRepository.findByEventIdAndRound(eventId, r).stream()
                .filter(res -> res.getPerformanceValue() != null && RANKABLE.contains(res.getStatus()))
                .sorted(Comparator.comparing(Result::getPerformanceValue, byMark)
                        .thenComparing(Result::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Result::getId))
                .toList();

        Instant now = Instant.now();
        List<Ranking> rows = new ArrayList<>(ranked.size());
        for (int i = 0; i < ranked.size(); i++) {
            Result res = ranked.get(i);
            boolean official = res.getStatus() == ResultStatus.OFFICIAL;
            rows.add(Ranking.builder()
                    .competition(res.getCompetition())
                    .event(event)
                    .result(res)
                    .athlete(res.getAthlete())
                    .club(res.getAthlete().getClub())
                    .round(r)
                    .rankPosition(i + 1)
                    .medal(FINAL_ROUND.equals(r) && i < MEDALS.length ? MEDALS[i] : null)
                    .performanceValue(res.getPerformanceValue())
                    .performanceText(res.getPerformanceText())
                    .points(res.getPoints())
                    .official(official)
                    .publishedAt(official ? now : null)
                    .build());
        }
        rankingRepository.saveAll(rows);
        log.info("Rankings recalculated for event {} round {}: {} athlete(s)", eventId, r, rows.size());
        return rows.size();
    }

    public static String normaliseRound(String round) {
        return (round == null || round.isBlank()) ? FINAL_ROUND : round.strip().toUpperCase(Locale.ROOT);
    }

    private RankingResponse toResponse(Ranking rk) {
        return RankingResponse.builder()
                .id(rk.getId())
                .competitionId(rk.getCompetition().getId())
                .eventId(rk.getEvent().getId())
                .eventName(rk.getEvent().getName())
                .round(rk.getRound())
                .rankPosition(rk.getRankPosition())
                .medal(rk.getMedal() != null ? rk.getMedal().name() : null)
                .resultId(rk.getResult().getId())
                .athleteId(rk.getAthlete().getId())
                .athleteName(rk.getAthlete().getFullName())
                .licenseNumber(rk.getAthlete().getLicenseNumber())
                .clubId(rk.getClub() != null ? rk.getClub().getId() : null)
                .clubName(rk.getClub() != null ? rk.getClub().getName() : null)
                .performanceValue(rk.getPerformanceValue())
                .performanceText(rk.getPerformanceText())
                .points(rk.getPoints())
                .official(rk.isOfficial())
                .publishedAt(rk.getPublishedAt())
                .build();
    }
}
