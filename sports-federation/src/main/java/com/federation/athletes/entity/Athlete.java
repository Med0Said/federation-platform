package com.federation.athletes.entity;

import com.federation.clubs.entity.Club;
import com.federation.common.util.BaseEntity;
import com.federation.common.util.Gender;
import com.federation.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Registered federation athlete.
 * Mapped to the {@code athletes} table (V6__athletes_schema.sql).
 * Note: {@code category} is a plain column (no longer GENERATED) — computed
 * by {@link com.federation.athletes.service.AthleteService} before save.
 */
@Entity
@Table(name = "athletes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Athlete extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    @Column(name = "license_number", nullable = false, unique = true, length = 50)
    private String licenseNumber;

    @Column(name = "license_expiry")
    private LocalDate licenseExpiry;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(nullable = false, length = 10, columnDefinition = "gender")
    private Gender gender;

    @Column(nullable = false, length = 100)
    @Builder.Default
    private String nationality = "Tunisian";

    @Column(name = "country_code", nullable = false, length = 3)
    @Builder.Default
    private String countryCode = "TUN";

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "height_cm", precision = 5, scale = 2)
    private BigDecimal heightCm;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(length = 20, columnDefinition = "athlete_category")
    private AthleteCategory category;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "emergency_contact_name", length = 200)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 30)
    private String emergencyContactPhone;

    @Column(name = "medical_clearance_date")
    private LocalDate medicalClearanceDate;

    @Column(name = "medical_notes", columnDefinition = "TEXT")
    private String medicalNotes;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(nullable = false, length = 20, columnDefinition = "athlete_status")
    @Builder.Default
    private AthleteStatus status = AthleteStatus.ACTIVE;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
