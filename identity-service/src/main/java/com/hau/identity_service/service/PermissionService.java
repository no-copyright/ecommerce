package com.hau.identity_service.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.hau.identity_service.dto.request.PermissionCreationRequest;
import com.hau.identity_service.dto.response.ApiResponse;
import com.hau.identity_service.dto.response.PermissionResponse;
import com.hau.identity_service.entity.Permission;
import com.hau.identity_service.exception.AppException;
import com.hau.identity_service.mapper.PermissionMapper;
import com.hau.identity_service.repository.PermissionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    public ApiResponse<PermissionResponse> createPermission(PermissionCreationRequest permissionCreationRequest) {
        Permission permission = permissionMapper.toPermission(permissionCreationRequest);
        permissionRepository.save(permission);
        return ApiResponse.<PermissionResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Tạo mới quyền thành công")
                .result(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public ApiResponse<List<PermissionResponse>> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAll();
        List<PermissionResponse> permissionResponses =
                permissions.stream().map(permissionMapper::toPermissionResponse).toList();
        return ApiResponse.<List<PermissionResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách quyền thành công")
                .result(permissionResponses)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public ApiResponse<PermissionResponse> deletePermission(String name) {
        Permission permission = permissionRepository
                .findByName(name)
                .orElseThrow(
                        () -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy quyền với tên: " + name, null));
        permissionRepository.delete(permission);
        return ApiResponse.<PermissionResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Xóa quyền thành công")
                .result(null)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
