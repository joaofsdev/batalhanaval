package com.softexpert.batalhanaval_api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditLogResponse(
    UUID id,
    UUID adminId,
    String adminUsername,
    String action,
    String targetType,
    UUID targetId,
    String details,
    Instant createdAt
) {}
