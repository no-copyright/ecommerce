package com.hau.identity_service.controller;

import com.hau.identity_service.dto.request.*;
import com.hau.identity_service.dto.response.ApiResponse;
import com.hau.identity_service.dto.response.AuthenticationResponse;
import com.hau.identity_service.dto.response.IntrospectResponse;
import com.hau.identity_service.dto.response.VerifyOtpResponse;
import com.hau.identity_service.service.AuthenticationService;
import com.hau.identity_service.service.ForgotPasswordService;
import com.hau.identity_service.service.TokenService;
import com.nimbusds.jose.JOSEException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final ForgotPasswordService forgotPasswordService;
    private final TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> authenticate(@RequestBody AuthenticationRequest authenticationRequest) {
        ApiResponse<AuthenticationResponse> apiResponse = authenticationService.authenticate(authenticationRequest);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody LogoutRequest logoutRequest) throws ParseException, JOSEException {
        ApiResponse<Void> apiResponse = authenticationService.logout(logoutRequest);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) throws ParseException, JOSEException {
        ApiResponse<AuthenticationResponse> apiResponse = tokenService.refreshToken(refreshTokenRequest);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/introspect")
    public ResponseEntity<ApiResponse<IntrospectResponse>> introspect(@RequestBody IntrospectRequest request) {
        ApiResponse<IntrospectResponse> apiResponse = authenticationService.introspect(request);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/password-recovery/otp")
    public ResponseEntity<ApiResponse<String>> sendOtpForgotPassword(@RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        ApiResponse<String> apiResponse = forgotPasswordService.sendOtp(forgotPasswordRequest);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/password-recovery/otp/verify") // Removed /{username}
    public ResponseEntity<ApiResponse<VerifyOtpResponse>> verifyOtpForgotPassword(
            // Removed @PathVariable String username
            @Valid @RequestBody VerifyOtpRequest verifyOtpRequest) { // Ensure validation
        // Updated service call
        ApiResponse<VerifyOtpResponse> apiResponse = forgotPasswordService.verifyOtp(verifyOtpRequest);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/password-recovery/reset")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @Valid @RequestBody ResetPasswordWithTokenRequest resetPasswordWithTokenRequest) {
        ApiResponse<String> apiResponse = forgotPasswordService.resetPassword(resetPasswordWithTokenRequest);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}