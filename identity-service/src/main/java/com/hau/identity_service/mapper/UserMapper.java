package com.hau.identity_service.mapper;

import com.hau.identity_service.dto.UserCreateRequest;
import com.hau.identity_service.dto.UserResponse;
import com.hau.identity_service.dto.UserUpdateRequest;
import com.hau.identity_service.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {
    @Mapping(target = "roles", ignore = true)
    User toUser(UserCreateRequest userCreateRequest);

    @Mapping(target = "roles", ignore = true)
    void toUserUpdateRequest(@MappingTarget User user, UserUpdateRequest userUpdateRequest);

    UserResponse toUserResponse(User user);
}
