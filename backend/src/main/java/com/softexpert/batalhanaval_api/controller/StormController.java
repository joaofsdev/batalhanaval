package com.softexpert.batalhanaval_api.controller;

import com.softexpert.batalhanaval_api.dto.response.StormInfoResponse;
import com.softexpert.batalhanaval_api.service.StormService;
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
@RequestMapping("/api/games/{gameId}/storm")
@RequiredArgsConstructor
@Tag(name = "Tempestade")
@SecurityRequirement(name = "bearerAuth")
public class StormController {

    private final StormService stormService;

    @GetMapping("/next")
    @Operation(
            summary = "Próximo evento de tempestade",
            description = "Retorna informações sobre o próximo evento climático: turno previsto, turno atual e quantos turnos faltam.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Informações de tempestade retornadas"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "404", description = "Partida não encontrada")
    })
    public StormInfoResponse getNextStorm(
        @PathVariable UUID gameId,
        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        return stormService.getNextStormInfo(gameId);
    }
}
