package com.hau.identity_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.hau.identity_service.dto.request.PermissionCreationRequest;
import com.hau.identity_service.dto.response.PermissionResponse;
import com.hau.identity_service.entity.Permission;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PermissionMapper {
    Permission toPermission(PermissionCreationRequest permissionCreationRequest);

    PermissionResponse toPermissionResponse(Permission permission);
}
