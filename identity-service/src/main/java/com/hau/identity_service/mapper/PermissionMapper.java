package com.hau.identity_service.mapper;

import com.hau.identity_service.dto.*;
import com.hau.identity_service.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PermissionMapper {
    Permission toPermission(PermissionCreationRequest permissionCreationRequest);
    PermissionResponse toPermissionResponse(Permission permission);
}
