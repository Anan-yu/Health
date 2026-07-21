package com.rayk.health.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.rayk.health.common.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsSpringSecurityAccessDeniedToForbiddenApiResponse() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.AUTH_FORBIDDEN.code());
        assertThat(response.getBody().message()).isEqualTo(ErrorCode.AUTH_FORBIDDEN.message());
    }

    @Test
    void mapsMalformedJsonToValidationError() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleValidation(new HttpMessageNotReadableException("bad body"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.SYSTEM_VALIDATION_ERROR.code());
    }

    @Test
    void mapsInvalidCredentialsToUnauthorized() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBusiness(new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS.code());
    }

    @Test
    void mapsDisallowedWorkbenchToForbidden() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBusiness(new BusinessException(ErrorCode.WORKBENCH_NOT_ALLOWED));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.WORKBENCH_NOT_ALLOWED.code());
    }
}
