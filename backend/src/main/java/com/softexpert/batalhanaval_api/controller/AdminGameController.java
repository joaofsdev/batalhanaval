package com.softexpert.batalhanaval_api.controller;

import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.dto.response.AdminGameBoardsResponse;
import com.softexpert.batalhanaval_api.dto.response.AdminGameResponse;
import com.softexpert.batalhanaval_api.dto.response.PageResponse;
import com.softexpert.batalhanaval_api.repository.UserRepository;
import com.softexpert.batalhanaval_api.service.AdminGameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/games")
@RequiredArgsConstructor
@Tag(name = "Admin — Partidas")
@SecurityRequirement(name = "bearerAuth")
public class AdminGameController {

    private final AdminGameService adminGameService;
    private final UserRepository userRepository;

    @GetMapping("/active")
    @Operation(
            summary = "Listar partidas ativas",
            description = "Retorna a lista paginada de partidas em andamento na plataforma.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de partidas ativas retornada"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores")
    })
    public PageResponse<AdminGameResponse> listActiveGames(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return adminGameService.listActiveGames(page, size);
    }

    @GetMapping("/{id}/boards")
    @Operation(
            summary = "Revelar tabuleiros",
            description = "Revela os tabuleiros completos de ambos os jogadores de uma partida (uso administrativo para investigação).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tabuleiros revelados com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores"),
        @ApiResponse(responseCode = "404", description = "Partida não encontrada")
    })
    public AdminGameBoardsResponse revealBoards(@PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return adminGameService.revealBoards(id, admin);
    }

    @PatchMapping("/{id}/force-end")
    @Operation(
            summary = "Forçar encerramento de partida",
            description = "Encerra forçadamente uma partida ativa (sem vencedor). Ação registrada no audit log.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Partida encerrada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores"),
        @ApiResponse(responseCode = "404", description = "Partida não encontrada"),
        @ApiResponse(responseCode = "409", description = "Partida não está em andamento")
    })
    public AdminGameResponse forceEnd(@PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return adminGameService.forceEnd(id, admin);
    }
}
