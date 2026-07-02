package com.softexpert.batalhanaval_api.dto.request;

import com.softexpert.batalhanaval_api.domain.GameMode;
import jakarta.validation.constraints.NotNull;

public record CreateGameRequest(
    @NotNull(message = "gameMode is required")
    GameMode gameMode
) {
}
