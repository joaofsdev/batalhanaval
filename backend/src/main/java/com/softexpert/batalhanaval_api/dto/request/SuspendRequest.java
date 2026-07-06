package com.softexpert.batalhanaval_api.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record SuspendRequest(
    @NotNull(message = "suspendedUntil is required")
    @Future(message = "suspendedUntil must be in the future")
    Instant suspendedUntil
) {}
