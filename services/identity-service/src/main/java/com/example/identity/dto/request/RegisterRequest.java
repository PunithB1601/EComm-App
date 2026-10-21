package com.example.identity.dto.request;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank @Size(max = 200) String fullName,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 12, max = 72) String password) {}
