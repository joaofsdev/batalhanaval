package com.softexpert.batalhanaval_api.controller;

import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.domain.ShipType;
import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.dto.request.PlaceShipsRequest;
import com.softexpert.batalhanaval_api.dto.response.FleetConfigResponse;
import com.softexpert.batalhanaval_api.dto.response.GameHistoryEntry;
import com.softexpert.batalhanaval_api.dto.response.GameResponse;
import com.softexpert.batalhanaval_api.dto.response.PageResponse;
import com.softexpert.batalhanaval_api.dto.response.PlaceShipsResponse;
import com.softexpert.batalhanaval_api.dto.response.RematchInvite;
import com.softexpert.batalhanaval_api.repository.UserRepository;
import com.softexpert.batalhanaval_api.service.GameService;
import com.softexpert.batalhanaval_api.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping("/fleet-config")
    public List<FleetConfigResponse> getFleetConfig() {
        return Arrays.stream(ShipType.values())
            .map(s -> new FleetConfigResponse(s.name(), s.getSize(), s.getDisplayName()))
            .toList();
    }

    @GetMapping("/active")
    public ResponseEntity<GameResponse> getActiveGame(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails);
        return gameService.getActiveGame(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/history")
    public PageResponse<GameHistoryEntry> getGameHistory(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = resolveUserId(userDetails);
        return gameService.getGameHistory(userId, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameResponse createOrJoinGame(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails);
        return gameService.createOrJoinGame(userId);
    }

    @GetMapping("/{id}")
    public GameResponse getGameState(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails);
        return gameService.getGameState(id, userId);
    }

    @PostMapping("/{id}/ships")
    public PlaceShipsResponse placeShips(
        @PathVariable UUID id,
        @Valid @RequestBody PlaceShipsRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = resolveUserId(userDetails);
        return gameService.placeShips(id, userId, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelGame(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails);
        gameService.cancelGame(id, userId);
    }

    @PostMapping("/{id}/surrender")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void surrender(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails);
        Game game = gameService.surrender(id, userId);
        notificationService.broadcastGameState(game);
    }

    @PostMapping("/{id}/rematch")
    @ResponseStatus(HttpStatus.CREATED)
    public GameResponse requestRematch(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails);
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        GameResponse newGame = gameService.requestRematch(id, userId);

        // Notify opponent about rematch invite
        UUID opponentId = gameService.getOpponentId(id, userId);
        if (opponentId != null) {
            notificationService.notifyRematchInvite(opponentId, new RematchInvite(newGame.id(), user.getUsername()));
        }

        return newGame;
    }

    private UUID resolveUserId(UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return user.getId();
    }
}
