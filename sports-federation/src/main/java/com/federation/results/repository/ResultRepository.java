package com.federation.results.repository;

import com.federation.results.entity.Result;
import com.federation.results.entity.ResultStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResultRepository extends JpaRepository<Result, UUID> {

    List<Result> findByCompetitionIdAndEventIdAndRound(
            UUID competitionId, UUID eventId, String round);

    @Query("""
           SELECT r FROM Result r
           LEFT JOIN FETCH r.athlete a
           LEFT JOIN FETCH a.club
           LEFT JOIN FETCH r.event e
           LEFT JOIN FETCH r.competition c
           WHERE (:competitionId IS NULL OR r.competition.id = :competitionId)
             AND (:eventId       IS NULL OR r.event.id       = :eventId)
             AND (:athleteId     IS NULL OR r.athlete.id     = :athleteId)
             AND (:status        IS NULL OR r.status         = :status)
           """)
    Page<Result> findAllFiltered(
            @Param("competitionId") UUID competitionId,
            @Param("eventId")       UUID eventId,
            @Param("athleteId")     UUID athleteId,
            @Param("status")        ResultStatus status,
            Pageable pageable);
}
