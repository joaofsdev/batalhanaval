package com.softexpert.batalhanaval_api.dto.request;

import com.softexpert.batalhanaval_api.domain.GameMode;
import jakarta.validation.constraints.NotNull;

public record CreateRoomRequest(
    @NotNull(message = "gameMode é obrigatório")
    GameMode gameMode
) {
}
