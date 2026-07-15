package com.softexpert.batalhanaval_api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Entrada individual no ranking")
public record RankingEntry(
    @Schema(description = "Posição no ranking", example = "1") int position,
    @Schema(description = "ID do jogador") UUID userId,
    @Schema(description = "Nome de usuário", example = "jogador1") String username,
    @Schema(description = "Número de vitórias", example = "30") long wins,
    @Schema(description = "Total de partidas", example = "50") long totalGames,
    @Schema(description = "Taxa de vitória", example = "0.6") double winRate,
    @Schema(description = "Rating ELO", example = "1350") int eloRating
) {}
