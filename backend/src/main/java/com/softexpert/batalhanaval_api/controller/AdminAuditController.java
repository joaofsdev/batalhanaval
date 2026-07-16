package com.softexpert.batalhanaval_api.controller;

import com.softexpert.batalhanaval_api.dto.response.AdminAuditLogResponse;
import com.softexpert.batalhanaval_api.dto.response.PageResponse;
import com.softexpert.batalhanaval_api.service.AdminAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit-log")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Auditoria")
@SecurityRequirement(name = "bearerAuth")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    @GetMapping
    @Operation(
            summary = "Listar logs de auditoria",
            description = "Retorna a lista paginada de ações administrativas registradas (bans, suspensões, encerramentos forçados).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logs de auditoria retornados"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores")
    })
    public PageResponse<AdminAuditLogResponse> listAuditLogs(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return adminAuditService.listAuditLogs(page, size);
    }
}
