package com.hau.identity_service.exception;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.hau.identity_service.dto.response.ErrorsResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // Xử lý ngoại lệ validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorsResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errorDetails = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            Map<String, String> detail = new HashMap<>();
            detail.put("field", error.getField());
            detail.put("message", error.getDefaultMessage());
            errorDetails.add(detail);
        });
        ErrorsResponse errorResponse = new ErrorsResponse(
                HttpStatus.BAD_REQUEST.value(), "Lỗi trường dữ liệu", errorDetails, LocalDateTime.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Xử lý ngoại lệ DataIntegrityViolationException
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorsResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        List<Map<String, String>> errorDetails = new ArrayList<>();
        Map<String, String> detail = new HashMap<>();
        detail.put("error", ex.getMessage());
        errorDetails.add(detail);
        ErrorsResponse errorResponse = new ErrorsResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Vi phạm tính toàn vẹn dữ liệu. Xem 'error' để biết chi tiết.",
                errorDetails,
                LocalDateTime.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Xử lý ngoại lệ tham số không đúng định dạng trong url
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorsResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String name = ex.getName();
        ErrorsResponse errorResponse = new ErrorsResponse(
                HttpStatus.BAD_REQUEST.value(), "Tham số " + name + " không đúng định dạng", null, LocalDateTime.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Xử lý lỗi truyền sai định dạng tham số trong body request
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorsResponse> handleHttpMessageNotReadableException() {
        ErrorsResponse errorResponse = new ErrorsResponse(
                HttpStatus.BAD_REQUEST.value(), "Dữ liệu request không đúng định dạng", null, LocalDateTime.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Xử lý lỗi khi không tìm thấy tài nguyên
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorsResponse> handleAppException(AppException ex) {
        ErrorsResponse errorResponse =
                new ErrorsResponse(ex.getHttpStatus().value(), ex.getMessage(), ex.getError(), LocalDateTime.now());
        return new ResponseEntity<>(errorResponse, ex.getHttpStatus()); // Trả về HttpStatus từ AppException
    }

    // Xử lý lỗi không có quyền truy cập
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorsResponse> handleAccessDeniedException() {
        ErrorsResponse errorResponse = new ErrorsResponse(
                HttpStatus.FORBIDDEN.value(), "Không có quyền truy cập tài nguyên này", null, LocalDateTime.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    // Xử lý tất cả các ngoại lệ chưa được xác định
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorsResponse> handleAllExceptions() {
        ErrorsResponse errorResponse = new ErrorsResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "Đã có lỗi xảy ra(chưa xác định)", null, LocalDateTime.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
