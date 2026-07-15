package com.softexpert.batalhanaval_api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Resumo de um jogador em uma partida")
public record PlayerSummary(
    @Schema(description = "ID do jogador", example = "550e8400-e29b-41d4-a716-446655440000") UUID id,
    @Schema(description = "Nome de usuário", example = "jogador1") String username
) {}
