package com.example.identity.dto.request;

import jakarta.validation.constraints.*;

public record ValidateTokenRequest(@NotBlank @Size(max = 8192) String token) {}
