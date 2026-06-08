package com.federation.competitions.repository;

import com.federation.competitions.entity.CompetitionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompetitionEventRepository extends JpaRepository<CompetitionEvent, UUID> {

    List<CompetitionEvent> findByCompetitionIdOrderByScheduledAtAsc(UUID competitionId);

    long countByCompetitionId(UUID competitionId);
}
