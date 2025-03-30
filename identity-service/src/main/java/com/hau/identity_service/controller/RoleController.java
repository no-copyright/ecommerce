package com.hau.identity_service.controller;

import com.hau.identity_service.dto.ApiResponse;
import com.hau.identity_service.dto.RoleCreationRequest;
import com.hau.identity_service.dto.RoleResponse;
import com.hau.identity_service.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/roles")
public class RoleController {
    private final RoleService roleService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@RequestBody RoleCreationRequest roleRequest) {
        ApiResponse<RoleResponse> apiResponse = roleService.createRole(roleRequest);
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        ApiResponse<List<RoleResponse>> apiResponse = roleService.getAllRoles();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{name}")
    public ResponseEntity<ApiResponse<RoleResponse>> deleteRole(@PathVariable String name) {
        ApiResponse<RoleResponse> apiResponse = roleService.deleteRole(name);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
