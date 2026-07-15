package com.softexpert.batalhanaval_api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Coordenadas de um disparo no tabuleiro")
public record FireRequest(
    @Min(0) @Max(9) @Schema(description = "Linha do tabuleiro (0-9)", example = "3") int row,
    @Min(0) @Max(9) @Schema(description = "Coluna do tabuleiro (0-9)", example = "7") int col
) {}
