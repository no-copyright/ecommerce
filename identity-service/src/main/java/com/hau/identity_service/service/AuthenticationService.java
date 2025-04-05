package com.hau.identity_service.service;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hau.identity_service.dto.request.AuthenticationRequest;
import com.hau.identity_service.dto.request.IntrospectRequest;
import com.hau.identity_service.dto.request.LogoutRequest;
import com.hau.identity_service.dto.response.ApiResponse;
import com.hau.identity_service.dto.response.AuthenticationResponse;
import com.hau.identity_service.dto.response.IntrospectResponse;
import com.hau.identity_service.entity.InvalidatedToken;
import com.hau.identity_service.entity.User;
import com.hau.identity_service.exception.AppException;
import com.hau.identity_service.repository.InvalidatedTokenRepository;
import com.hau.identity_service.repository.UserRepository;
import com.nimbusds.jose.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class AuthenticationService {
    private final UserRepository userRepository;
    private final InvalidatedTokenRepository invalidatedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public ApiResponse<AuthenticationResponse> authenticate(AuthenticationRequest authenticationRequest) {
        User user = userRepository
                .findByUsername(authenticationRequest.getUsername())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(authenticationRequest.getPassword(), user.getPassword())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tên đăng nhập hoặc mật khẩu không chính xác", null);
        }

        String token;
        try {
            token = tokenService.generateToken(user);
        } catch (JOSEException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi tạo token", e);
        }

        return ApiResponse.<AuthenticationResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Đăng nhập thành công")
                .result(AuthenticationResponse.builder()
                        .authenticated(true)
                        .token(token)
                        .build())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public ApiResponse<Void> logout(LogoutRequest logoutRequest) throws ParseException {
        var signToken = tokenService.verifyToken(logoutRequest.getToken(), true);
        String jit = signToken.getJWTClaimsSet().getJWTID();
        Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken =
                InvalidatedToken.builder().id(jit).expiryDate(expiryTime).build();
        invalidatedTokenRepository.save(invalidatedToken);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Đăng xuất thành công")
                .result(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public ApiResponse<IntrospectResponse> introspect(IntrospectRequest introspectRequest) {
        String token = introspectRequest.getToken();

        if (token == null || token.isEmpty()) {
            return buildErrorResponse("Token không được cung cấp");
        }

        try {
            // Sử dụng TokenService thay vì logic tại chỗ
            IntrospectResponse result = tokenService.validateToken(token);

            if (result.isValid()) {
                return ApiResponse.<IntrospectResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message("Token hợp lệ")
                        .result(result)
                        .timestamp(LocalDateTime.now())
                        .build();
            } else {
                return buildErrorResponse("Token không hợp lệ");
            }
        } catch (Exception e) {
            return buildErrorResponse(e.getMessage());
        }
    }

    private ApiResponse<IntrospectResponse> buildErrorResponse(String message) {
        return ApiResponse.<IntrospectResponse>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .result(IntrospectResponse.builder().valid(false).build())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
