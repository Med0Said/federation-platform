package com.federation.clubs.dto;

import com.federation.clubs.entity.ClubStatus;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClubRequest {

    @NotBlank(message = "Club name is required")
    @Size(max = 200)
    private String name;

    @Size(max = 20)
    private String shortName;

    @NotBlank(message = "License number is required")
    @Size(max = 50)
    private String licenseNumber;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String region;

    @Size(max = 100)
    private String country;

    @Size(max = 255)
    private String address;

    @Min(1800)
    private Integer foundedYear;

    private String description;

    @Size(max = 500)
    private String logoUrl;

    @Size(max = 300)
    private String website;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 30)
    private String phone;

    private ClubStatus status;
}
