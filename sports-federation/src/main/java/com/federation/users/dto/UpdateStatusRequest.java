package com.federation.users.dto;

import com.federation.users.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateStatusRequest {
    @NotNull(message = "Status is required")
    private UserStatus status;
}
