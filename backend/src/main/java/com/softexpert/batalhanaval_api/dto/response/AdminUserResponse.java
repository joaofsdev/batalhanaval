package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.UserRole;
import com.softexpert.batalhanaval_api.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Dados de usuário para painel administrativo")
public record AdminUserResponse(
    @Schema(description = "ID do usuário") UUID id,
    @Schema(description = "Nome de usuário", example = "jogador1") String username,
    @Schema(description = "Email", example = "jogador@exemplo.com") String email,
    @Schema(description = "Papel do usuário", example = "PLAYER") UserRole role,
    @Schema(description = "Status atual", example = "ACTIVE") UserStatus status,
    @Schema(description = "Suspenso até (null se ativo)") Instant suspendedUntil,
    @Schema(description = "Data de criação da conta") Instant createdAt
) {}
