package com.hau.identity_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.hau.identity_service.dto.request.UserCreateRequest;
import com.hau.identity_service.dto.request.UserUpdateInfoRequest;
import com.hau.identity_service.dto.request.UserUpdateRequest;
import com.hau.identity_service.dto.response.UserResponse;
import com.hau.identity_service.entity.User;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {
    @Mapping(target = "roles", ignore = true)
    User toUser(UserCreateRequest userCreateRequest);

    @Mapping(target = "roles", ignore = true)
    void toUserUpdateRequest(@MappingTarget User user, UserUpdateRequest userUpdateRequest);

    void toUserUpdateInfoRequest(@MappingTarget User user, UserUpdateInfoRequest userUpdateInfoRequest);

    UserResponse toUserResponse(User user);
}
