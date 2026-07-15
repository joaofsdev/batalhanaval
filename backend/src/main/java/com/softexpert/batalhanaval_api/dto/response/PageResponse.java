package com.softexpert.batalhanaval_api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resposta paginada genérica")
public record PageResponse<T>(
    @Schema(description = "Lista de itens da página atual") List<T> content,
    @Schema(description = "Número da página (base 0)", example = "0") int page,
    @Schema(description = "Tamanho da página", example = "10") int size,
    @Schema(description = "Total de elementos", example = "42") long totalElements,
    @Schema(description = "Total de páginas", example = "5") int totalPages
) {}
