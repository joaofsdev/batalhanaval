package com.softexpert.batalhanaval_api.dto.request;

import com.softexpert.batalhanaval_api.domain.AbilityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Requisição para usar uma habilidade especial do modo Tempestade")
public record UseAbilityRequest(
    @NotNull @Schema(description = "Tipo da habilidade a ser usada", example = "RADAR") AbilityType abilityType,
    @Schema(description = "Linha alvo (necessário para RADAR e DOUBLE_TORPEDO)", example = "4") Integer row,
    @Schema(description = "Coluna alvo (necessário para RADAR e DOUBLE_TORPEDO)", example = "5") Integer col,
    @Schema(description = "Eixo do bombardeio (ROW ou COL, necessário para LINE_BOMBARDMENT)", example = "ROW") String axis,
    @Schema(description = "Índice da linha/coluna (necessário para LINE_BOMBARDMENT)", example = "3") Integer index
) {}
