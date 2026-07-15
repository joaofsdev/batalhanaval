package com.softexpert.batalhanaval_api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Configuração de um tipo de navio da frota")
public record FleetConfigResponse(
    @Schema(description = "Identificador do tipo de navio", example = "BATTLESHIP") String type,
    @Schema(description = "Tamanho em células", example = "4") int size,
    @Schema(description = "Nome de exibição", example = "Encouraçado") String name
) {}
