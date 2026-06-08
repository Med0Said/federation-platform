package com.federation.athletes.dto;

import com.federation.athletes.entity.AthleteStatus;
import com.federation.common.util.Gender;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AthleteRequest {

    @NotBlank(message = "License number is required")
    @Size(max = 50)
    private String licenseNumber;

    private LocalDate licenseExpiry;

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @Size(max = 100)
    private String nationality;

    @Size(max = 3)
    private String countryCode;

    @DecimalMin("0.0") @DecimalMax("499.99")
    private BigDecimal weightKg;

    @DecimalMin("0.0") @DecimalMax("299.99")
    private BigDecimal heightCm;

    private UUID clubId;

    @Email @Size(max = 255)
    private String email;

    @Size(max = 30)
    private String phone;

    @Size(max = 200)
    private String emergencyContactName;

    @Size(max = 30)
    private String emergencyContactPhone;

    private LocalDate medicalClearanceDate;
    private String    medicalNotes;

    private AthleteStatus status;

    @Size(max = 500)
    private String photoUrl;

    private String notes;
}
