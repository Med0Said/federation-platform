package com.federation.users.dto;

import com.federation.users.entity.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateRoleRequest {
    @NotNull(message = "Role is required")
    private UserRole role;
}
