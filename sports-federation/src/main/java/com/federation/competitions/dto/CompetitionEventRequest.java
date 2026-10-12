package com.federation.competitions.dto;

import com.federation.athletes.entity.AthleteCategory;
import com.federation.common.util.Gender;
import com.federation.competitions.entity.CompetitionStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompetitionEventRequest {

    @NotBlank(message = "Event name is required")
    @Size(max = 200)
    private String name;

    @Size(max = 30)
    private String code;

    @NotBlank(message = "Discipline is required")
    @Size(max = 100)
    private String discipline;

    /** {@code null} = open to all genders. */
    private Gender genderCategory;

    /** {@code null} = open to all age categories. */
    private AthleteCategory ageCategory;

    private Instant scheduledAt;

    @Min(1)
    private Integer durationMinutes;

    @Min(1)
    private Integer maxParticipants;

    @Size(max = 50)
    private String scoringUnit;

    /** {@code true} for time-based events, {@code false} for distance/points. Defaults to {@code true}. */
    private Boolean lowerIsBetter;

    private CompetitionStatus status;

    private String notes;
}
