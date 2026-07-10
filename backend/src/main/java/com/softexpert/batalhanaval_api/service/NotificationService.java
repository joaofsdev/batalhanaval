package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.dto.response.AbilityResultResponse;
import com.softexpert.batalhanaval_api.dto.response.AbilityRotationNotification;
import com.softexpert.batalhanaval_api.dto.response.AbilityRotationResult;
import com.softexpert.batalhanaval_api.dto.response.GameStateNotification;
import com.softexpert.batalhanaval_api.dto.response.OpponentConnectionEvent;
import com.softexpert.batalhanaval_api.dto.response.OpponentShotNotification;
import com.softexpert.batalhanaval_api.dto.response.RematchInvite;
import com.softexpert.batalhanaval_api.dto.response.ShotResultResponse;
import com.softexpert.batalhanaval_api.dto.response.StormEventNotification;
import com.softexpert.batalhanaval_api.repository.StormEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final StormEventRepository stormEventRepository;

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

        int turnsUntilStorm = Math.max(0, game.getNextStormTurn() - game.getCurrentTurnNumber());
        boolean isStormTurn = stormEventRepository.findByGameIdAndResolvedFalse(game.getId()).isPresent();

        GameStateNotification notification = new GameStateNotification(
            game.getStatus(), currentTurnId, winnerId,
            game.getCurrentTurnNumber(), isStormTurn, turnsUntilStorm,
            game.isBonusShot(), game.isFogActive()
        );

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

    public void notifyRematchMatched(UUID originalGameId, UUID newGameId) {
        Object payload = java.util.Map.of("status", "MATCHED", "gameId", newGameId.toString());
        messagingTemplate.convertAndSend(
            "/topic/game/" + originalGameId + "/rematch",
            payload
        );
    }

    public void notifyOpponentDisconnected(UUID gameId, int gracePeriodSeconds) {
        messagingTemplate.convertAndSend(
            "/topic/game/" + gameId + "/opponent-disconnected",
            OpponentConnectionEvent.disconnected(gracePeriodSeconds)
        );
    }

    public void notifyOpponentReconnected(UUID gameId) {
        messagingTemplate.convertAndSend(
            "/topic/game/" + gameId + "/opponent-disconnected",
            OpponentConnectionEvent.reconnected()
        );
    }

    public void broadcastStormEvent(UUID gameId, StormEventNotification notification) {
        messagingTemplate.convertAndSend(
            "/topic/game/" + gameId + "/storm",
            notification
        );
    }

    public void notifyAbilityResult(UUID userId, UUID gameId, AbilityResultResponse result) {
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/game/ability-result",
            result
        );
    }

    public void notifyRoomUpdate(UUID gameId, Object roomResponse) {
        messagingTemplate.convertAndSend(
            "/topic/room/" + gameId,
            roomResponse
        );
    }

    public void notifyRoomCancelled(UUID gameId) {
        Object payload = java.util.Map.of("status", "CANCELLED");
        messagingTemplate.convertAndSend(
            "/topic/room/" + gameId,
            payload
        );
    }

    public void notifyAbilityRotated(List<AbilityRotationResult> rotationResults) {
        for (AbilityRotationResult result : rotationResults) {
            AbilityRotationNotification notification = AbilityRotationNotification.from(result);
            messagingTemplate.convertAndSendToUser(
                result.playerId().toString(),
                "/queue/game/ability-rotated",
                notification
            );
        }
    }
}
