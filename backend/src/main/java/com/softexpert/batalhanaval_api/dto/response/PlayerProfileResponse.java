package com.softexpert.batalhanaval_api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Perfil completo de um jogador com estatísticas")
public record PlayerProfileResponse(
    @Schema(description = "ID do jogador") UUID id,
    @Schema(description = "Nome de usuário", example = "jogador1") String username,
    @Schema(description = "Rating ELO atual", example = "1200") int eloRating,
    @Schema(description = "Total de partidas jogadas", example = "50") long totalGames,
    @Schema(description = "Vitórias", example = "30") long wins,
    @Schema(description = "Derrotas", example = "20") long losses,
    @Schema(description = "Taxa de vitória (0.0 a 1.0)", example = "0.6") double winRate,
    @Schema(description = "Posição no ranking global", example = "15") int rank,
    @Schema(description = "Data de criação da conta") Instant memberSince,
    @Schema(description = "Total de tiros disparados", example = "500") long totalShots,
    @Schema(description = "Tiros que acertaram", example = "200") long shotsHit,
    @Schema(description = "Navios afundados", example = "85") long shipsSunk,
    @Schema(description = "Precisão de tiro (0.0 a 1.0)", example = "0.4") double accuracy,
    @Schema(description = "Últimas partidas jogadas") List<GameHistoryEntry> recentGames
) {}
