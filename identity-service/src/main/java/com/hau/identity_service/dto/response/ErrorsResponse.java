package com.hau.identity_service.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorsResponse {
    int status;
    String message;
    Object error;
    LocalDateTime timestamp;
}
