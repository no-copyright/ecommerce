package com.hau.identity_service.dto.request;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;

import lombok.*;

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
