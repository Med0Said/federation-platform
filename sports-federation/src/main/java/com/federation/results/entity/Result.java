package com.federation.results.entity;

import com.federation.athletes.entity.Athlete;
import com.federation.common.util.BaseEntity;
import com.federation.competitions.entity.Competition;
import com.federation.competitions.entity.CompetitionEvent;
import com.federation.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Result extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private CompetitionEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String round = "FINAL";

    @Column(name = "heat_number")
    private Integer heatNumber;

    @Column(name = "lane_number")
    private Integer laneNumber;

    @Column(name = "performance_value", precision = 12, scale = 4)
    private BigDecimal performanceValue;

    @Column(name = "performance_unit", length = 30)
    private String performanceUnit;

    @Column(name = "performance_text", length = 50)
    private String performanceText;

    @Column(name = "wind_speed", precision = 5, scale = 2)
    private BigDecimal windSpeed;

    @Column
    private Integer points;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(nullable = false, length = 20, columnDefinition = "result_status")
    @Builder.Default
    private ResultStatus status = ResultStatus.UNOFFICIAL;

    @Column(name = "disqualification_reason", columnDefinition = "TEXT")
    private String disqualificationReason;

    @Column(name = "is_personal_best", nullable = false)
    @Builder.Default
    private boolean personalBest = false;

    @Column(name = "is_season_best", nullable = false)
    @Builder.Default
    private boolean seasonBest = false;

    @Column(name = "is_competition_record", nullable = false)
    @Builder.Default
    private boolean competitionRecord = false;

    @Column(name = "is_national_record", nullable = false)
    @Builder.Default
    private boolean nationalRecord = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
