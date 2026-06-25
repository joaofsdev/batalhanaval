package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.GameStatus;

import java.util.UUID;

public record GameStateNotification(
    GameStatus status,
    UUID currentTurnPlayerId,
    UUID winnerId
) {}
