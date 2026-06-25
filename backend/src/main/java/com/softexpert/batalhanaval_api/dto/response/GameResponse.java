package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.GameStatus;

import java.time.Instant;
import java.util.UUID;

public record GameResponse(
    UUID id,
    GameStatus status,
    PlayerSummary player1,
    PlayerSummary player2,
    UUID currentTurnPlayerId,
    UUID winnerId,
    BoardResponse myBoard,
    OpponentBoardResponse opponentBoard,
    Instant createdAt
) {}
