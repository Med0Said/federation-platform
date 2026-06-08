package com.federation.results.entity;

import com.federation.athletes.entity.Athlete;
import com.federation.clubs.entity.Club;
import com.federation.common.util.BaseEntity;
import com.federation.competitions.entity.Competition;
import com.federation.competitions.entity.CompetitionEvent;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "rankings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ranking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private CompetitionEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "result_id", nullable = false)
    private Result result;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    @Column(name = "rank_position", nullable = false)
    private int rankPosition;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String round = "FINAL";

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private MedalType medal;

    @Column(name = "performance_value", precision = 12, scale = 4)
    private BigDecimal performanceValue;

    @Column(name = "performance_text", length = 50)
    private String performanceText;

    @Column
    private Integer points;

    @Column(name = "is_official", nullable = false)
    @Builder.Default
    private boolean official = false;

    @Column(name = "published_at")
    private Instant publishedAt;
}
