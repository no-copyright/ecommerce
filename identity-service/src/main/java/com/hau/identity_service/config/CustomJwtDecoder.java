package com.hau.identity_service.config;

import com.hau.identity_service.dto.request.IntrospectRequest;
import com.hau.identity_service.dto.response.ApiResponse;
import com.hau.identity_service.dto.response.IntrospectResponse;
import com.hau.identity_service.service.AuthenticationService;
import com.hau.identity_service.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CustomJwtDecoder implements JwtDecoder {
    @Value("${jwt.signerKey}")
    private String SIGNER_KEY;

    // Thay đổi từ AuthenticationService sang TokenService
    private final TokenService tokenService;

    private NimbusJwtDecoder nimbusJwtDecoder = null;

    @Override
    public Jwt decode(String token) throws JwtException {
        if (token == null || token.isEmpty()) {
            throw new JwtException("Token không được cung cấp");
        }

        try {
            // Gọi trực tiếp TokenService thay vì qua AuthenticationService
            IntrospectResponse response = tokenService.validateToken(token);

            // Kiểm tra kết quả
            if (!response.isValid()) {
                throw new JwtException("Token không hợp lệ");
            }

            // Tiếp tục xử lý token nếu hợp lệ
            if (Objects.isNull(nimbusJwtDecoder)) {
                SecretKeySpec secretKeySpec = new SecretKeySpec(SIGNER_KEY.getBytes(), "HS512");
                nimbusJwtDecoder = NimbusJwtDecoder
                        .withSecretKey(secretKeySpec)
                        .macAlgorithm(MacAlgorithm.HS512)
                        .build();
            }

            return nimbusJwtDecoder.decode(token);
        } catch (Exception e) {
            throw new JwtException(e.getMessage());
        }
    }
}
