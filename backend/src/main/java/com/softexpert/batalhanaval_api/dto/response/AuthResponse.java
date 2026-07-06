package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.UserRole;

import java.util.UUID;

public record AuthResponse(
    UUID id,
    String username,
    String email,
    UserRole role,
    String token
) {}
