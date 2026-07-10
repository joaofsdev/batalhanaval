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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final DisconnectionService disconnectionService;

    private final Map<UUID, AtomicInteger> activeSessionCounts = new ConcurrentHashMap<>();

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal instanceof StompPrincipal stompPrincipal) {
            UUID userId = stompPrincipal.userId();
            activeSessionCounts.computeIfAbsent(userId, k -> new AtomicInteger(0)).incrementAndGet();
            log.debug("WebSocket session connected for user {}. Active sessions: {}",
                userId, activeSessionCounts.get(userId).get());
            disconnectionService.handleReconnect(userId);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal instanceof StompPrincipal stompPrincipal) {
            UUID userId = stompPrincipal.userId();
            AtomicInteger count = activeSessionCounts.get(userId);
            int remaining = (count != null) ? count.decrementAndGet() : 0;
            log.debug("WebSocket session disconnected for user {}. Active sessions: {}",
                userId, remaining);

            if (remaining <= 0) {
                activeSessionCounts.remove(userId);
                disconnectionService.handleDisconnect(userId);
            }
        }
    }
}
