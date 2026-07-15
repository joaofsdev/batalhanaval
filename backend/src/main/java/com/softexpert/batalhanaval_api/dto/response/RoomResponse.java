package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.GameMode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Estado de uma sala privada")
public record RoomResponse(
    @Schema(description = "ID da partida/sala") UUID gameId,
    @Schema(description = "Token de convite para compartilhar", example = "ABC123") String token,
    @Schema(description = "Modo de jogo da sala", example = "STORM") GameMode gameMode,
    @Schema(description = "Nome do criador da sala", example = "jogador1") String hostUsername,
    @Schema(description = "Nome do convidado (null se aguardando)", example = "jogador2") String guestUsername,
    @Schema(description = "Criador confirmou prontidão") boolean hostReady,
    @Schema(description = "Convidado confirmou prontidão") boolean guestReady,
    @Schema(description = "Status da sala", example = "WAITING_OPPONENT") RoomStatus status
) {
    public enum RoomStatus {
        WAITING_OPPONENT,
        WAITING_CONFIRMATION,
        STARTING
    }
}
