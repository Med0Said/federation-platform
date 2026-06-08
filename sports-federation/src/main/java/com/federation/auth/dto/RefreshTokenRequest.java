package com.federation.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Payload for obtaining a new token pair from a valid refresh token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    @Schema(description = "The refresh token issued at login or last refresh")
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
