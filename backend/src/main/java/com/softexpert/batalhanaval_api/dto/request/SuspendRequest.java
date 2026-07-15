package com.softexpert.batalhanaval_api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(description = "Requisição para suspender um usuário temporariamente")
public record SuspendRequest(
    @NotNull(message = "suspendedUntil é obrigatório")
    @Future(message = "suspendedUntil deve ser uma data futura")
    @Schema(description = "Data/hora até quando o usuário ficará suspenso (ISO-8601)", example = "2025-12-31T23:59:59Z")
    Instant suspendedUntil
) {}
