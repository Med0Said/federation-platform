package com.federation.registrations.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/** Staff-only partial update: {@code PATCH /registrations/{id}}. Null fields are left unchanged. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegistrationUpdateRequest {

    @Min(1)
    private Integer bibNumber;

    @DecimalMin("0.0")
    private BigDecimal seedValue;

    @Size(max = 30)
    private String seedUnit;

    private Boolean feePaid;

    private Boolean medicalWaiver;

    @Size(max = 2000)
    private String notes;
}
