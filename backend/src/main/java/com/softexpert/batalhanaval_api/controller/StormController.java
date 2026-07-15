package com.softexpert.batalhanaval_api.controller;

import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.dto.response.StormInfoResponse;
import com.softexpert.batalhanaval_api.exception.GameNotFoundException;
import com.softexpert.batalhanaval_api.repository.GameRepository;
import com.softexpert.batalhanaval_api.repository.UserRepository;
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

    private final GameRepository gameRepository;
    private final UserRepository userRepository;

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
        Game game = gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);

        int turnsUntilStorm = Math.max(0, game.getNextStormTurn() - game.getCurrentTurnNumber());

        return new StormInfoResponse(
            game.getNextStormTurn(),
            game.getCurrentTurnNumber(),
            turnsUntilStorm
        );
    }
}
