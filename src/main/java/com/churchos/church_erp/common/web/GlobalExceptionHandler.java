package com.churchos.church_erp.common.web;

import com.churchos.church_erp.common.exception.InvalidCredentialsException;
import com.churchos.church_erp.tenant.exception.InvalidTenantSlugException;
import com.churchos.church_erp.tenant.exception.TenantAlreadyExistsException;
import com.churchos.church_erp.tenant.exception.TenantNotFoundException;
import com.churchos.church_erp.tenant.exception.TenantProvisioningException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleTenantNotFound(TenantNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTenantSlugException.class)
    public ResponseEntity<Map<String, String>> handleInvalidTenantSlug(InvalidTenantSlugException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(TenantAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleTenantAlreadyExists(TenantAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(TenantProvisioningException.class)
    public ResponseEntity<Map<String, String>> handleTenantProvisioningFailure(TenantProvisioningException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
    }
}
