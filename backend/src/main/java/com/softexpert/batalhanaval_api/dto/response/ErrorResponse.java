package com.softexpert.batalhanaval_api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Resposta de erro padronizada da API")
public record ErrorResponse(
    @Schema(description = "Código de erro", example = "GAME_NOT_FOUND") String code,
    @Schema(description = "Mensagem descritiva do erro", example = "Partida não encontrada") String message,
    @Schema(description = "Data/hora do erro") Instant timestamp
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, Instant.now());
    }
}
