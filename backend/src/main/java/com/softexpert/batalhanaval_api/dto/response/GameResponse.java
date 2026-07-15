package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.GameMode;
import com.softexpert.batalhanaval_api.domain.GameStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Estado completo de uma partida")
public record GameResponse(
    @Schema(description = "ID da partida") UUID id,
    @Schema(description = "Status atual da partida", example = "IN_PROGRESS") GameStatus status,
    @Schema(description = "Modo de jogo", example = "CLASSIC") GameMode gameMode,
    @Schema(description = "Jogador 1") PlayerSummary player1,
    @Schema(description = "Jogador 2 (null se aguardando oponente)") PlayerSummary player2,
    @Schema(description = "ID do jogador cujo turno é atual") UUID currentTurnPlayerId,
    @Schema(description = "ID do vencedor (null se partida em andamento)") UUID winnerId,
    @Schema(description = "Tabuleiro do jogador autenticado") BoardResponse myBoard,
    @Schema(description = "Tabuleiro do oponente (apenas células atacadas visíveis)") OpponentBoardResponse opponentBoard,
    @Schema(description = "Variação de ELO após o término da partida") Integer eloDelta,
    @Schema(description = "Data de criação da partida") Instant createdAt,
    @Schema(description = "Prazo limite para posicionamento") Instant placementDeadline
) {}
