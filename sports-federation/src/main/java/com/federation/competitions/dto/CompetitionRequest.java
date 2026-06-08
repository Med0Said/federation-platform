package com.federation.competitions.dto;

import com.federation.competitions.entity.CompetitionFormat;
import com.federation.competitions.entity.CompetitionLevel;
import com.federation.competitions.entity.CompetitionStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompetitionRequest {

    @NotBlank(message = "Competition name is required")
    @Size(max = 200)
    private String name;

    @Size(max = 20)
    private String edition;

    private String description;

    @NotBlank(message = "Sport is required")
    @Size(max = 100)
    private String sport;

    private CompetitionLevel  level;
    private CompetitionFormat format;

    @Size(max = 200)
    private String venueName;

    @Size(max = 100)
    private String venueCity;

    @Size(max = 100)
    private String venueCountry;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private LocalDate registrationDeadline;

    @Min(1)
    private Integer maxParticipants;

    @Min(1)
    private Integer maxPerClub;

    private UUID hostClubId;
    private UUID organizerId;

    @Size(max = 500)
    private String rulesUrl;

    @Size(max = 500)
    private String posterUrl;

    private String prizeInfo;

    @DecimalMin("0.0")
    private BigDecimal entryFee;

    @Size(max = 3)
    private String currency;

    private CompetitionStatus status;
}
