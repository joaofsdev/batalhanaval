package com.softexpert.batalhanaval_api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            if (token != null && jwtService.isTokenValid(token)) {
                UUID userId = jwtService.extractUserId(token);
                String username = jwtService.extractUsername(token);
                accessor.setUser(new StompPrincipal(userId, username));
            } else {
                throw new IllegalArgumentException("Invalid or missing JWT token");
            }
        }
        return message;
    }

    public record StompPrincipal(UUID userId, String username) implements Principal {
        @Override
        public String getName() {
            return userId.toString();
        }
    }
}
