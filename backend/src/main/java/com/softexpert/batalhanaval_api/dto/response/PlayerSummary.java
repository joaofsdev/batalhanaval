package com.softexpert.batalhanaval_api.dto.response;

import java.util.UUID;

public record PlayerSummary(
    UUID id,
    String username
) {}
