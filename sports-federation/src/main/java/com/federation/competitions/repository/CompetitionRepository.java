package com.federation.competitions.repository;

import com.federation.competitions.entity.Competition;
import com.federation.competitions.entity.CompetitionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, UUID> {

    Optional<Competition> findBySlug(String slug);
    boolean existsBySlug(String slug);

    @Query("""
           SELECT c FROM Competition c
           WHERE (:search IS NULL
                  OR LOWER(c.name) LIKE LOWER(CONCAT('%',:search,'%'))
                  OR LOWER(c.sport) LIKE LOWER(CONCAT('%',:search,'%')))
             AND (:status IS NULL OR c.status = :status)
             AND (:from   IS NULL OR c.startDate >= :from)
             AND (:to     IS NULL OR c.endDate   <= :to)
           """)
    Page<Competition> findAllFiltered(
            @Param("search") String search,
            @Param("status") CompetitionStatus status,
            @Param("from")   LocalDate from,
            @Param("to")     LocalDate to,
            Pageable pageable);
}
