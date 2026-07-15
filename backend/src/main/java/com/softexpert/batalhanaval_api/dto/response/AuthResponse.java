package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Resposta de autenticação com token JWT")
public record AuthResponse(
    @Schema(description = "ID do jogador", example = "550e8400-e29b-41d4-a716-446655440000") UUID id,
    @Schema(description = "Nome de usuário", example = "jogador1") String username,
    @Schema(description = "Email do jogador", example = "jogador@exemplo.com") String email,
    @Schema(description = "Papel do usuário", example = "PLAYER") UserRole role,
    @Schema(description = "Token JWT para autenticação", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2dhZG9yMSJ9.abc123") String token
) {}
