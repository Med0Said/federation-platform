package com.federation.competitions.entity;

import com.federation.athletes.entity.AthleteCategory;
import com.federation.common.util.BaseEntity;
import com.federation.common.util.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "competition_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompetitionEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String discipline;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(name = "gender_category", length = 10, columnDefinition = "gender")
    private Gender genderCategory;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(name = "age_category", length = 20, columnDefinition = "athlete_category")
    private AthleteCategory ageCategory;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "scoring_unit", length = 50)
    private String scoringUnit;

    @Column(name = "lower_is_better", nullable = false)
    @Builder.Default
    private boolean lowerIsBetter = true;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(nullable = false, length = 25, columnDefinition = "competition_status")
    @Builder.Default
    private CompetitionStatus status = CompetitionStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
