package com.example.identity.dto.request;

import jakarta.validation.constraints.*;

public record UpdateStatusRequest(@NotNull com.example.identity.enums.AccountStatus status) {}
