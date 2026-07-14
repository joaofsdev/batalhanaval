package com.softexpert.batalhanaval_api.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record SuspendRequest(
    @NotNull(message = "suspendedUntil é obrigatório")
    @Future(message = "suspendedUntil deve ser uma data futura")
    Instant suspendedUntil
) {}
