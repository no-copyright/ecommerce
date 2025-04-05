package com.hau.identity_service.service;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import com.hau.identity_service.dto.request.ForgotPasswordRequest;
import com.hau.identity_service.dto.request.ResetPasswordWithTokenRequest;
import com.hau.identity_service.dto.request.VerifyOtpRequest;
import com.hau.identity_service.dto.response.ApiResponse;
import com.hau.identity_service.dto.response.VerifyOtpResponse;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${jwt.signerKey}")
    private String signerKey;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.passwordResetTokenExpiryMinutes}")
    private long passwordResetTokenExpiryMinutes;

    @Value("${jwt.otpExpiryMinutes}")
    private long otpExpiryMinutes;

    @Value("${jwt.otpRequestCooldownMinutes}")
    private long otpRequestCooldownMinutes;

    private static final Random random = new Random();
    private static final String PURPOSE_CLAIM = "purpose";
    private static final String PURPOSE_VALUE = "PASSWORD_RESET";

    private final Map<String, String> otpCache = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> otpExpiryCache = new ConcurrentHashMap<>();

    private final Map<String, LocalDateTime> otpRequestTimestamps = new ConcurrentHashMap<>();

    private final Map<String, LocalDateTime> usedResetTokens = new ConcurrentHashMap<>();

    public ApiResponse<String> sendOtp(ForgotPasswordRequest forgotPasswordRequest) {
        String username = forgotPasswordRequest.getUsername();

        // --- Rate Limiting Check ---
        LocalDateTime lastRequestTime = otpRequestTimestamps.get(username);
        LocalDateTime now = LocalDateTime.now();
        if (lastRequestTime != null) {
            Duration timeSinceLastRequest = Duration.between(lastRequestTime, now);
            if (timeSinceLastRequest.toMinutes() < otpRequestCooldownMinutes) {
                long waitMinutes = otpRequestCooldownMinutes - timeSinceLastRequest.toMinutes();
                log.warn("Rate limit hit for OTP request by user: {}. Wait {} more minute(s).", username, waitMinutes);
                return ApiResponse.<String>builder()
                        .status(HttpStatus.TOO_MANY_REQUESTS.value())
                        .message("Bạn vừa yêu cầu OTP gần đây. Vui lòng đợi " + waitMinutes
                                + " phút nữa trước khi thử lại.")
                        .result(null)
                        .timestamp(now)
                        .build();
            }
        }
        // --- End Rate Limiting Check ---

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy người dùng với username: " + username, null));

        String otp = generateOtp();
        otpCache.put(username, otp);
        otpExpiryCache.put(username, now.plusMinutes(otpExpiryMinutes));

        log.info("Generated OTP for user {}. Triggering async email.", username);

        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("otp", otp);
        context.setVariable("expiryMinutes", otpExpiryMinutes);

        String emailSubject = "Mã OTP xác thực quên mật khẩu";
        String templateName = "otp-email-template";

        emailService.sendHtmlEmail(user.getEmail(), emailSubject, templateName, context);

        // Update the timestamp *after* successfully processing the request
        otpRequestTimestamps.put(username, now);

        return ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Yêu cầu gửi OTP đã được xử lý. Vui lòng kiểm tra email của bạn.")
                .result(null)
                .timestamp(now)
                .build();
    }

    public ApiResponse<VerifyOtpResponse> verifyOtp(VerifyOtpRequest verifyOtpRequest) {
        // Extract username from the request body DTO
        String username = verifyOtpRequest.getUsername();
        int otp = verifyOtpRequest.getOtp(); // Get OTP from request

        String cachedOtp = otpCache.get(username);
        LocalDateTime expiryTime = otpExpiryCache.get(username);
        LocalDateTime now = LocalDateTime.now();

        // --- Logic using the extracted username ---
        if (cachedOtp == null) {
            log.warn("Verify OTP attempt for user '{}' failed: No OTP found in cache.", username);
            return buildVerifyOtpErrorResponse("OTP không hợp lệ hoặc chưa được yêu cầu. Vui lòng thử lại.");
        }

        if (expiryTime != null && now.isAfter(expiryTime)) {
            log.warn("Verify OTP attempt for user '{}' failed: OTP expired.", username);
            otpCache.remove(username);
            otpExpiryCache.remove(username);
            return buildVerifyOtpErrorResponse("OTP đã hết hạn. Vui lòng yêu cầu OTP mới.");
        }

        if (!cachedOtp.equals(String.valueOf(otp))) {
            log.warn("Verify OTP attempt for user '{}' failed: Incorrect OTP.", username);
            // Consider adding rate limiting for failed attempts here
            return buildVerifyOtpErrorResponse("OTP không chính xác. Vui lòng thử lại.");
        }
        // --- End Logic ---

        // OTP is correct, remove it from cache
        otpCache.remove(username);
        otpExpiryCache.remove(username);
        log.info("OTP successfully verified for user '{}'.", username);

        // Generate Password Reset JWT
        String passwordResetToken;
        try {
            passwordResetToken = generatePasswordResetToken(username); // Pass username to token generation
            log.info("Generated password reset JWT for user {}", username);
        } catch (JOSEException e) {
            log.error("Error generating password reset JWT for user {}: {}", username, e.getMessage());
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi tạo mã xác nhận đặt lại mật khẩu", e);
        }

        return ApiResponse.<VerifyOtpResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Xác thực OTP thành công. Mã xác nhận đặt lại mật khẩu đã được tạo.")
                .result(VerifyOtpResponse.builder()
                        .verificationToken(passwordResetToken)
                        .build())
                .timestamp(now)
                .build();
    }

    public ApiResponse<String> resetPassword(ResetPasswordWithTokenRequest resetPasswordWithTokenRequest) {
        String verificationToken = resetPasswordWithTokenRequest.getVerificationToken();
        String username;
        String jti; // JWT ID
        Date tokenExpiry;

        JWTClaimsSet claimsSet;
        try {
            claimsSet = validatePasswordResetToken(verificationToken); // Validation now includes blacklist check
            username = claimsSet.getSubject();
            jti = claimsSet.getJWTID(); // Get JTI
            tokenExpiry = claimsSet.getExpirationTime(); // Get expiry for blacklist

            if (username == null || username.trim().isEmpty() || jti == null || tokenExpiry == null) {
                log.error("Validated password reset token is missing critical claims (sub, jti, or exp).");
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi xử lý mã xác nhận.", null);
            }
        } catch (AppException e) {
            return ApiResponse.<String>builder()
                    .status(e.getHttpStatus().value())
                    .message(e.getMessage())
                    .result(null)
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error processing password reset token: {}", e.getMessage(), e);
            return ApiResponse.<String>builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Lỗi không xác định khi xử lý mã đặt lại mật khẩu.")
                    .result(null)
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        User user = userRepository.findByUsername(username).orElseThrow(() -> {
            log.error("User '{}' not found after successful token validation (JTI: {}).", username, jti);
            return new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi không mong đợi: Không tìm thấy người dùng.", null);
        });

        user.setPassword(passwordEncoder.encode(resetPasswordWithTokenRequest.getNewPassword()));
        userRepository.save(user);

        // --- Add token JTI to blacklist after successful password reset ---
        // Store with original expiry time to allow potential cleanup later
        usedResetTokens.put(
                jti,
                tokenExpiry.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        log.info("Password successfully reset for user {}. Token JTI {} blacklisted.", username, jti);
        // Note: In a production/scaled environment, consider a more robust blacklist (e.g., Redis with TTL)
        // and a cleanup mechanism for the in-memory map if used long-term.
        // --- End blacklist update ---

        return ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Mật khẩu đã được đặt lại thành công.")
                .result(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // --- Helper Methods ---

    private String generatePasswordResetToken(String username) throws JOSEException {
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(username)
                .issuer(issuer)
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now()
                        .plus(passwordResetTokenExpiryMinutes, ChronoUnit.MINUTES)
                        .toEpochMilli()))
                .claim(PURPOSE_CLAIM, PURPOSE_VALUE)
                .jwtID(UUID.randomUUID().toString()) // Ensure unique JTI is generated
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);
        jwsObject.sign(new MACSigner(signerKey.getBytes()));
        return jwsObject.serialize();
    }

    private JWTClaimsSet validatePasswordResetToken(String token) throws ParseException, JOSEException, AppException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWSVerifier verifier = new MACVerifier(signerKey.getBytes());

        boolean signatureValid = signedJWT.verify(verifier);
        if (!signatureValid) {
            log.warn("Invalid password reset token signature received.");
            throw new AppException(HttpStatus.BAD_REQUEST, "Mã xác nhận không hợp lệ (chữ ký sai).", null);
        }

        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
        String jti = claimsSet.getJWTID();
        String tokenUsername = claimsSet.getSubject(); // Get username for logging context

        // --- Check Blacklist (Single-Use) ---
        if (jti != null && usedResetTokens.containsKey(jti)) {
            log.warn("Attempt to reuse already used password reset token. User: {}, JTI: {}", tokenUsername, jti);
            // Consider 409 Conflict, but 400 is also common.
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Mã xác nhận đã được sử dụng. Vui lòng thực hiện lại quy trình quên mật khẩu.",
                    null);
        }
        // --- End Blacklist Check ---

        Date expirationTime = claimsSet.getExpirationTime();
        if (expirationTime == null || expirationTime.before(new Date())) {
            log.info("Expired password reset token used (User: {}, JTI: {}).", tokenUsername, jti);
            throw new AppException(HttpStatus.BAD_REQUEST, "Mã xác nhận đã hết hạn.", null);
        }

        String tokenIssuer = claimsSet.getIssuer();
        if (tokenIssuer == null || !tokenIssuer.equals(issuer)) {
            log.warn(
                    "Invalid issuer in password reset token. User: {}, Expected: {}, Found: {}. JTI: {}",
                    tokenUsername,
                    issuer,
                    tokenIssuer,
                    jti);
            throw new AppException(HttpStatus.BAD_REQUEST, "Mã xác nhận không hợp lệ (issuer sai).", null);
        }

        String purpose = claimsSet.getStringClaim(PURPOSE_CLAIM);
        if (!PURPOSE_VALUE.equals(purpose)) {
            log.warn(
                    "Incorrect purpose claim in token. User: {}, Expected: {}, Found: {}. JTI: {}",
                    tokenUsername,
                    PURPOSE_VALUE,
                    purpose,
                    jti);
            throw new AppException(HttpStatus.BAD_REQUEST, "Mã xác nhận không hợp lệ (mục đích sai).", null);
        }

        if (tokenUsername == null || tokenUsername.trim().isEmpty()) {
            log.warn("Password reset token is missing the subject (username) claim. JTI: {}", jti);
            throw new AppException(
                    HttpStatus.BAD_REQUEST, "Mã xác nhận không hợp lệ (thiếu thông tin người dùng).", null);
        }

        Date issueTime = claimsSet.getIssueTime();
        if (issueTime != null && issueTime.after(new Date())) {
            log.warn("Password reset token used before issue time for user {}. JTI: {}", tokenUsername, jti);
            throw new AppException(HttpStatus.BAD_REQUEST, "Mã xác nhận chưa có hiệu lực.", null);
        }

        if (jti == null) {
            // Should not happen if generation includes it, but check defensively.
            log.error("Password reset token is missing the JTI claim. User: {}", tokenUsername);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi xử lý mã xác nhận (thiếu JTI).", null);
        }

        log.info("Password reset token successfully validated for user {}", tokenUsername);
        return claimsSet;
    }

    private String generateOtp() {
        int otpValue = 100000 + random.nextInt(900000);
        return String.valueOf(otpValue);
    }

    private ApiResponse<VerifyOtpResponse> buildVerifyOtpErrorResponse(String message) {
        return ApiResponse.<VerifyOtpResponse>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .result(null)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
