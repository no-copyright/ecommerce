package com.hau.identity_service.service;

import com.hau.identity_service.dto.response.IntrospectResponse;
import com.hau.identity_service.entity.User;
import com.hau.identity_service.repository.InvalidatedTokenRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Service
public class TokenService {
    @Value("${jwt.signerKey}")
    private String SINGER_KEY;

    @Value("${jwt.issuer}")
    private String ISSUER;

    @Value("${jwt.expirationMinutes}")
    private Long EXPIRATION;

    private final InvalidatedTokenRepository invalidatedTokenRepository;

    public TokenService(InvalidatedTokenRepository invalidatedTokenRepository) {
        this.invalidatedTokenRepository = invalidatedTokenRepository;
    }

    // Chuyển các phương thức xác thực từ AuthenticationService sang đây
    public IntrospectResponse validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return IntrospectResponse.builder().valid(false).build();
        }

        try {
            // Logic xác thực token từ phương thức verifyToken của AuthenticationService
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
