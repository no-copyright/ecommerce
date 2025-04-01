package com.hau.identity_service.service;

import com.hau.identity_service.dto.request.AuthenticationRequest;
import com.hau.identity_service.dto.request.IntrospectRequest;
import com.hau.identity_service.dto.response.ApiResponse;
import com.hau.identity_service.dto.response.AuthenticationResponse;
import com.hau.identity_service.dto.response.IntrospectResponse;
import com.hau.identity_service.entity.User;
import com.hau.identity_service.exception.AppException;
import com.hau.identity_service.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Value("${jwt.signerKey}")
    private String SINGER_KEY;

    @Value("${jwt.issuer}")
    private String ISSUER;

    @Value("${jwt.expirationMinutes}")
    private Long EXPIRATION;

    public ApiResponse<AuthenticationResponse> authenticate(AuthenticationRequest authenticationRequest) {
        User user = userRepository.findByUsername(authenticationRequest.getUsername())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(authenticationRequest.getPassword(), user.getPassword())) {
            return ApiResponse.<AuthenticationResponse>builder()
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .message("Thông tin đăng nhập không chính xác")
                    .result(AuthenticationResponse.builder()
                            .authenticated(false)
                            .token(null)
                            .build())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        String token;
        try {
            token = generateToken(user);
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

    public ApiResponse<IntrospectResponse> introspect(IntrospectRequest introspectRequest) {
        String token = introspectRequest.getToken();

        // Check if token is empty or null
        if (token == null || token.isEmpty()) {
            return buildErrorResponse("Token không được cung cấp");
        }

        try {
            // Parse and verify token
            JWSVerifier verifier = new MACVerifier(SINGER_KEY.getBytes());
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            // Verify signature
            boolean signatureValid = signedJWT.verify(verifier);
            if (!signatureValid) {
                return buildErrorResponse("Chữ ký token không hợp lệ");
            }

            // Check expiration
            Date expirationTime = claimsSet.getExpirationTime();
            if (expirationTime == null || expirationTime.before(new Date())) {
                return buildErrorResponse("Token đã hết hạn");
            }

            // Check issuer
            String tokenIssuer = claimsSet.getIssuer();
            if (tokenIssuer == null || !tokenIssuer.equals(ISSUER)) {
                return buildErrorResponse("Token issuer không hợp lệ");
            }

            // Check if token is not used before its issue time
            Date issueTime = claimsSet.getIssueTime();
            if (issueTime != null && issueTime.after(new Date())) {
                return buildErrorResponse("Token chưa có hiệu lực");
            }

            return ApiResponse.<IntrospectResponse>builder()
                    .status(HttpStatus.OK.value())
                    .message("Token hợp lệ")
                    .result(IntrospectResponse.builder()
                            .valid(true)
                            .username(claimsSet.getSubject())
                            .build())
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (ParseException e) {
            return buildErrorResponse("Token không đúng định dạng");
        } catch (JOSEException e) {
            return buildErrorResponse("Lỗi xác thực token");
        } catch (Exception e) {
            return buildErrorResponse("Lỗi không xác định: " + e.getMessage());
        }
    }

    private ApiResponse<IntrospectResponse> buildErrorResponse(String message) {
        return ApiResponse.<IntrospectResponse>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .result(IntrospectResponse.builder()
                        .valid(false)
                        .build())
                .timestamp(LocalDateTime.now())
                .build();
    }

    String generateToken(User user) throws JOSEException {
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer(ISSUER)
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(EXPIRATION, ChronoUnit.MINUTES).toEpochMilli()
                ))
                .claim("scope", buildScope(user))
                .jwtID(UUID.randomUUID().toString())
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);
        jwsObject.sign(new MACSigner(SINGER_KEY.getBytes()));
        return jwsObject.serialize();
    }

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRoles())) {
            user.getRoles().forEach(role -> {
                stringJoiner.add("ROLE_" + role.getName());
                if (!CollectionUtils.isEmpty(role.getPermissions())) {
                    role.getPermissions().forEach(permission -> stringJoiner.add(permission.getName()));
                }
            });
        }
        return stringJoiner.toString();
    }
}