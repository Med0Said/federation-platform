package com.federation.results.repository;

import com.federation.results.entity.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RankingRepository extends JpaRepository<Ranking, UUID> {

    List<Ranking> findByEventIdAndRoundOrderByRankPositionAsc(UUID eventId, String round);

    List<Ranking> findByCompetitionIdAndOfficialTrueOrderByRankPositionAsc(UUID competitionId);

    List<Ranking> findByCompetitionIdAndMedalIsNotNull(UUID competitionId);

    /** Bulk delete before a recalculation; flushes pending changes first so constraints see a clean slate. */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM Ranking r WHERE r.event.id = :eventId AND r.round = :round")
    int deleteByEventAndRound(@Param("eventId") UUID eventId, @Param("round") String round);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM Ranking r WHERE r.result.id = :resultId")
    int deleteByResult(@Param("resultId") UUID resultId);
}
