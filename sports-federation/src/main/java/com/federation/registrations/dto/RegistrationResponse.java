package com.federation.registrations.dto;

import com.federation.registrations.entity.RegistrationStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegistrationResponse {
    private UUID               id;
    private String             registrationNumber;
    private UUID               competitionId;
    private String             competitionName;
    private LocalDate          competitionStartDate;
    private UUID               eventId;
    private String             eventName;
    private String             discipline;
    private UUID               athleteId;
    private String             athleteName;
    private String             licenseNumber;
    private UUID               clubId;
    private String             clubName;
    private UUID               registeredById;
    private String             registeredByName;
    private Integer            bibNumber;
    private BigDecimal         seedValue;
    private String             seedUnit;
    private RegistrationStatus status;
    private Integer            waitlistPosition;
    private Instant            confirmedAt;
    private Instant            cancelledAt;
    private String             cancellationReason;
    private BigDecimal         feeAmount;
    private String             feeCurrency;
    private boolean            feePaid;
    private boolean            medicalWaiver;
    private String             notes;
    private Instant            createdAt;
    private Instant            updatedAt;
}
