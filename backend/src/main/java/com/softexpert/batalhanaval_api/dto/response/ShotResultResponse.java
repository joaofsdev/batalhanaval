package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.Orientation;
import com.softexpert.batalhanaval_api.domain.ShipType;
import com.softexpert.batalhanaval_api.domain.ShotResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Resultado de um disparo")
public record ShotResultResponse(
    @Schema(description = "ID da partida") UUID gameId,
    @Schema(description = "Linha do disparo", example = "3") int row,
    @Schema(description = "Coluna do disparo", example = "7") int col,
    @Schema(description = "Resultado do disparo", example = "HIT") ShotResult result,
    @Schema(description = "Tipo do navio afundado (null se não afundou)") ShipType sunkShipType,
    @Schema(description = "Linha de origem do navio afundado") Integer sunkShipOriginRow,
    @Schema(description = "Coluna de origem do navio afundado") Integer sunkShipOriginCol,
    @Schema(description = "Orientação do navio afundado") Orientation sunkShipOrientation
) {}
