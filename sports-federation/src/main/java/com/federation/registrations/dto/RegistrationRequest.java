package com.federation.registrations.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/** Payload for {@code POST /competitions/{competitionId}/registrations}. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegistrationRequest {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    /** Optional for athletes registering themselves — defaults to the caller's own athlete record. */
    private UUID athleteId;

    /** Declared entry mark (e.g. personal best), used for seeding heats. */
    @DecimalMin("0.0")
    private BigDecimal seedValue;

    @Size(max = 30)
    private String seedUnit;

    private boolean medicalWaiver;

    @Size(max = 2000)
    private String notes;
}
