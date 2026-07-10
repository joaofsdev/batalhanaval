package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.GameStatus;

import java.util.UUID;

public record GameStateNotification(
    GameStatus status,
    UUID currentTurnPlayerId,
    UUID winnerId,
    int turnNumber,
    boolean isStormTurn,
    int turnsUntilStorm,
    boolean bonusShot,
    boolean fogActive
) {
    public GameStateNotification(GameStatus status, UUID currentTurnPlayerId, UUID winnerId) {
        this(status, currentTurnPlayerId, winnerId, 0, false, 0, false, false);
    }
}
