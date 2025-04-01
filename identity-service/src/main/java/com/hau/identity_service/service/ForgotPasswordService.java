package com.hau.identity_service.service;

import com.hau.identity_service.dto.*;
import com.hau.identity_service.entity.User;
import com.hau.identity_service.exception.AppException;
import com.hau.identity_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordService {
    private final UserRepository userRepository;
    private final JavaMailSender javaMailSender;
    private final PasswordEncoder passwordEncoder;
    private final TemplateEngine templateEngine;

    private final Map<String, String> otpCache = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> otpExpiryCache = new ConcurrentHashMap<>();
    private final Map<String, VerificationTokenData> verificationTokenCache = new ConcurrentHashMap<>();

    private static final long OTP_EXPIRY_MINUTES = 5;
    private static final long VERIFICATION_TOKEN_EXPIRY_MINUTES = 15;


    public ApiResponse<String> sendOtp(ForgotPasswordRequest forgotPasswordRequest) {
        String username = forgotPasswordRequest.getUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng với username: " + username, null));

        String otp = generateOtp();
        otpCache.put(username, otp);
        otpExpiryCache.put(username, LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        verificationTokenCache.remove(username);

        try {
            sendOtpEmail(user.getEmail(), otp, OTP_EXPIRY_MINUTES); // Truyền thêm expiryMinutes
        } catch (MessagingException e) {
            log.error("Lỗi gửi email OTP: ", e);
            otpCache.remove(username);
            otpExpiryCache.remove(username);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi gửi email OTP", e.getMessage());
        }

        return ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư đến.")
                .result(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public ApiResponse<VerifyOtpResponse> verifyOtp(String username, VerifyOtpRequest verifyOtpRequest) {
        String cachedOtp = otpCache.get(username);
        LocalDateTime expiryTime = otpExpiryCache.get(username);
        LocalDateTime now = LocalDateTime.now();

        if (cachedOtp == null) {
            return ApiResponse.<VerifyOtpResponse>builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("OTP không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.")
                    .result(null)
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        if (expiryTime != null && now.isAfter(expiryTime)) {
            otpCache.remove(username);
            otpExpiryCache.remove(username);
            return ApiResponse.<VerifyOtpResponse>builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("OTP đã hết hạn. Vui lòng yêu cầu OTP mới.")
                    .result(null)
                    .timestamp(LocalDateTime.now())
                    .build();
        }


        if (!cachedOtp.equals(String.valueOf(verifyOtpRequest.getOtp()))) {
            return ApiResponse.<VerifyOtpResponse>builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("OTP không chính xác. Vui lòng thử lại.")
                    .result(null)
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        otpCache.remove(username);
        otpExpiryCache.remove(username);

        // Generate Verification Token
        String verificationToken = UUID.randomUUID().toString();
        LocalDateTime tokenExpiryTime = LocalDateTime.now().plusMinutes(VERIFICATION_TOKEN_EXPIRY_MINUTES);
        verificationTokenCache.put(username, new VerificationTokenData(verificationToken, tokenExpiryTime));


        return ApiResponse.<VerifyOtpResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Xác thực OTP thành công. Mã xác nhận đã được tạo.")
                .result(VerifyOtpResponse.builder()
                        .verificationToken(verificationToken)
                        .build())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public ApiResponse<String> resetPassword(String username, ResetPasswordWithTokenRequest resetPasswordWithTokenRequest) {
        String verificationToken = resetPasswordWithTokenRequest.getVerificationToken();
        VerificationTokenData tokenData = verificationTokenCache.get(username);


        if (tokenData == null || !tokenData.getToken().equals(verificationToken)) {
            return ApiResponse.<String>builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Mã xác nhận không hợp lệ hoặc đã hết hạn. Vui lòng xác thực OTP lại.")
                    .result(null)
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        if (tokenData.getExpiryTime().isBefore(LocalDateTime.now())) {
            verificationTokenCache.remove(username);
            return ApiResponse.<String>builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Mã xác nhận đã hết hạn. Vui lòng xác thực OTP lại.")
                    .result(null)
                    .timestamp(LocalDateTime.now())
                    .build();
        }


        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng với username: " + username, null));

        user.setPassword(passwordEncoder.encode(resetPasswordWithTokenRequest.getNewPassword()));
        userRepository.save(user);
        verificationTokenCache.remove(username);

        return ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Mật khẩu đã được đặt lại thành công.")
                .result(null)
                .timestamp(LocalDateTime.now())
                .build();
    }


    private String generateOtp() {
        Random random = new Random();
        int otpValue = 100000 + random.nextInt(900000);
        return String.valueOf(otpValue);
    }

    private void sendOtpEmail(String toEmail, String otp, long expiryMinutes) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8"); // Use true for multipart, UTF-8 encoding

        try {
            helper.setTo(toEmail);
            helper.setSubject("Mã OTP xác thực quên mật khẩu");

            // 1. Create Thymeleaf Context
            Context context = new Context();
            context.setVariable("otp", otp); // Variable name matches th:text="${otp}"
            context.setVariable("expiryMinutes", expiryMinutes); // Variable name matches [[${expiryMinutes}]]

            // 2. Process the template using TemplateEngine
            String emailContent = templateEngine.process("otp-email-template", context); // Use template name (without .html)

            // 3. Set the processed HTML content
            helper.setText(emailContent, true); // true indicates HTML content

            javaMailSender.send(mimeMessage);
            log.info("OTP email sent successfully to {}", toEmail);

        } catch (MessagingException e) {
            log.error("Error creating or sending OTP email to {}: {}", toEmail, e.getMessage());
            throw e; // Re-throw to be handled by the caller if necessary
        }
    }
}