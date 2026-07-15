package com.softexpert.batalhanaval_api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados de registro de novo jogador")
public record RegisterRequest(
    @NotBlank @Size(min = 3, max = 30) @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Apenas letras, números e underscore")
    @Schema(description = "Nome de usuário (3-30 caracteres, alfanumérico + underscore)", example = "jogador1")
    String username,

    @NotBlank @Email @Size(max = 100)
    @Pattern(regexp = "^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$", message = "Email inválido")
    @Schema(description = "Email do jogador", example = "jogador@exemplo.com")
    String email,

    @NotBlank @Size(min = 6, max = 100, message = "A senha deve ter entre 6 e 100 caracteres")
    @Pattern(regexp = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*", message = "A senha deve conter pelo menos 1 símbolo")
    @Schema(description = "Senha (6-100 caracteres, deve conter pelo menos 1 símbolo)", example = "Senha@123")
    String password
) {}
