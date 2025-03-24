package com.hau.identity_service.mapper;

import com.hau.identity_service.dto.UserCreateRequest;
import com.hau.identity_service.dto.UserResponse;
import com.hau.identity_service.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {
    User toUser(UserCreateRequest userCreateRequest);

    UserResponse toUserResponse(User user);
}
