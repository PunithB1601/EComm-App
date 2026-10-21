package com.example.identity.dto.response;

import com.example.identity.enums.AccountStatus;
import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
    Long userId,
    String fullName,
    String email,
    AccountStatus accountStatus,
    Set<String> roles,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
