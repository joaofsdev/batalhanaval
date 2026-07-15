package com.softexpert.batalhanaval_api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Resultado de solicitação de revanche")
public record RematchResponse(
    @Schema(description = "Status da revanche", example = "WAITING") RematchStatus status,
    @Schema(description = "ID da nova partida (quando status = MATCHED)") UUID gameId
) {
    public enum RematchStatus {
        WAITING,
        MATCHED
    }
}
