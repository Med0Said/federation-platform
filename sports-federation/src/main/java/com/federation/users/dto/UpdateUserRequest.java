package com.federation.users.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Profile fields a user may change — used by {@code PATCH /auth/me} and
 * {@code PUT /users/{id}}. Null fields are left unchanged.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateUserRequest {

    @Size(min = 1, max = 100)
    private String firstName;

    @Size(min = 1, max = 100)
    private String lastName;

    @Size(max = 20)
    private String phone;

    @Size(max = 500)
    private String avatarUrl;
}
