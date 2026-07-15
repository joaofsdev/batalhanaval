package com.softexpert.batalhanaval_api.dto.request;

import com.softexpert.batalhanaval_api.domain.Orientation;
import com.softexpert.batalhanaval_api.domain.ShipType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Posicionamento individual de um navio")
public record ShipPlacement(
    @NotNull @Schema(description = "Tipo do navio", example = "BATTLESHIP") ShipType shipType,
    @Min(0) @Max(9) @Schema(description = "Linha de origem (0-9)", example = "0") int originRow,
    @Min(0) @Max(9) @Schema(description = "Coluna de origem (0-9)", example = "2") int originCol,
    @NotNull @Schema(description = "Orientação do navio", example = "HORIZONTAL") Orientation orientation
) {}
