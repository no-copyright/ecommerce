package com.hau.identity_service.dto.request;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleCreationRequest {
    private String name;
    private String description;
    private Set<String> permissions;
}
