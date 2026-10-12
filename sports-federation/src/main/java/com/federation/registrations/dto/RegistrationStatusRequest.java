package com.federation.registrations.dto;

import com.federation.registrations.entity.RegistrationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Payload for {@code PATCH /registrations/{id}/status}. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegistrationStatusRequest {

    @NotNull(message = "Status is required")
    private RegistrationStatus status;

    /** Required context when cancelling or disqualifying. */
    @Size(max = 2000)
    private String reason;
}
