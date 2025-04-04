package com.hau.identity_service.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionCreationRequest {
    @NotBlank(message = "Tên quyền không được để trống")
    private String name;

    private String description;
}
