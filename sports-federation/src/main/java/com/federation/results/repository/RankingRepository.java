package com.federation.results.repository;

import com.federation.results.entity.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RankingRepository extends JpaRepository<Ranking, UUID> {

    List<Ranking> findByEventIdAndRoundOrderByRankPositionAsc(UUID eventId, String round);

    List<Ranking> findByCompetitionIdAndOfficialTrueOrderByRankPositionAsc(UUID competitionId);

    @Query("""
           SELECT r FROM Ranking r
           LEFT JOIN FETCH r.athlete a
           LEFT JOIN FETCH a.club
           WHERE r.event.id = :eventId
             AND r.round    = :round
             AND r.official = true
           ORDER BY r.rankPosition ASC
           """)
    List<Ranking> findOfficialByEventAndRound(
            @Param("eventId") UUID eventId,
            @Param("round")   String round);
}
