package com.federation.registrations.repository;

import com.federation.registrations.entity.CompetitionRegistration;
import com.federation.registrations.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface CompetitionRegistrationRepository
        extends JpaRepository<CompetitionRegistration, UUID>, JpaSpecificationExecutor<CompetitionRegistration> {

    boolean existsByAthleteIdAndEventId(UUID athleteId, UUID eventId);

    boolean existsByEventId(UUID eventId);

    boolean existsByCompetitionId(UUID competitionId);

    long countByEventIdAndStatusIn(UUID eventId, Collection<RegistrationStatus> statuses);

    long countByCompetitionIdAndStatusIn(UUID competitionId, Collection<RegistrationStatus> statuses);

    long countByCompetitionIdAndClubIdAndStatusIn(UUID competitionId, UUID clubId,
                                                   Collection<RegistrationStatus> statuses);

    long countByStatusIn(Collection<RegistrationStatus> statuses);

    /** Waiting list of an event, head first. */
    List<CompetitionRegistration> findByEventIdAndStatusOrderByWaitlistPositionAsc(
            UUID eventId, RegistrationStatus status);

    List<CompetitionRegistration> findByCompetitionIdAndStatusOrderByCreatedAtAsc(
            UUID competitionId, RegistrationStatus status);

    // Enum literals in JPQL are cast to the Java type name on PostgreSQL — always bind enums as parameters
    @Query("SELECT COALESCE(MAX(r.waitlistPosition), 0) FROM CompetitionRegistration r "
         + "WHERE r.event.id = :eventId AND r.status = :status")
    int maxPositionInStatus(@Param("eventId") UUID eventId, @Param("status") RegistrationStatus status);

    default int maxWaitlistPosition(UUID eventId) {
        return maxPositionInStatus(eventId, RegistrationStatus.WAITLISTED);
    }

    @Query("SELECT COALESCE(MAX(r.bibNumber), 0) FROM CompetitionRegistration r WHERE r.competition.id = :competitionId")
    int maxBibNumber(@Param("competitionId") UUID competitionId);

    /** Uses the sequence created in V8 so numbers stay unique across concurrent inserts. */
    @Query(value = "SELECT nextval('registration_number_seq')", nativeQuery = true)
    long nextRegistrationSequence();
}
