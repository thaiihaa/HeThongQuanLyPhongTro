package com.quanlyphongtro.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException exception) {
        return ResponseEntity.status(exception.status())
                .body(new ErrorResponse(exception.code(), exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "VALIDATION_FAILED",
                "Thông tin gửi lên chưa hợp lệ. Vui lòng kiểm tra lại các trường.",
                Instant.now()
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "VALIDATION_FAILED",
                "Thông tin gửi lên chưa hợp lệ. Vui lòng kiểm tra lại các trường.",
                Instant.now()
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                "DATA_CONFLICT",
                "Dữ liệu bị trùng hoặc đang được tham chiếu nên không thể lưu thay đổi.",
                Instant.now()
        ));
    }

    public record ErrorResponse(String code, String message, Instant timestamp) {
    }
}
