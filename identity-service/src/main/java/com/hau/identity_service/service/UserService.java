package com.hau.identity_service.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hau.identity_service.dto.request.ChangePasswordRequest;
import com.hau.identity_service.dto.request.UserCreateRequest;
import com.hau.identity_service.dto.request.UserUpdateInfoRequest;
import com.hau.identity_service.dto.request.UserUpdateRequest;
import com.hau.identity_service.dto.response.ApiResponse;
import com.hau.identity_service.dto.response.UserResponse;
import com.hau.identity_service.entity.User;
import com.hau.identity_service.exception.AppException;
import com.hau.identity_service.mapper.UserMapper;
import com.hau.identity_service.repository.RoleRepository;
import com.hau.identity_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public ApiResponse<UserResponse> createUser(UserCreateRequest userCreateRequest) {
        User user = userMapper.toUser(userCreateRequest);
        var roles = roleRepository.findAllById(Set.of("USER"));
        user.setRoles(new HashSet<>(roles));
        user.setPassword(passwordEncoder.encode(userCreateRequest.getPassword()));
        try {
            userRepository.save(user);

            return ApiResponse.<UserResponse>builder()
                    .status(HttpStatus.CREATED.value())
                    .message("Tạo mới user thành công")
                    .result(null)
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, "username đã tồn tại", null);
        }
    }

    public Page<UserResponse> getAllUsers(int pageIndex, int pageSize, String username, Integer gender) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info("Username : {}", authentication.getName());
        authentication.getAuthorities().forEach(grantedAuthority -> log.info(grantedAuthority.getAuthority()));

        Specification<User> spec = Specification.where(null);

        if (username != null) {
            spec = spec.and(
                    (root, query, cb) -> cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%"));
        }

        if (gender != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("gender"), gender));
        }

        Pageable pageable = PageRequest.of(pageIndex, pageSize);
        Page<User> userPage = userRepository.findAll(spec, pageable);

        return userPage.map(userMapper::toUserResponse);
    }

    public ApiResponse<UserResponse> myInfo() {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy user", null));
        UserResponse userResponse = userMapper.toUserResponse(user);

        return ApiResponse.<UserResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy thông tin user thành công")
                .result(userResponse)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public ApiResponse<UserResponse> getUserById(Long id) {
        User user = findUserById(id);
        UserResponse userResponse = userMapper.toUserResponse(user);
        return ApiResponse.<UserResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy thông tin user thành công")
                .result(userResponse)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public ApiResponse<UserResponse> updateUser(Long id, UserUpdateRequest userUpdateRequest) {
        User user = findUserById(id);
        userMapper.toUserUpdateRequest(user, userUpdateRequest);
        var roles = roleRepository.findAllById(userUpdateRequest.getRoles());
        user.setRoles(new HashSet<>(roles));
        user.setPassword(passwordEncoder.encode(userUpdateRequest.getPassword()));

        userRepository.save(user);
        return ApiResponse.<UserResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật thông tin user thành công")
                .result(userMapper.toUserResponse(user))
                .timestamp(LocalDateTime.now())
                .build();
    }

    public ApiResponse<UserResponse> updateUserInfo(Long id, UserUpdateInfoRequest userUpdateInfoRequest) {
        User user = findUserById(id);
        userMapper.toUserUpdateInfoRequest(user, userUpdateInfoRequest);
        userRepository.save(user);
        return ApiResponse.<UserResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật thông tin user thành công")
                .result(userMapper.toUserResponse(user))
                .timestamp(LocalDateTime.now())
                .build();
    }

    public ApiResponse<UserResponse> changePassword(Long id, ChangePasswordRequest changePasswordRequest) {
        User user = findUserById(id);
        if (!passwordEncoder.matches(changePasswordRequest.getOldPassword(), user.getPassword())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mật khẩu cũ không đúng", null);
        }

        user.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        userRepository.save(user);
        return ApiResponse.<UserResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Đổi mật khẩu thành công")
                .result(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public ApiResponse<UserResponse> deleteUser(Long id) {
        User user = findUserById(id);
        userRepository.delete(user);
        return ApiResponse.<UserResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Xóa user thành công")
                .result(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public User findUserById(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy user có id: " + id, null));
    }

    public boolean isOwnerOfUser(Long requestedUserId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String authenticatedUsername = authentication.getName();
        try {
            User requestedUser = findUserById(requestedUserId);
            return requestedUser.getUsername().equals(authenticatedUsername);
        } catch (AppException e) {
            log.warn("Không tìm thấy người dùng với ID: {}", requestedUserId);
            return false;
        }
    }
}
