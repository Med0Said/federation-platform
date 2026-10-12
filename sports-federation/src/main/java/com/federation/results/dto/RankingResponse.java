package com.federation.results.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RankingResponse {
    private UUID       id;
    private UUID       competitionId;
    private UUID       eventId;
    private String     eventName;
    private String     round;
    private int        rankPosition;
    private String     medal;
    private UUID       resultId;
    private UUID       athleteId;
    private String     athleteName;
    private String     licenseNumber;
    private UUID       clubId;
    private String     clubName;
    private BigDecimal performanceValue;
    private String     performanceText;
    private Integer    points;
    private boolean    official;
    private Instant    publishedAt;
}
