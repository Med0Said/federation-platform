package com.federation.competitions.entity;

import com.federation.clubs.entity.Club;
import com.federation.common.util.BaseEntity;
import com.federation.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "competitions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Competition extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(length = 20)
    private String edition;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String sport;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(nullable = false, length = 20, columnDefinition = "competition_level")
    @Builder.Default
    private CompetitionLevel level = CompetitionLevel.NATIONAL;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(nullable = false, length = 15, columnDefinition = "competition_format")
    @Builder.Default
    private CompetitionFormat format = CompetitionFormat.INDIVIDUAL;

    @Column(name = "venue_name", length = 200)
    private String venueName;

    @Column(name = "venue_city", length = 100)
    private String venueCity;

    @Column(name = "venue_country", nullable = false, length = 100)
    @Builder.Default
    private String venueCountry = "TN";

    @Column(name = "venue_address", columnDefinition = "TEXT")
    private String venueAddress;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "registration_deadline")
    private LocalDate registrationDeadline;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "max_per_club")
    private Integer maxPerClub;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id")
    private User organizer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_club_id")
    private Club hostClub;

    @Column(name = "rules_url", length = 500)
    private String rulesUrl;

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Column(name = "prize_info", columnDefinition = "TEXT")
    private String prizeInfo;

    @Column(name = "entry_fee", precision = 10, scale = 2)
    private BigDecimal entryFee;

    @Column(length = 3)
    @Builder.Default
    private String currency = "TND";

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(nullable = false, length = 25, columnDefinition = "competition_status")
    @Builder.Default
    private CompetitionStatus status = CompetitionStatus.DRAFT;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
}
