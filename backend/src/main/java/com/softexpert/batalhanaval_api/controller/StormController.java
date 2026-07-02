package com.softexpert.batalhanaval_api.controller;

import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.dto.response.StormInfoResponse;
import com.softexpert.batalhanaval_api.exception.GameNotFoundException;
import com.softexpert.batalhanaval_api.repository.GameRepository;
import com.softexpert.batalhanaval_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/games/{gameId}/storm")
@RequiredArgsConstructor
public class StormController {

    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    @GetMapping("/next")
    public StormInfoResponse getNextStorm(
        @PathVariable UUID gameId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        Game game = gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);

        int turnsUntilStorm = Math.max(0, game.getNextStormTurn() - game.getCurrentTurnNumber());

        return new StormInfoResponse(
            game.getNextStormTurn(),
            game.getCurrentTurnNumber(),
            turnsUntilStorm
        );
    }
}
