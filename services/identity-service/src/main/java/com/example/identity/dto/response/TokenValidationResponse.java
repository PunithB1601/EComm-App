package com.example.identity.dto.response;

import java.time.Instant;
import java.util.List;

public record TokenValidationResponse(
    boolean valid, String subject, List<String> roles, Instant expiresAt) {}
