package com.softexpert.batalhanaval_api.websocket;

import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.domain.GameMode;
import com.softexpert.batalhanaval_api.domain.GameStatus;
import com.softexpert.batalhanaval_api.dto.request.FireRequest;
import com.softexpert.batalhanaval_api.dto.response.ErrorResponse;
import com.softexpert.batalhanaval_api.dto.response.ShotResultResponse;
import com.softexpert.batalhanaval_api.dto.response.StormEventNotification;
import com.softexpert.batalhanaval_api.exception.DomainException;
import com.softexpert.batalhanaval_api.repository.GameRepository;
import com.softexpert.batalhanaval_api.security.WebSocketAuthInterceptor.StompPrincipal;
import com.softexpert.batalhanaval_api.service.NotificationService;
import com.softexpert.batalhanaval_api.service.ShotService;
import com.softexpert.batalhanaval_api.service.StormService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameWebSocketHandler {

    private final ShotService shotService;
    private final StormService stormService;
    private final NotificationService notificationService;
    private final GameRepository gameRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game/{gameId}/fire")
    public void fire(@DestinationVariable UUID gameId, FireRequest request, Principal principal) {
        StompPrincipal stompPrincipal = (StompPrincipal) principal;
        UUID attackerId = stompPrincipal.userId();

        try {
            Game gameBefore = gameRepository.findById(gameId).orElseThrow();
            if (gameBefore.getGameMode() == GameMode.STORM
                && gameBefore.getStatus() == GameStatus.IN_PROGRESS
                && stormService.isStormTurn(gameId, gameBefore.getCurrentTurnNumber())) {

                StormEventNotification stormNotification = stormService.resolveStormEvent(gameId);
                if (stormNotification != null) {
                    notificationService.broadcastStormEvent(gameId, stormNotification);
                }
            }

            ShotResultResponse result = shotService.processShot(gameId, attackerId, request.row(), request.col());

            notificationService.notifyShotResult(attackerId, result);

            Game game = gameRepository.findById(gameId).orElseThrow();
            UUID defenderId = shotService.getDefenderId(game, attackerId);
            notificationService.notifyOpponentShot(defenderId, result);
            notificationService.broadcastGameState(game);
        } catch (DomainException ex) {
            messagingTemplate.convertAndSendToUser(
                attackerId.toString(),
                "/queue/errors",
                ErrorResponse.of(ex.getErrorCode(), ex.getMessage())
            );
        } catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate shot ignored: game={}, attacker={}, row={}, col={}",
                gameId, attackerId, request.row(), request.col());
            messagingTemplate.convertAndSendToUser(
                attackerId.toString(),
                "/queue/errors",
                ErrorResponse.of("CELL_ALREADY_ATTACKED", "Você já atacou esta posição.")
            );
        }
    }
}
