package com.federation.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "New account registration payload")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @Schema(description = "Unique email address", example = "jane@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Size(max = 255)
    private String email;

    @Schema(description = "Unique username (letters, digits, . _ -)", example = "jane_doe")
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be 3–100 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9._-]+$",
        message = "Username may only contain letters, digits, dots, underscores and hyphens"
    )
    private String username;

    @Schema(description = "Password — min 8 chars, requires upper, lower, digit and special character",
            example = "Secret@123")
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one digit and one special character"
    )
    private String password;

    @Schema(example = "Jane")
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @Schema(example = "Doe")
    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;
}
