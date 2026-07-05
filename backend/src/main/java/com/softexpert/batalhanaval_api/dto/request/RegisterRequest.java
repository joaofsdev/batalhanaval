package com.softexpert.batalhanaval_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(min = 3, max = 30) @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Apenas letras, números e underscore")
    String username,

    @NotBlank @Email @Size(max = 100)
    @Pattern(regexp = "^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$", message = "Email inválido")
    String email,

    @NotBlank @Size(min = 6, max = 100, message = "A senha deve ter entre 6 e 100 caracteres")
    @Pattern(regexp = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*", message = "A senha deve conter pelo menos 1 símbolo")
    String password
) {}
