package com.example.identity.controller;

import com.example.identity.dto.request.UpdateStatusRequest;
import com.example.identity.dto.response.*;
import com.example.identity.security.IdentityPrincipal;
import com.example.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {
  private final UserService users;

  @GetMapping("/me")
  @Operation(summary = "Get the authenticated account")
  public ApiResponse<UserResponse> me(@AuthenticationPrincipal IdentityPrincipal principal) {
    return ApiResponse.of(
        "User retrieved", users.getCurrentUser(principal.userId()), HttpStatus.OK);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get an account; owner or administrator")
  public ApiResponse<UserResponse> get(@PathVariable @Positive Long id) {
    return ApiResponse.of("User retrieved", users.getUserById(id), HttpStatus.OK);
  }

  @GetMapping
  @Operation(summary = "List accounts; administrator only")
  public ApiResponse<UserPageResponse> all(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return ApiResponse.of("Users retrieved", users.getAllUsers(page, size), HttpStatus.OK);
  }

  @PatchMapping("/{id}/status")
  @Operation(
      summary = "Change account status and revoke sessions when unavailable; administrator only")
  public ApiResponse<UserResponse> status(
      @PathVariable @Positive Long id, @Valid @RequestBody UpdateStatusRequest request) {
    return ApiResponse.of(
        "Account status updated", users.updateAccountStatus(id, request.status()), HttpStatus.OK);
  }
}
