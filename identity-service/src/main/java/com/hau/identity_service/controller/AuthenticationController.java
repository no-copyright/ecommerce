package com.hau.identity_service.controller;

import com.hau.identity_service.dto.*;
import com.hau.identity_service.service.AuthenticationService;
import com.hau.identity_service.service.ForgotPasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final ForgotPasswordService forgotPasswordService;

    @PostMapping("/login")
    private ResponseEntity<ApiResponse<AuthenticationResponse>> authenticate(@RequestBody AuthenticationRequest authenticationRequest) {
        ApiResponse<AuthenticationResponse> apiResponse = authenticationService.authenticate(authenticationRequest);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/introspect")
    private ResponseEntity<ApiResponse<IntrospectResponse>> introspect(@RequestBody IntrospectRequest request) {
        ApiResponse<IntrospectResponse> apiResponse = authenticationService.introspect(request);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/password-recovery/otp")
    public ResponseEntity<ApiResponse<String>> sendOtpForgotPassword(@RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        ApiResponse<String> apiResponse = forgotPasswordService.sendOtp(forgotPasswordRequest);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/password-recovery/otp/verify/{username}")
    public ResponseEntity<ApiResponse<VerifyOtpResponse>> verifyOtpForgotPassword( // Changed response type to VerifyOtpResponse
                                                                                   @PathVariable String username,
                                                                                   @RequestBody VerifyOtpRequest verifyOtpRequest) {
        ApiResponse<VerifyOtpResponse> apiResponse = forgotPasswordService.verifyOtp(username, verifyOtpRequest); // Changed response type
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/password-recovery/reset/{username}")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @PathVariable String username,
            @Valid @RequestBody ResetPasswordWithTokenRequest resetPasswordWithTokenRequest) { // Changed request type
        ApiResponse<String> apiResponse = forgotPasswordService.resetPassword(username, resetPasswordWithTokenRequest); // Changed request type
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}