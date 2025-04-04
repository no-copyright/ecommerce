package com.hau.identity_service.service;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.hau.identity_service.dto.request.RefreshTokenRequest;
import com.hau.identity_service.dto.response.ApiResponse;
import com.hau.identity_service.dto.response.AuthenticationResponse;
import com.hau.identity_service.dto.response.IntrospectResponse;
import com.hau.identity_service.entity.InvalidatedToken;
import com.hau.identity_service.entity.User;
import com.hau.identity_service.exception.AppException;
import com.hau.identity_service.repository.InvalidatedTokenRepository;
import com.hau.identity_service.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {

    @Value("${jwt.signerKey}")
    private String signerKey;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expirationMinutes}")
    private Long expiration;

    @Value("${jwt.expirationRefreshMinutes}")
    private Long expirationRefresh;

    private final InvalidatedTokenRepository invalidatedTokenRepository;
    private final UserRepository userRepository;

    private JWTClaimsSet validateTokenClaims(String token, boolean isRefresh) throws AppException {
        try {
            JWSVerifier verifier = new MACVerifier(signerKey.getBytes());
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            if (!signedJWT.verify(verifier)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Chữ ký token không hợp lệ", null);
            }

            Date expirationTime = (isRefresh)
                    ? new Date(claimsSet
                            .getIssueTime()
                            .toInstant()
                            .plus(expirationRefresh, ChronoUnit.MINUTES)
                            .toEpochMilli())
                    : claimsSet.getExpirationTime();

            if (expirationTime == null || expirationTime.before(new Date())) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Token đã hết hạn", null);
            }

            if (claimsSet.getIssuer() == null || !claimsSet.getIssuer().equals(issuer)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Token issuer không hợp lệ", null);
            }

            if (claimsSet.getIssueTime() != null && claimsSet.getIssueTime().after(new Date())) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Token chưa có hiệu lực", null);
            }

            if (invalidatedTokenRepository.existsById(claimsSet.getJWTID())) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Token đã hết hiệu lực", null);
            }

            return claimsSet;

        } catch (ParseException | JOSEException e) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Token không hợp lệ", null);
        }
    }

    public SignedJWT verifyToken(String token, boolean isRefresh) throws AppException {
        try {
            validateTokenClaims(token, isRefresh);
            return SignedJWT.parse(token);
        } catch (ParseException e) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Token không hợp lệ", null);
        }
    }

    public IntrospectResponse validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return IntrospectResponse.builder().valid(false).build(); // Use the builder directly
        }
        try {
            JWTClaimsSet claimsSet = validateTokenClaims(token, false);
            return IntrospectResponse.builder()
                    .valid(true)
                    .username(claimsSet.getSubject())
                    .build();
        } catch (AppException e) {
            return IntrospectResponse.builder().valid(false).build(); // Consistent error handling
        }
    }

    public String generateToken(User user) throws JOSEException {
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer(issuer)
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(expiration, ChronoUnit.MINUTES).toEpochMilli()))
                .claim("scope", buildScope(user))
                .jwtID(UUID.randomUUID().toString())
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);
        jwsObject.sign(new MACSigner(signerKey.getBytes()));
        return jwsObject.serialize();
    }

    public ApiResponse<AuthenticationResponse> refreshToken(RefreshTokenRequest refreshTokenRequest)
            throws JOSEException, ParseException {
        SignedJWT signedJWT = verifyToken(refreshTokenRequest.getToken(), true);

        String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        invalidatedTokenRepository.save(
                InvalidatedToken.builder().id(jwtId).expiryDate(expiryTime).build());

        String username = signedJWT.getJWTClaimsSet().getSubject();
        log.info("Refresh token for user: {}", username);
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Người dùng không tồn tại", null));

        String token = generateToken(user);
        log.info("Refresh token: {}", token);

        return ApiResponse.<AuthenticationResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Làm mới token thành công")
                .result(AuthenticationResponse.builder()
                        .authenticated(true)
                        .token(token)
                        .build())
                .timestamp(LocalDateTime.now())
                .build();
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

    @Scheduled(fixedRate = 1440, timeUnit = TimeUnit.MINUTES)
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        Date date = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        List<InvalidatedToken> expiredTokens = invalidatedTokenRepository.findByExpiryDateLessThanEqual(date);
        invalidatedTokenRepository.deleteAll(expiredTokens);
        log.info("Cleaned up {} expired tokens.", expiredTokens.size()); // Use SLF4J
    }
}
