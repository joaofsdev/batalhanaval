package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.dto.response.GameStateNotification;
import com.softexpert.batalhanaval_api.dto.response.OpponentShotNotification;
import com.softexpert.batalhanaval_api.dto.response.RematchInvite;
import com.softexpert.batalhanaval_api.dto.response.ShotResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyShotResult(UUID attackerId, ShotResultResponse result) {
        messagingTemplate.convertAndSendToUser(
            attackerId.toString(),
            "/queue/game/shot-result",
            result
        );
    }

    public void notifyOpponentShot(UUID defenderId, ShotResultResponse result) {
        OpponentShotNotification notification = new OpponentShotNotification(
            result.gameId(), result.row(), result.col(), result.result(), result.sunkShipType()
        );
        messagingTemplate.convertAndSendToUser(
            defenderId.toString(),
            "/queue/game/opponent-shot",
            notification
        );
    }

    public void broadcastGameState(Game game) {
        UUID currentTurnId = game.getCurrentTurn() != null ? game.getCurrentTurn().getId() : null;
        UUID winnerId = game.getWinner() != null ? game.getWinner().getId() : null;

        GameStateNotification notification = new GameStateNotification(game.getStatus(), currentTurnId, winnerId);
        messagingTemplate.convertAndSend(
            "/topic/game/" + game.getId() + "/state",
            notification
        );
    }

    public void notifyRematchInvite(UUID opponentId, RematchInvite invite) {
        messagingTemplate.convertAndSendToUser(
            opponentId.toString(),
            "/queue/game/rematch-invite",
            invite
        );
    }
}
