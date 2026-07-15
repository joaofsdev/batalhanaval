package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.GameStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado do posicionamento de frota")
public record PlaceShipsResponse(
    @Schema(description = "Mensagem de confirmação", example = "Frota posicionada com sucesso") String message,
    @Schema(description = "Se o tabuleiro está pronto para a partida", example = "true") boolean boardReady,
    @Schema(description = "Status atualizado da partida", example = "IN_PROGRESS") GameStatus gameStatus
) {}
