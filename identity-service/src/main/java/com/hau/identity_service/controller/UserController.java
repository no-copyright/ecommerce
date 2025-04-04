package com.hau.identity_service.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.hau.identity_service.dto.request.ChangePasswordRequest;
import com.hau.identity_service.dto.request.UserCreateRequest;
import com.hau.identity_service.dto.request.UserUpdateInfoRequest;
import com.hau.identity_service.dto.request.UserUpdateRequest;
import com.hau.identity_service.dto.response.ApiResponse;
import com.hau.identity_service.dto.response.UserResponse;
import com.hau.identity_service.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreateRequest userCreateRequest) {
        ApiResponse<UserResponse> userResponse = userService.createUser(userCreateRequest);
        return new ResponseEntity<>(userResponse, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN') or @userService.isOwnerOfUser(#userId, authentication)")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long userId) {
        ApiResponse<UserResponse> userResponse = userService.getUserById(userId);
        return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<UserResponse>> myInfo() {
        ApiResponse<UserResponse> userResponse = userService.myInfo();
        return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long userId, @Valid @RequestBody UserUpdateRequest userUpdateRequest) {
        ApiResponse<UserResponse> userResponse = userService.updateUser(userId, userUpdateRequest);
        return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN') or @userService.isOwnerOfUser(#userId, authentication)")
    @PutMapping("/{userId}/info")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserInfo(
            @PathVariable Long userId, @Valid @RequestBody UserUpdateInfoRequest userUpdateInfoRequest) {
        ApiResponse<UserResponse> userResponse = userService.updateUserInfo(userId, userUpdateInfoRequest);
        return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN') or @userService.isOwnerOfUser(#userId, authentication)")
    @PatchMapping("/{userId}/password")
    public ResponseEntity<ApiResponse<UserResponse>> updatePassword(
            @PathVariable Long userId, @RequestBody @Valid ChangePasswordRequest changePasswordRequest) {
        ApiResponse<UserResponse> userResponse = userService.changePassword(userId, changePasswordRequest);
        return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(required = false, defaultValue = "0") int pageIndex,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer gender) {
        Page<UserResponse> userPage = userService.getAllUsers(pageIndex, pageSize, username, gender);
        return new ResponseEntity<>(userPage, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> deleteUser(@PathVariable Long userId) {
        ApiResponse<UserResponse> userResponse = userService.deleteUser(userId);
        return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }
}
