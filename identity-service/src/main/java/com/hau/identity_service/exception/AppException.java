package com.hau.identity_service.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Setter
@Getter
public class AppException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final Object error;
    private final LocalDateTime timestamp;

    public AppException(HttpStatus httpStatus, String message, Object error) {
        super(message);
        this.httpStatus = httpStatus;
        this.error = error;
        this.timestamp = LocalDateTime.now();
    }
}
