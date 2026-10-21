package com.example.identity.controller;

import com.example.identity.dto.request.*;
import com.example.identity.dto.response.*;
import com.example.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService auth;

  @PostMapping("/register")
  @Operation(
      summary = "Register a customer; roles cannot be selected",
      security = {})
  public ResponseEntity<ApiResponse<AuthResponse>> register(
      @Valid @RequestBody RegisterRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResponse.of("Registration successful", auth.register(request), HttpStatus.CREATED));
  }

  @PostMapping("/login")
  @Operation(
      summary = "Authenticate with email and password",
      security = {})
  public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.of("Login successful", auth.login(request), HttpStatus.OK);
  }

  @PostMapping("/refresh")
  @Operation(
      summary = "Rotate refresh token; discard the previous token",
      security = {})
  public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return ApiResponse.of("Token refreshed", auth.refreshToken(request), HttpStatus.OK);
  }

  @PostMapping("/logout")
  @Operation(
      summary = "Revoke the refresh-token family; existing access tokens expire normally",
      security = {})
  public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
    auth.logout(request);
    return ApiResponse.of("Logged out", null, HttpStatus.OK);
  }

  @PostMapping("/validate")
  @Operation(
      summary = "Validate an access token; administrator only",
      security = @SecurityRequirement(name = "bearerAuth"))
  public ApiResponse<TokenValidationResponse> validate(
      @Valid @RequestBody ValidateTokenRequest request) {
    return ApiResponse.of("Token valid", auth.validateToken(request.token()), HttpStatus.OK);
  }
}
