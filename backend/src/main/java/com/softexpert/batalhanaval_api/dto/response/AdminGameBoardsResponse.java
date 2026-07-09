package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.GameMode;
import com.softexpert.batalhanaval_api.domain.GameStatus;

import java.util.UUID;

public record AdminGameBoardsResponse(
    UUID gameId,
    GameStatus status,
    GameMode gameMode,
    PlayerBoardSummary player1,
    PlayerBoardSummary player2
) {}
