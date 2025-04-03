package com.hau.identity_service.service;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {
    @Value("${jwt.signerKey}")
    private String SINGER_KEY;

    @Value("${jwt.issuer}")
    private String ISSUER;

    @Value("${jwt.expirationMinutes}")
    private Long EXPIRATION;

    private final InvalidatedTokenRepository invalidatedTokenRepository;
    private final UserRepository userRepository;


    public IntrospectResponse validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return IntrospectResponse.builder().valid(false).build();
        }

        try {
            JWSVerifier verifier = new MACVerifier(SINGER_KEY.getBytes());
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            // Kiểm tra chữ ký
            if (!signedJWT.verify(verifier)) {
                return IntrospectResponse.builder().valid(false).build();
            }

            // Kiểm tra thời hạn
            Date expirationTime = claimsSet.getExpirationTime();
            if (expirationTime == null || expirationTime.before(new Date())) {
                return IntrospectResponse.builder().valid(false).build();
            }

            // Kiểm tra issuer
            String tokenIssuer = claimsSet.getIssuer();
            if (tokenIssuer == null || !tokenIssuer.equals(ISSUER)) {
                return IntrospectResponse.builder().valid(false).build();
            }

            // Kiểm tra thời gian phát hành
            Date issueTime = claimsSet.getIssueTime();
            if (issueTime != null && issueTime.after(new Date())) {
                return IntrospectResponse.builder().valid(false).build();
            }

            // Kiểm tra token đã bị vô hiệu hóa chưa
            if (invalidatedTokenRepository.existsById(claimsSet.getJWTID())) {
                return IntrospectResponse.builder().valid(false).build();
            }

            return IntrospectResponse.builder()
                    .valid(true)
                    .username(claimsSet.getSubject())
                    .build();
        } catch (Exception e) {
            return IntrospectResponse.builder().valid(false).build();
        }
    }

    public String generateToken(User user) throws JOSEException {
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

    public ApiResponse<AuthenticationResponse> refreshToken(RefreshTokenRequest refreshTokenRequest) throws JOSEException, ParseException {
        var signedJWT = verifyToken(refreshTokenRequest.getToken());

        var jit = signedJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jit)
                .expiryDate(expiryTime)
                .build();
        invalidatedTokenRepository.save(invalidatedToken);
        var username = signedJWT.getJWTClaimsSet().getSubject();
        log.info("Refresh token for user: {}", username);
        var user = userRepository.findByUsername(username)
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

    public SignedJWT verifyToken(String token) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SINGER_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

        boolean signatureValid = signedJWT.verify(verifier);
        if (!signatureValid) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Chữ ký token không hợp lệ", null);
        }

        Date expirationTime = claimsSet.getExpirationTime();
        if (expirationTime == null || expirationTime.before(new Date())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Token đã hết hạn", null);
        }

        String tokenIssuer = claimsSet.getIssuer();
        if (tokenIssuer == null || !tokenIssuer.equals(ISSUER)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Token issuer không hợp lệ", null);
        }
        Date issueTime = claimsSet.getIssueTime();
        if (issueTime != null && issueTime.after(new Date())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Token chưa có hiệu lực", null);
        }

        if (invalidatedTokenRepository.existsById(claimsSet.getJWTID())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Token đã hết hiệu lực", null);
        }

        return signedJWT;
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
        System.out.println("Starting token cleanup at: " + now);
        Date date = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        List<InvalidatedToken> expiredTokens = invalidatedTokenRepository.findByExpiryDateLessThanEqual(date);
        invalidatedTokenRepository.deleteAll(expiredTokens);
        System.out.println("Cleaned up " + expiredTokens.size() + " expired tokens.");
    }
}
