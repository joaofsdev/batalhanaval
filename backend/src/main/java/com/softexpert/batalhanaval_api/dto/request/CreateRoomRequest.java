package com.softexpert.batalhanaval_api.dto.request;

import com.softexpert.batalhanaval_api.domain.GameMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Requisição para criar sala privada")
public record CreateRoomRequest(
    @NotNull(message = "gameMode é obrigatório")
    @Schema(description = "Modo de jogo da sala", example = "STORM")
    GameMode gameMode
) {
}
