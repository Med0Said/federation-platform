package com.federation.clubs.entity;

import com.federation.common.util.BaseEntity;
import com.federation.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * Federation-registered sports club.
 * Mapped to the {@code clubs} table (V5__clubs_schema.sql).
 */
@Entity
@Table(name = "clubs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Club extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "short_name", length = 20)
    private String shortName;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(name = "license_number", nullable = false, unique = true, length = 50)
    private String licenseNumber;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String region;

    @Column(nullable = false, length = 100)
    @Builder.Default
    private String country = "TN";

    @Column(length = 255)
    private String address;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "founded_year")
    private Short foundedYear;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(length = 300)
    private String website;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(nullable = false, length = 20, columnDefinition = "club_status")
    @Builder.Default
    private ClubStatus status = ClubStatus.ACTIVE;
}
