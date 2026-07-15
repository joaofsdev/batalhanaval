package com.softexpert.batalhanaval_api.controller;

import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.domain.UserStatus;
import com.softexpert.batalhanaval_api.dto.request.SuspendRequest;
import com.softexpert.batalhanaval_api.dto.response.AdminUserResponse;
import com.softexpert.batalhanaval_api.dto.response.PageResponse;
import com.softexpert.batalhanaval_api.repository.UserRepository;
import com.softexpert.batalhanaval_api.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin — Usuários")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(
            summary = "Listar usuários",
            description = "Retorna a lista paginada de usuários da plataforma. Filtrável por status (ACTIVE, SUSPENDED, BANNED).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de usuários retornada"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores")
    })
    public PageResponse<AdminUserResponse> listUsers(
        @RequestParam(required = false) UserStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return adminUserService.listUsers(status, page, size);
    }

    @PatchMapping("/{id}/ban")
    @Operation(
            summary = "Banir usuário",
            description = "Bane permanentemente um usuário da plataforma. Ação registrada no audit log.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário banido com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public AdminUserResponse ban(@PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        User admin = resolveAdmin(userDetails);
        return adminUserService.ban(id, admin);
    }

    @PatchMapping("/{id}/suspend")
    @Operation(
            summary = "Suspender usuário",
            description = "Suspende temporariamente um usuário até a data especificada. Ação registrada no audit log.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário suspenso com sucesso"),
        @ApiResponse(responseCode = "400", description = "Data de suspensão inválida"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public AdminUserResponse suspend(@PathVariable UUID id, @Valid @RequestBody SuspendRequest request,
                                     @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        User admin = resolveAdmin(userDetails);
        return adminUserService.suspend(id, request.suspendedUntil(), admin);
    }

    @PatchMapping("/{id}/reactivate")
    @Operation(
            summary = "Reativar usuário",
            description = "Reativa um usuário que estava suspenso ou banido. Ação registrada no audit log.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário reativado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public AdminUserResponse reactivate(@PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        User admin = resolveAdmin(userDetails);
        return adminUserService.reactivate(id, admin);
    }

    private User resolveAdmin(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
    }
}
