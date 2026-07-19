package com.rayk.health.common.exception;

import com.rayk.health.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
        ErrorCode error = exception.getErrorCode();
        HttpStatus status = switch (error) {
            case AUTH_UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case AUTH_FORBIDDEN -> HttpStatus.FORBIDDEN;
            case FILE_STORAGE_UNAVAILABLE, AI_SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(ApiResponse.error(error.code(), error.message()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidation(Exception exception) {
        return ResponseEntity.badRequest()
                .body(
                        ApiResponse.error(
                                ErrorCode.SYSTEM_VALIDATION_ERROR.code(),
                                ErrorCode.SYSTEM_VALIDATION_ERROR.message()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception exception) {
        log.error("Unhandled server exception", exception);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ErrorCode.SYSTEM_ERROR.code(), ErrorCode.SYSTEM_ERROR.message()));
    }
}
