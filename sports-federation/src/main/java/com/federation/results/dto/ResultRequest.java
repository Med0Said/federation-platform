package com.federation.results.dto;

import com.federation.results.entity.ResultStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResultRequest {

    @NotNull(message = "Competition ID is required")
    private UUID competitionId;

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @NotNull(message = "Athlete ID is required")
    private UUID athleteId;

    private String     round;
    private Integer    heatNumber;
    private Integer    laneNumber;
    private BigDecimal performanceValue;
    private String     performanceUnit;
    private String     performanceText;
    private BigDecimal windSpeed;
    private Integer    points;
    private ResultStatus status;

    private boolean personalBest;
    private boolean seasonBest;
    private boolean competitionRecord;
    private boolean nationalRecord;

    private UUID   recordedById;
    private String notes;
}
