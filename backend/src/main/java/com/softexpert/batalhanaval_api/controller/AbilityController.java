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
public class AbilityController {

    private final AbilityService abilityService;
    private final NotificationService notificationService;
    private final PlayerAbilityRepository playerAbilityRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    @GetMapping
    public AbilityStatusResponse getAbility(
        @PathVariable UUID gameId,
        @AuthenticationPrincipal UserDetails userDetails
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
    public AbilityResultResponse useAbility(
        @PathVariable UUID gameId,
        @Valid @RequestBody UseAbilityRequest request,
        @AuthenticationPrincipal UserDetails userDetails
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
