package com.federation.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Payload for changing the authenticated user's password")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @Schema(description = "Current account password for verification")
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @Schema(description = "New password — must meet complexity requirements")
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one digit and one special character"
    )
    private String newPassword;

    @Schema(description = "Must match newPassword exactly")
    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}
