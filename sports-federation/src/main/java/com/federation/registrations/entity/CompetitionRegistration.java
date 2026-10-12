package com.federation.registrations.entity;

import com.federation.athletes.entity.Athlete;
import com.federation.clubs.entity.Club;
import com.federation.common.util.BaseEntity;
import com.federation.competitions.entity.Competition;
import com.federation.competitions.entity.CompetitionEvent;
import com.federation.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * An athlete's entry into one event of a competition.
 * Mapped to {@code competition_registrations} (V8__competition_registrations_schema.sql).
 */
@Entity
@Table(name = "competition_registrations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompetitionRegistration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private CompetitionEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    /** Club snapshot at registration time. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by")
    private User registeredBy;

    @Column(name = "registration_number", nullable = false, unique = true, length = 30)
    private String registrationNumber;

    @Column(name = "bib_number")
    private Integer bibNumber;

    @Column(name = "seed_value", precision = 10, scale = 4)
    private BigDecimal seedValue;

    @Column(name = "seed_unit", length = 30)
    private String seedUnit;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(nullable = false, length = 20, columnDefinition = "registration_status")
    @Builder.Default
    private RegistrationStatus status = RegistrationStatus.PENDING;

    @Column(name = "waitlist_position")
    private Integer waitlistPosition;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "fee_amount", precision = 10, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "fee_currency", nullable = false, length = 3)
    @Builder.Default
    private String feeCurrency = "TND";

    @Column(name = "fee_paid", nullable = false)
    @Builder.Default
    private boolean feePaid = false;

    @Column(name = "medical_waiver", nullable = false)
    @Builder.Default
    private boolean medicalWaiver = false;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
