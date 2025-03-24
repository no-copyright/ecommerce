package com.hau.identity_service.service;

import com.hau.identity_service.dto.ApiResponse;
import com.hau.identity_service.dto.UserCreateRequest;
import com.hau.identity_service.dto.UserResponse;
import com.hau.identity_service.entity.User;
import com.hau.identity_service.mapper.UserMapper;
import com.hau.identity_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public ApiResponse<UserResponse> createUser(UserCreateRequest userCreateRequest) {
        Optional<User> existedUser = userRepository.findByUsername(userCreateRequest.getUsername());
        if (existedUser.isPresent()) {
            return ApiResponse.<UserResponse>builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Username đã tồn tại")
                    .result(0)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
        User user = userMapper.toUser(userCreateRequest);
        userRepository.save(user);
        return ApiResponse.<UserResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Tạo mới user thành công")
                .result(1)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
