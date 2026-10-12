package com.federation.results.repository;

import com.federation.results.entity.Result;
import com.federation.results.entity.ResultStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResultRepository extends JpaRepository<Result, UUID>, JpaSpecificationExecutor<Result> {

    List<Result> findByEventIdAndRound(UUID eventId, String round);

    List<Result> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);

    boolean existsByCompetitionId(UUID competitionId);

    boolean existsByAthleteIdAndEventIdAndRound(UUID athleteId, UUID eventId, String round);

}
