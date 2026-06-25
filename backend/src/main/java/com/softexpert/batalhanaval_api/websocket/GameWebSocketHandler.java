package com.softexpert.batalhanaval_api.websocket;

import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.dto.request.FireRequest;
import com.softexpert.batalhanaval_api.dto.response.ErrorResponse;
import com.softexpert.batalhanaval_api.dto.response.ShotResultResponse;
import com.softexpert.batalhanaval_api.exception.DomainException;
import com.softexpert.batalhanaval_api.repository.GameRepository;
import com.softexpert.batalhanaval_api.security.WebSocketAuthInterceptor.StompPrincipal;
import com.softexpert.batalhanaval_api.service.NotificationService;
import com.softexpert.batalhanaval_api.service.ShotService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class GameWebSocketHandler {

    private final ShotService shotService;
    private final NotificationService notificationService;
    private final GameRepository gameRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game/{gameId}/fire")
    public void fire(@DestinationVariable UUID gameId, FireRequest request, Principal principal) {
        StompPrincipal stompPrincipal = (StompPrincipal) principal;
        UUID attackerId = stompPrincipal.userId();

        try {
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
        }
    }
}
