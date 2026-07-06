package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.domain.GameStatus;
import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.dto.response.AdminGameResponse;
import com.softexpert.batalhanaval_api.dto.response.PageResponse;
import com.softexpert.batalhanaval_api.dto.response.PlayerSummary;
import com.softexpert.batalhanaval_api.exception.GameNotFoundException;
import com.softexpert.batalhanaval_api.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminGameService {

    private final GameRepository gameRepository;
    private final NotificationService notificationService;
    private final AdminAuditService adminAuditService;

    @Transactional(readOnly = true)
    public PageResponse<AdminGameResponse> listActiveGames(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Game> gamePage = gameRepository.findByStatusIn(
            List.of(GameStatus.WAITING, GameStatus.PLACING, GameStatus.IN_PROGRESS),
            pageRequest
        );

        var content = gamePage.getContent().stream()
            .map(this::toResponse)
            .toList();

        return new PageResponse<>(content, gamePage.getNumber(), gamePage.getSize(),
            gamePage.getTotalElements(), gamePage.getTotalPages());
    }

    @Transactional
    public AdminGameResponse forceEnd(UUID gameId, User admin) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(GameNotFoundException::new);

        if (game.getStatus() == GameStatus.FINISHED) {
            throw new GameNotFoundException();
        }

        game.setStatus(GameStatus.FINISHED);
        game.setWinner(null);
        game.setCurrentTurn(null);
        gameRepository.save(game);

        notificationService.broadcastGameState(game);

        adminAuditService.log(admin, "GAME_FORCE_ENDED", "GAME", gameId, null);

        return toResponse(game);
    }

    private AdminGameResponse toResponse(Game game) {
        PlayerSummary p1 = new PlayerSummary(game.getPlayer1().getId(), game.getPlayer1().getUsername());
        PlayerSummary p2 = game.getPlayer2() != null
            ? new PlayerSummary(game.getPlayer2().getId(), game.getPlayer2().getUsername())
            : null;

        return new AdminGameResponse(
            game.getId(),
            game.getStatus(),
            game.getGameMode(),
            p1,
            p2,
            game.getCreatedAt(),
            game.getUpdatedAt()
        );
    }
}
