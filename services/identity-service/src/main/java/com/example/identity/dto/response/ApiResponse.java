package com.example.identity.dto.response;

import org.springframework.http.HttpStatus;

public record ApiResponse<T>(String message, T data, String httpStatus) {
  public static <T> ApiResponse<T> of(String message, T data, HttpStatus status) {
    return new ApiResponse<>(message, data, status.name());
  }
}
