package com.example.identity.dto.request;

import jakarta.validation.constraints.*;

public record RefreshTokenRequest(@NotBlank @Size(min = 43, max = 128) String refreshToken) {}
