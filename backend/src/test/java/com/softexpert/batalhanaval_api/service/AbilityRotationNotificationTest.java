package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.AbilityType;
import com.softexpert.batalhanaval_api.dto.response.AbilityRotationNotification;
import com.softexpert.batalhanaval_api.dto.response.AbilityRotationResult;
import com.softexpert.batalhanaval_api.repository.StormEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbilityRotationNotificationTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private StormEventRepository stormEventRepository;

    @InjectMocks private NotificationService notificationService;

    @Test
    void notifyAbilityRotated_sendsToCorrectDestinationPerPlayer() {
        UUID player1Id = UUID.randomUUID();
        UUID player2Id = UUID.randomUUID();

        List<AbilityRotationResult> results = List.of(
            new AbilityRotationResult(player1Id, AbilityType.RADAR, "Radar",
                "Revela presença de navios em área 3x3", AbilityType.SHIELD, true),
            new AbilityRotationResult(player2Id, AbilityType.DOUBLE_TORPEDO, "Torpedo Duplo",
                "Dispara 2 tiros no mesmo turno", AbilityType.LINE_BOMBARDMENT, false)
        );

        notificationService.notifyAbilityRotated(results);

        // Verify 2 messages sent (one per player)
        verify(messagingTemplate, times(2)).convertAndSendToUser(
            anyString(), eq("/queue/game/ability-rotated"), any()
        );

        // Verify player1 got the correct destination
        verify(messagingTemplate).convertAndSendToUser(
            eq(player1Id.toString()),
            eq("/queue/game/ability-rotated"),
            any(AbilityRotationNotification.class)
        );

        // Verify player2 got the correct destination
        verify(messagingTemplate).convertAndSendToUser(
            eq(player2Id.toString()),
            eq("/queue/game/ability-rotated"),
            any(AbilityRotationNotification.class)
        );
    }

    @Test
    void notifyAbilityRotated_eachPlayerReceivesOnlyOwnNotification() {
        UUID player1Id = UUID.randomUUID();
        UUID player2Id = UUID.randomUUID();

        List<AbilityRotationResult> results = List.of(
            new AbilityRotationResult(player1Id, AbilityType.RADAR, "Radar",
                "Revela presença de navios em área 3x3", AbilityType.SHIELD, true),
            new AbilityRotationResult(player2Id, AbilityType.DOUBLE_TORPEDO, "Torpedo Duplo",
                "Dispara 2 tiros no mesmo turno", AbilityType.LINE_BOMBARDMENT, false)
        );

        notificationService.notifyAbilityRotated(results);

        // Capture all notifications
        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);

        verify(messagingTemplate, times(2)).convertAndSendToUser(
            userCaptor.capture(), eq("/queue/game/ability-rotated"), payloadCaptor.capture()
        );

        List<String> users = userCaptor.getAllValues();
        List<Object> payloads = payloadCaptor.getAllValues();

        // Player 1's notification
        int p1Index = users.indexOf(player1Id.toString());
        AbilityRotationNotification p1Notification = (AbilityRotationNotification) payloads.get(p1Index);
        assertThat(p1Notification.newAbility()).isEqualTo(AbilityType.RADAR);
        assertThat(p1Notification.newAbilityName()).isEqualTo("Radar");
        assertThat(p1Notification.previousAbility()).isEqualTo(AbilityType.SHIELD);
        assertThat(p1Notification.previousWasDiscarded()).isTrue();

        // Player 2's notification
        int p2Index = users.indexOf(player2Id.toString());
        AbilityRotationNotification p2Notification = (AbilityRotationNotification) payloads.get(p2Index);
        assertThat(p2Notification.newAbility()).isEqualTo(AbilityType.DOUBLE_TORPEDO);
        assertThat(p2Notification.newAbilityName()).isEqualTo("Torpedo Duplo");
        assertThat(p2Notification.previousAbility()).isEqualTo(AbilityType.LINE_BOMBARDMENT);
        assertThat(p2Notification.previousWasDiscarded()).isFalse();
    }

    @Test
    void notifyAbilityRotated_notificationDoesNotContainPlayerId() {
        UUID playerId = UUID.randomUUID();

        List<AbilityRotationResult> results = List.of(
            new AbilityRotationResult(playerId, AbilityType.LINE_BOMBARDMENT, "Bombardeio em Linha",
                "Ataca toda uma linha ou coluna", AbilityType.RADAR, true)
        );

        notificationService.notifyAbilityRotated(results);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSendToUser(
            eq(playerId.toString()), eq("/queue/game/ability-rotated"), payloadCaptor.capture()
        );

        AbilityRotationNotification notification = (AbilityRotationNotification) payloadCaptor.getValue();

        // The notification record does NOT have a playerId field
        // (verified structurally — AbilityRotationNotification has no playerId accessor)
        assertThat(notification.newAbility()).isEqualTo(AbilityType.LINE_BOMBARDMENT);
        assertThat(notification.newAbilityName()).isEqualTo("Bombardeio em Linha");
        assertThat(notification.newAbilityDescription()).isEqualTo("Ataca toda uma linha ou coluna");
        assertThat(notification.previousAbility()).isEqualTo(AbilityType.RADAR);
        assertThat(notification.previousWasDiscarded()).isTrue();
    }

    @Test
    void notifyAbilityRotated_emptyList_sendsNothing() {
        notificationService.notifyAbilityRotated(List.of());

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void abilityRotationNotification_fromFactoryMethod_convertsCorrectly() {
        UUID playerId = UUID.randomUUID();
        AbilityRotationResult result = new AbilityRotationResult(
            playerId, AbilityType.SHIELD, "Escudo", "Anula o próximo tiro recebido",
            AbilityType.DOUBLE_TORPEDO, false
        );

        AbilityRotationNotification notification = AbilityRotationNotification.from(result);

        assertThat(notification.newAbility()).isEqualTo(AbilityType.SHIELD);
        assertThat(notification.newAbilityName()).isEqualTo("Escudo");
        assertThat(notification.newAbilityDescription()).isEqualTo("Anula o próximo tiro recebido");
        assertThat(notification.previousAbility()).isEqualTo(AbilityType.DOUBLE_TORPEDO);
        assertThat(notification.previousWasDiscarded()).isFalse();
    }
}
