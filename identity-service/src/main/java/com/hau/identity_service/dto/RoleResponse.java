package com.hau.identity_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {
    private String name;
    private String description;
    private Set<PermissionResponse> permissions;
}
