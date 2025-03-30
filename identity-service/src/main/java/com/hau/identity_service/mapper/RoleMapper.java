package com.hau.identity_service.mapper;

import com.hau.identity_service.dto.RoleCreationRequest;
import com.hau.identity_service.dto.RoleResponse;
import com.hau.identity_service.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleCreationRequest roleCreationRequest);
    RoleResponse toRoleResponse(Role role);
}
