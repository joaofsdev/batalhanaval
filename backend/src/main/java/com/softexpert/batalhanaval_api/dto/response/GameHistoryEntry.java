package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.GameStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Entrada no histórico de partidas")
public record GameHistoryEntry(
    @Schema(description = "ID da partida") UUID id,
    @Schema(description = "Nome do oponente", example = "jogador2") String opponentUsername,
    @Schema(description = "Status final da partida", example = "FINISHED") GameStatus status,
    @Schema(description = "Se o jogador venceu", example = "true") boolean won,
    @Schema(description = "Duração da partida em segundos", example = "320") long durationSeconds,
    @Schema(description = "Data/hora da partida") Instant playedAt
) {}
