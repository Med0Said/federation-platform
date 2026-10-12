package com.federation.users.dto;

import com.federation.common.validation.StrongPassword;
import com.federation.users.entity.UserRole;
import com.federation.users.entity.UserStatus;
import jakarta.validation.constraints.*;
import lombok.*;

/** Admin-created account (any role). */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
             message = "Username may only contain letters, digits, dots, underscores and hyphens")
    private String username;

    @NotBlank(message = "Password is required")
    @StrongPassword
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @NotNull(message = "Role is required")
    private UserRole role;

    /** Defaults to ACTIVE. */
    private UserStatus status;

    @Size(max = 20)
    private String phone;
}
