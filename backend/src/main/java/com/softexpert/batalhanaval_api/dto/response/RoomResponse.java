package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.GameMode;

import java.util.UUID;

public record RoomResponse(
    UUID gameId,
    String token,
    GameMode gameMode,
    String hostUsername,
    String guestUsername,
    boolean hostReady,
    boolean guestReady,
    RoomStatus status
) {
    public enum RoomStatus {
        WAITING_OPPONENT,
        WAITING_CONFIRMATION,
        STARTING
    }
}
