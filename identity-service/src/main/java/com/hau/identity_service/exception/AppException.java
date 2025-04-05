package com.hau.identity_service.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AppException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final transient Object error;
    private final LocalDateTime timestamp;

    public AppException(HttpStatus httpStatus, String message, Object error) {
        super(message);
        this.httpStatus = httpStatus;
        this.error = error;
        this.timestamp = LocalDateTime.now();
    }
}
