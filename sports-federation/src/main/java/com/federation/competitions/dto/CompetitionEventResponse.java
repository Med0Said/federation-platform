package com.federation.competitions.dto;

import com.federation.athletes.entity.AthleteCategory;
import com.federation.common.util.Gender;
import com.federation.competitions.entity.CompetitionStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompetitionEventResponse {
    private UUID              id;
    private UUID              competitionId;
    private String            competitionName;
    private String            name;
    private String            code;
    private String            discipline;
    private Gender            genderCategory;
    private AthleteCategory   ageCategory;
    private Instant           scheduledAt;
    private Integer           durationMinutes;
    private Integer           maxParticipants;
    private String            scoringUnit;
    private boolean           lowerIsBetter;
    private CompetitionStatus status;
    private String            notes;
    private long              registrationCount;
    private long              waitlistCount;
    private Instant           createdAt;
    private Instant           updatedAt;
}
