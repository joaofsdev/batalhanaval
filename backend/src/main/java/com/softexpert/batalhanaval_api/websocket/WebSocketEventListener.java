package com.softexpert.batalhanaval_api.websocket;

import com.softexpert.batalhanaval_api.security.WebSocketAuthInterceptor.StompPrincipal;
import com.softexpert.batalhanaval_api.service.DisconnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final DisconnectionService disconnectionService;

    private final Map<UUID, Set<String>> activeSessions = new ConcurrentHashMap<>();

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        String sessionId = accessor.getSessionId();
        if (principal instanceof StompPrincipal stompPrincipal && sessionId != null) {
            UUID userId = stompPrincipal.userId();
            activeSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
            log.debug("Sessão WebSocket conectada para usuário {}. SessionId={}. Sessões ativas: {}",
                userId, sessionId, activeSessions.get(userId).size());
            disconnectionService.handleReconnect(userId);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        String sessionId = accessor.getSessionId();
        if (principal instanceof StompPrincipal stompPrincipal && sessionId != null) {
            UUID userId = stompPrincipal.userId();
            Set<String> sessions = activeSessions.get(userId);

            if (sessions != null) {
                sessions.remove(sessionId);
                log.debug("Sessão WebSocket desconectada para usuário {}. SessionId={}. Sessões ativas: {}",
                    userId, sessionId, sessions.size());

                if (sessions.isEmpty()) {
                    activeSessions.remove(userId);
                    disconnectionService.handleDisconnect(userId);
                }
            }
        }
    }
}
