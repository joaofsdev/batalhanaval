package com.softexpert.batalhanaval_api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Informações sobre o próximo evento de tempestade")
public record StormInfoResponse(
    @Schema(description = "Turno em que ocorrerá o próximo evento", example = "8") int nextStormTurn,
    @Schema(description = "Turno atual da partida", example = "5") int currentTurn,
    @Schema(description = "Turnos restantes até o evento", example = "3") int turnsUntilStorm
) {}
