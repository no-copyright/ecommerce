package com.hau.identity_service.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.hau.identity_service.dto.request.RoleCreationRequest;
import com.hau.identity_service.dto.response.ApiResponse;
import com.hau.identity_service.dto.response.RoleResponse;
import com.hau.identity_service.entity.Role;
import com.hau.identity_service.exception.AppException;
import com.hau.identity_service.mapper.RoleMapper;
import com.hau.identity_service.repository.PermissionRepository;
import com.hau.identity_service.repository.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    public ApiResponse<RoleResponse> createRole(RoleCreationRequest request) {
        var role = roleMapper.toRole(request);
        var permissionIds = request.getPermissions();
        var permissions = permissionRepository.findAllById(permissionIds);
        if (CollectionUtils.isEmpty(permissions)) {
            return ApiResponse.<RoleResponse>builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Không tìm thấy permission nào")
                    .result(null)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
        role.setPermissions(new HashSet<>(permissions));
        roleRepository.save(role);
        return ApiResponse.<RoleResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Tạo role thành công")
                .result(roleMapper.toRoleResponse(role))
                .timestamp(LocalDateTime.now())
                .build();
    }

    public ApiResponse<List<RoleResponse>> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        List<RoleResponse> roleResponses =
                roles.stream().map(roleMapper::toRoleResponse).toList();
        return ApiResponse.<List<RoleResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách role thành công")
                .result(roleResponses)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public ApiResponse<RoleResponse> deleteRole(String name) {
        Role role = roleRepository
                .findByName(name)
                .orElseThrow(
                        () -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy role với tên: " + name, null));
        roleRepository.delete(role);
        return ApiResponse.<RoleResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Xóa role thành công")
                .result(null)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
