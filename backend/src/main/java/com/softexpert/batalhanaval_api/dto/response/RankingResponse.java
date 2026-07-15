package com.softexpert.batalhanaval_api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Ranking de jogadores com posição do jogador atual")
public record RankingResponse(
    @Schema(description = "Lista de jogadores no ranking") List<RankingEntry> ranking,
    @Schema(description = "Posição do jogador autenticado no ranking") RankingEntry myPosition,
    @Schema(description = "Número da página", example = "0") int page,
    @Schema(description = "Tamanho da página", example = "20") int size,
    @Schema(description = "Total de jogadores ranqueados", example = "150") long totalElements,
    @Schema(description = "Total de páginas", example = "8") int totalPages
) {}
