package com.federation.athletes.dto;

import com.federation.athletes.entity.AthleteCategory;
import com.federation.athletes.entity.AthleteStatus;
import com.federation.common.util.Gender;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AthleteResponse {
    private UUID            id;
    private String          licenseNumber;
    private LocalDate       licenseExpiry;
    private String          firstName;
    private String          lastName;
    private String          fullName;
    private LocalDate       dateOfBirth;
    private int             age;
    private Gender          gender;
    private String          nationality;
    private String          countryCode;
    private AthleteCategory category;
    private AthleteStatus   status;
    private BigDecimal      weightKg;
    private BigDecimal      heightCm;
    private UUID            clubId;
    private String          clubName;
    private String          email;
    private String          phone;
    private String          emergencyContactName;
    private String          emergencyContactPhone;
    private LocalDate       medicalClearanceDate;
    private String          photoUrl;
    private String          notes;
    private UUID            userId;
    private Instant         createdAt;
    private Instant         updatedAt;
}
