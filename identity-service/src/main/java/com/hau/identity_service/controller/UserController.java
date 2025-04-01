package com.hau.identity_service.controller;

import com.hau.identity_service.dto.*;
import com.hau.identity_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserCreateRequest userCreateRequest) {
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

    @PreAuthorize("hasRole('ADMIN') or @userService.isOwnerOfUser(#userId, authentication)")
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable Long userId, @Valid @RequestBody UserUpdateRequest userUpdateRequest) {
        ApiResponse<UserResponse> userResponse = userService.updateUser(userId, userUpdateRequest);
        return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN') or @userService.isOwnerOfUser(#userId, authentication)")
    @PatchMapping("/{userId}/password")
    public ResponseEntity<ApiResponse<UserResponse>> updatePassword(@PathVariable Long userId, @RequestBody ChangePasswordRequest changePasswordRequest) {
        ApiResponse<UserResponse> userResponse = userService.changePassword(userId, changePasswordRequest);
        return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(required = false, defaultValue = "0") int pageIndex,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer gender
    ) {
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