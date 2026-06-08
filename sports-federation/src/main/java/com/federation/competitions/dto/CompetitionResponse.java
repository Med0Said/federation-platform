package com.federation.competitions.dto;

import com.federation.competitions.entity.CompetitionFormat;
import com.federation.competitions.entity.CompetitionLevel;
import com.federation.competitions.entity.CompetitionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompetitionResponse {
    private UUID              id;
    private String            name;
    private String            slug;
    private String            edition;
    private String            description;
    private String            sport;
    private CompetitionLevel  level;
    private CompetitionFormat format;
    private CompetitionStatus status;
    private String            venueName;
    private String            venueCity;
    private String            venueCountry;
    private LocalDate         startDate;
    private LocalDate         endDate;
    private LocalDate         registrationDeadline;
    private Integer           maxParticipants;
    private Integer           maxPerClub;
    private UUID              hostClubId;
    private String            hostClubName;
    private UUID              organizerId;
    private String            organizerName;
    private String            rulesUrl;
    private String            posterUrl;
    private String            prizeInfo;
    private BigDecimal        entryFee;
    private String            currency;
    private long              eventCount;
    private long              confirmedRegistrations;
    private Instant           publishedAt;
    private Instant           createdAt;
    private Instant           updatedAt;
}
