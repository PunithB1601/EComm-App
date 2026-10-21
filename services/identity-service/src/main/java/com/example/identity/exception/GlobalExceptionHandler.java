package com.example.identity.exception;

import com.example.identity.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApplicationException.class)
  ResponseEntity<?> application(ApplicationException e) {
    return error(e.getStatus(), e.getMessage(), null);
  }

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<?> authentication(AuthenticationException e) {
    return error(HttpStatus.UNAUTHORIZED, "Invalid credentials or unavailable account", null);
  }

  @ExceptionHandler(JwtException.class)
  ResponseEntity<?> jwt(JwtException e) {
    return error(HttpStatus.UNAUTHORIZED, "Invalid or expired access token", null);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<?> forbidden(AccessDeniedException e) {
    return error(HttpStatus.FORBIDDEN, "Access denied", null);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<?> validation(MethodArgumentNotValidException e) {
    Map<String, String> fields = new TreeMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(f -> fields.put(f.getField(), f.getDefaultMessage()));
    return error(HttpStatus.BAD_REQUEST, "Validation failed", fields);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    ConstraintViolationException.class,
    org.springframework.web.method.annotation.HandlerMethodValidationException.class
  })
  ResponseEntity<?> malformed(Exception e) {
    return error(HttpStatus.BAD_REQUEST, "Invalid request", null);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<?> conflict(DataIntegrityViolationException e) {
    return error(HttpStatus.CONFLICT, "Request conflicts with an existing record", null);
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  ResponseEntity<?> method(Exception e) {
    return error(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", null);
  }

  @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
  ResponseEntity<?> media(Exception e) {
    return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type", null);
  }

  @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
  ResponseEntity<?> missing(Exception e) {
    return error(HttpStatus.NOT_FOUND, "Resource not found", null);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> unexpected(Exception e) {
    // Do not log exception messages or request bodies: they may contain sensitive data.
    log.error("Unhandled request failure of type {}", e.getClass().getName());
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null);
  }

  private ResponseEntity<?> error(HttpStatus status, String message, Object data) {
    return ResponseEntity.status(status).body(ApiResponse.of(message, data, status));
  }
}
