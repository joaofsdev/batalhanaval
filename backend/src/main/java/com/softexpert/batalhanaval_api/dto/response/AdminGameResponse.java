package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.GameMode;
import com.softexpert.batalhanaval_api.domain.GameStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminGameResponse(
    UUID id,
    GameStatus status,
    GameMode gameMode,
    PlayerSummary player1,
    PlayerSummary player2,
    Instant createdAt,
    Instant updatedAt
) {}
