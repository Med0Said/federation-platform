package com.federation.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Login credentials — accepts email or username")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @Schema(description = "Email address or username", example = "admin@federation.local")
    @NotBlank(message = "Email or username is required")
    private String usernameOrEmail;

    @Schema(description = "Account password", example = "Admin@1234")
    @NotBlank(message = "Password is required")
    private String password;
}
