package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.UserRole;
import com.softexpert.batalhanaval_api.domain.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
    UUID id,
    String username,
    String email,
    UserRole role,
    UserStatus status,
    Instant suspendedUntil,
    Instant createdAt
) {}
