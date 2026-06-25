package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.GameStatus;

public record PlaceShipsResponse(
    String message,
    boolean boardReady,
    GameStatus gameStatus
) {}
