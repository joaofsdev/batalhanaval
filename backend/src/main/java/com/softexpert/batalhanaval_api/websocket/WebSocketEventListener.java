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

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final DisconnectionService disconnectionService;

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal instanceof StompPrincipal stompPrincipal) {
            disconnectionService.handleReconnect(stompPrincipal.userId());
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal instanceof StompPrincipal stompPrincipal) {
            disconnectionService.handleDisconnect(stompPrincipal.userId());
        }
    }
}
