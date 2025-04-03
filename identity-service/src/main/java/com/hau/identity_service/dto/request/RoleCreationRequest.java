package com.hau.identity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleCreationRequest {
    @NotBlank(message = "Tên vai trò không được để trống")
    private String name;
    private String description;
    private Set<String> permissions;
}
