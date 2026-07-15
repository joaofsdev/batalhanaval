package com.softexpert.batalhanaval_api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais de login")
public record LoginRequest(
    @NotBlank @Schema(description = "Nome de usuário", example = "jogador1") String username,
    @NotBlank @Schema(description = "Senha do jogador", example = "Senha@123") String password
) {}
