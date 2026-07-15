package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.AbilityType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status da habilidade especial do jogador na partida")
public record AbilityStatusResponse(
    @Schema(description = "Tipo da habilidade atribuída", example = "RADAR") AbilityType abilityType,
    @Schema(description = "Nome de exibição da habilidade", example = "Radar") String name,
    @Schema(description = "Descrição do efeito da habilidade") String description,
    @Schema(description = "Se a habilidade já foi utilizada", example = "false") boolean used,
    @Schema(description = "Turno em que foi utilizada (null se não usada)") Integer usedOnTurn
) {}
