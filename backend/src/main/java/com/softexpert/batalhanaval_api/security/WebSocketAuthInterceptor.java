package com.softexpert.batalhanaval_api.security;

import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.repository.GameRepository;
import com.softexpert.batalhanaval_api.repository.UserRepository;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    private static final Pattern GAME_TOPIC_PATTERN = Pattern.compile("^/topic/game/([a-f0-9\\-]+)/.*$");
    private static final Pattern ROOM_TOPIC_PATTERN = Pattern.compile("^/topic/room/([a-f0-9\\-]+)$");

    private static final int STOMP_SEND_LIMIT_PER_SECOND = 5;
    private final Map<UUID, Bucket> stompSendBuckets = new ConcurrentHashMap<>();

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribe(accessor);
        } else if (StompCommand.SEND.equals(accessor.getCommand())) {
            handleSend(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String token = accessor.getFirstNativeHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token != null && jwtService.isTokenValid(token)) {
            UUID userId = jwtService.extractUserId(token);
            String username = jwtService.extractUsername(token);

            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null || !user.isActive()) {
                throw new IllegalArgumentException("Conta suspensa ou banida");
            }

            accessor.setUser(new StompPrincipal(userId, username));
        } else {
            throw new IllegalArgumentException("Token JWT inválido ou ausente");
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) return;

        // User-specific queues are always allowed (session-scoped by Spring)
        if (destination.startsWith("/user/")) return;

        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new IllegalArgumentException("Não autenticado");
        }

        UUID userId = ((StompPrincipal) principal).userId();

        // Check game topic authorization
        Matcher gameMatcher = GAME_TOPIC_PATTERN.matcher(destination);
        if (gameMatcher.matches()) {
            UUID gameId = UUID.fromString(gameMatcher.group(1));
            if (!isGameParticipant(gameId, userId)) {
                throw new IllegalArgumentException("Não autorizado a observar esta partida");
            }
            return;
        }

        // Check room topic authorization
        Matcher roomMatcher = ROOM_TOPIC_PATTERN.matcher(destination);
        if (roomMatcher.matches()) {
            UUID gameId = UUID.fromString(roomMatcher.group(1));
            if (!isGameParticipant(gameId, userId)) {
                throw new IllegalArgumentException("Não autorizado a observar esta sala");
            }
            return;
        }

        // Any other topic not matching known patterns is denied
        throw new IllegalArgumentException("Destino de subscription não autorizado");
    }

    private void handleSend(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new IllegalArgumentException("Não autenticado");
        }

        UUID userId = ((StompPrincipal) principal).userId();
        Bucket bucket = stompSendBuckets.computeIfAbsent(userId, k ->
            Bucket.builder()
                .addLimit(Bandwidth.simple(STOMP_SEND_LIMIT_PER_SECOND, Duration.ofSeconds(1)))
                .build()
        );

        if (!bucket.tryConsume(1)) {
            throw new IllegalArgumentException("Rate limit excedido. Aguarde antes de enviar mais mensagens.");
        }
    }

    private boolean isGameParticipant(UUID gameId, UUID userId) {
        return gameRepository.findById(gameId)
            .map(game -> {
                UUID p1 = game.getPlayer1() != null ? game.getPlayer1().getId() : null;
                UUID p2 = game.getPlayer2() != null ? game.getPlayer2().getId() : null;
                return userId.equals(p1) || userId.equals(p2);
            })
            .orElse(false);
    }

    public record StompPrincipal(UUID userId, String username) implements Principal {
        @Override
        public String getName() {
            return userId.toString();
        }
    }
}
