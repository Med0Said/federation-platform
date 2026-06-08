package com.federation.clubs.dto;

import com.federation.clubs.entity.ClubStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClubResponse {
    private UUID        id;
    private String      name;
    private String      shortName;
    private String      slug;
    private String      licenseNumber;
    private String      city;
    private String      region;
    private String      country;
    private String      address;
    private Integer     foundedYear;
    private String      description;
    private String      logoUrl;
    private String      website;
    private String      email;
    private String      phone;
    private ClubStatus  status;
    private UUID        managerId;
    private String      managerName;
    private long        activeAthletes;
    private Instant     createdAt;
    private Instant     updatedAt;
}
