package com.softexpert.batalhanaval_api.controller;

import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.dto.response.PlayerProfileResponse;
import com.softexpert.batalhanaval_api.repository.UserRepository;
import com.softexpert.batalhanaval_api.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Perfil")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;
    private final UserRepository userRepository;

    @GetMapping("/me/profile")
    @Operation(
            summary = "Obter meu perfil",
            description = "Retorna o perfil e estatísticas do jogador autenticado (vitórias, derrotas, ELO, etc.).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil retornado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido")
    })
    public PlayerProfileResponse getMyProfile(@Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return profileService.getProfile(currentUser.getId());
    }

    @GetMapping("/{id}/profile")
    @Operation(
            summary = "Obter perfil de outro jogador",
            description = "Retorna o perfil público e estatísticas de um jogador específico pelo ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil retornado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "404", description = "Jogador não encontrado")
    })
    public PlayerProfileResponse getProfile(@PathVariable UUID id) {
        return profileService.getProfile(id);
    }
}
