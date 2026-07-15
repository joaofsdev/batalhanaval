package com.softexpert.batalhanaval_api.dto.request;

import com.softexpert.batalhanaval_api.domain.GameMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Requisição para criar ou entrar em partida via matchmaking")
public record CreateGameRequest(
    @NotNull(message = "gameMode é obrigatório")
    @Schema(description = "Modo de jogo desejado", example = "CLASSIC")
    GameMode gameMode
) {
}
