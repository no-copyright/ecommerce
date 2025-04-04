package com.hau.identity_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.hau.identity_service.dto.request.RoleCreationRequest;
import com.hau.identity_service.dto.response.RoleResponse;
import com.hau.identity_service.entity.Role;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleCreationRequest roleCreationRequest);

    RoleResponse toRoleResponse(Role role);
}
