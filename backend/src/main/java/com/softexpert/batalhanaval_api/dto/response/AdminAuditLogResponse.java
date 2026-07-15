package com.softexpert.batalhanaval_api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Entrada de log de auditoria administrativa")
public record AdminAuditLogResponse(
    @Schema(description = "ID do registro") UUID id,
    @Schema(description = "ID do administrador que executou a ação") UUID adminId,
    @Schema(description = "Nome do administrador", example = "admin") String adminUsername,
    @Schema(description = "Ação realizada", example = "BAN") String action,
    @Schema(description = "Tipo do alvo da ação", example = "USER") String targetType,
    @Schema(description = "ID do alvo") UUID targetId,
    @Schema(description = "Detalhes adicionais") String details,
    @Schema(description = "Data/hora da ação") Instant createdAt
) {}
