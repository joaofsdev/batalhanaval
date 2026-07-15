package com.softexpert.batalhanaval_api.controller;

import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.domain.GameMode;
import com.softexpert.batalhanaval_api.domain.GameStatus;
import com.softexpert.batalhanaval_api.domain.PlayerAbility;
import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.dto.request.UseAbilityRequest;
import com.softexpert.batalhanaval_api.dto.response.AbilityResultResponse;
import com.softexpert.batalhanaval_api.dto.response.AbilityStatusResponse;
import com.softexpert.batalhanaval_api.exception.GameNotFoundException;
import com.softexpert.batalhanaval_api.repository.GameRepository;
import com.softexpert.batalhanaval_api.repository.PlayerAbilityRepository;
import com.softexpert.batalhanaval_api.repository.UserRepository;
import com.softexpert.batalhanaval_api.service.AbilityService;
import com.softexpert.batalhanaval_api.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/games/{gameId}/ability")
@RequiredArgsConstructor
@Tag(name = "Habilidades")
@SecurityRequirement(name = "bearerAuth")
public class AbilityController {

    private final AbilityService abilityService;
    private final NotificationService notificationService;
    private final PlayerAbilityRepository playerAbilityRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    @GetMapping
    @Operation(
            summary = "Consultar habilidade disponível",
            description = "Retorna a habilidade especial atribuída ao jogador nesta partida (modo Tempestade), indicando se já foi utilizada.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Habilidade retornada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "404", description = "Partida não encontrada ou não é modo Tempestade")
    })
    public AbilityStatusResponse getAbility(
        @PathVariable UUID gameId,
        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = resolveUserId(userDetails);

        PlayerAbility ability = playerAbilityRepository.findByGameIdAndUserId(gameId, userId)
            .orElseGet(() -> lazyInitializeIfEligible(gameId, userId));

        return new AbilityStatusResponse(
            ability.getAbilityType(),
            ability.getAbilityType().getDisplayName(),
            ability.getAbilityType().getDescription(),
            ability.isUsed(),
            ability.getUsedOnTurn()
        );
    }

    @PostMapping
    @Operation(
            summary = "Usar habilidade especial",
            description = "Ativa a habilidade especial do jogador (Radar, Torpedo Duplo, Escudo ou Bombardeio em Linha). Cada habilidade só pode ser usada uma vez por partida.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Habilidade utilizada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Parâmetros inválidos para a habilidade"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "404", description = "Partida não encontrada"),
        @ApiResponse(responseCode = "409", description = "Habilidade já utilizada ou não é o turno do jogador")
    })
    public AbilityResultResponse useAbility(
        @PathVariable UUID gameId,
        @Valid @RequestBody UseAbilityRequest request,
        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = resolveUserId(userDetails);

        AbilityResultResponse result = abilityService.useAbility(
            gameId, userId, request.abilityType(),
            request.row(), request.col(),
            request.axis(), request.index()
        );

        notificationService.notifyAbilityResult(userId, gameId, result);

        Game game = gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);
        notificationService.broadcastGameState(game);

        return result;
    }

    private UUID resolveUserId(UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return user.getId();
    }

    private PlayerAbility lazyInitializeIfEligible(UUID gameId, UUID userId) {
        Game game = gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);

        if (game.getGameMode() == GameMode.STORM && game.getStatus() == GameStatus.IN_PROGRESS) {
            log.warn("[AUTO-CORREÇÃO] Habilidades ausentes para partida TEMPESTADE em andamento. partida={}, jogador={}. Inicializando agora.", gameId, userId);

            try {
                abilityService.initializeAbilities(game);
            } catch (DataIntegrityViolationException e) {
                log.info("[AUTO-CORREÇÃO] Inicialização concorrente detectada (chave duplicada). partida={}, jogador={}. Usando registro existente.", gameId, userId);
            }

            return playerAbilityRepository.findByGameIdAndUserId(gameId, userId)
                .orElseThrow(GameNotFoundException::new);
        }

        throw new GameNotFoundException();
    }
}
