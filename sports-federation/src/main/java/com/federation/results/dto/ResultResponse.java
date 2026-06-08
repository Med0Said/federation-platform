package com.federation.results.dto;

import com.federation.common.util.Gender;
import com.federation.results.entity.ResultStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResultResponse {
    private UUID         id;
    private UUID         competitionId;
    private String       competitionName;
    private UUID         eventId;
    private String       eventName;
    private String       discipline;
    private UUID         athleteId;
    private String       athleteName;
    private String       licenseNumber;
    private Gender       gender;
    private String       category;
    private String       nationality;
    private UUID         clubId;
    private String       clubName;
    private String       round;
    private Integer      heatNumber;
    private Integer      laneNumber;
    private BigDecimal   performanceValue;
    private String       performanceUnit;
    private String       performanceText;
    private BigDecimal   windSpeed;
    private Integer      points;
    private ResultStatus status;
    private boolean      personalBest;
    private boolean      seasonBest;
    private boolean      competitionRecord;
    private boolean      nationalRecord;
    private Integer      rankPosition;
    private String       medal;
    private String       notes;
    private Instant      createdAt;
    private Instant      updatedAt;
}
