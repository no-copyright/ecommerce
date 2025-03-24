package com.hau.identity_service.controller;

import com.hau.identity_service.dto.ApiResponse;
import com.hau.identity_service.dto.UserCreateRequest;
import com.hau.identity_service.dto.UserResponse;
import com.hau.identity_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor

public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserCreateRequest userCreateRequest) {
        ApiResponse<UserResponse> userResponse = userService.createUser(userCreateRequest);
        return new ResponseEntity<>(userResponse, HttpStatus.CREATED);
    }


}
