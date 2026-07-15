package com.softexpert.batalhanaval_api.controller;

import com.softexpert.batalhanaval_api.dto.request.LoginRequest;
import com.softexpert.batalhanaval_api.dto.request.RegisterRequest;
import com.softexpert.batalhanaval_api.dto.response.AuthResponse;
import com.softexpert.batalhanaval_api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Registrar novo jogador",
            description = "Cria uma conta de jogador e retorna o token JWT para uso imediato.",
            security = {})
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Jogador registrado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos ou campos obrigatórios ausentes"),
        @ApiResponse(responseCode = "409", description = "Nome de usuário já existe")
    })
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Autenticar jogador",
            description = "Valida credenciais e retorna o token JWT para acesso aos endpoints protegidos.",
            security = {})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Credenciais incorretas")
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
