package com.hau.identity_service.controller;

import com.hau.identity_service.dto.response.ApiResponse;
import com.hau.identity_service.dto.request.PermissionCreationRequest;
import com.hau.identity_service.dto.response.PermissionResponse;
import com.hau.identity_service.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor

public class PermissionController {
    private final PermissionService permissionService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<PermissionResponse>> createPermission(@RequestBody PermissionCreationRequest permissionCreationRequest) {
        ApiResponse<PermissionResponse> apiResponse = permissionService.createPermission(permissionCreationRequest);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {
        ApiResponse<List<PermissionResponse>> apiResponse = permissionService.getAllPermissions();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{name}")
    public ResponseEntity<ApiResponse<PermissionResponse>> deletePermission(@PathVariable String name) {
        ApiResponse<PermissionResponse> apiResponse = permissionService.deletePermission(name);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
