package com.softexpert.batalhanaval_api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Requisição para entrar em sala privada via token de convite")
public record JoinRoomRequest(
    @NotBlank(message = "token é obrigatório")
    @Size(max = 50, message = "token inválido")
    @Schema(description = "Token de convite da sala", example = "ABC123")
    String token
) {
}
