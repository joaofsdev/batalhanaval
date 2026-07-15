package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.GameMode;
import com.softexpert.batalhanaval_api.domain.GameStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Dados de partida para painel administrativo")
public record AdminGameResponse(
    @Schema(description = "ID da partida") UUID id,
    @Schema(description = "Status da partida", example = "IN_PROGRESS") GameStatus status,
    @Schema(description = "Modo de jogo", example = "CLASSIC") GameMode gameMode,
    @Schema(description = "Jogador 1") PlayerSummary player1,
    @Schema(description = "Jogador 2") PlayerSummary player2,
    @Schema(description = "Data de criação") Instant createdAt,
    @Schema(description = "Última atualização") Instant updatedAt
) {}
