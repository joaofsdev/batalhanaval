package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.*;
import com.softexpert.batalhanaval_api.dto.response.*;
import com.softexpert.batalhanaval_api.exception.GameNotFoundException;
import com.softexpert.batalhanaval_api.repository.BoardRepository;
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
    private final BoardRepository boardRepository;
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

    @Transactional
    public AdminGameBoardsResponse revealBoards(UUID gameId, User admin) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(GameNotFoundException::new);

        List<Board> boards = boardRepository.findByGameId(gameId);

        PlayerBoardSummary p1Summary = boards.stream()
            .filter(b -> b.getOwner().getId().equals(game.getPlayer1().getId()))
            .findFirst()
            .map(b -> new PlayerBoardSummary(
                game.getPlayer1().getId(),
                game.getPlayer1().getUsername(),
                buildBoardResponse(b)
            ))
            .orElse(new PlayerBoardSummary(
                game.getPlayer1().getId(),
                game.getPlayer1().getUsername(),
                null
            ));

        PlayerBoardSummary p2Summary = null;
        if (game.getPlayer2() != null) {
            p2Summary = boards.stream()
                .filter(b -> b.getOwner().getId().equals(game.getPlayer2().getId()))
                .findFirst()
                .map(b -> new PlayerBoardSummary(
                    game.getPlayer2().getId(),
                    game.getPlayer2().getUsername(),
                    buildBoardResponse(b)
                ))
                .orElse(new PlayerBoardSummary(
                    game.getPlayer2().getId(),
                    game.getPlayer2().getUsername(),
                    null
                ));
        }

        adminAuditService.log(admin, "BOARD_REVEALED", "GAME", gameId, null);

        return new AdminGameBoardsResponse(
            game.getId(),
            game.getStatus(),
            game.getGameMode(),
            p1Summary,
            p2Summary
        );
    }

    private BoardResponse buildBoardResponse(Board board) {
        List<ShipResponse> ships = board.getShips().stream()
            .map(s -> new ShipResponse(
                s.getShipType(), s.getOriginRow(), s.getOriginCol(),
                s.getOrientation(), s.getHits(), s.isSunk()
            ))
            .toList();

        List<CellResponse> cells = board.getCells().stream()
            .map(c -> new CellResponse(c.getRow(), c.getCol(), c.isHasShip(), c.isHit()))
            .toList();

        return new BoardResponse(board.isReady(), ships, cells);
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
