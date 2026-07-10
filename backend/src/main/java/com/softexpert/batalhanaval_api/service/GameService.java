package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.*;
import com.softexpert.batalhanaval_api.dto.request.PlaceShipsRequest;
import com.softexpert.batalhanaval_api.dto.response.*;
import com.softexpert.batalhanaval_api.exception.*;
import com.softexpert.batalhanaval_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final ShotRepository shotRepository;
    private final PlacementService placementService;
    private final AbilityService abilityService;
    private final EloService eloService;

    private final Map<UUID, UUID> pendingRematches = new ConcurrentHashMap<>();

    @Transactional
    public GameResponse createOrJoinGame(UUID userId, GameMode gameMode) {
        gameRepository.findActiveGameByUserId(userId, List.of(GameStatus.WAITING, GameStatus.PLACING, GameStatus.IN_PROGRESS))
            .ifPresent(g -> { throw new PlayerAlreadyInGameException(); });

        User user = userRepository.findById(userId).orElseThrow();

        Optional<Game> waitingGame = gameRepository.findFirstWaitingGameByModeForUpdate(GameStatus.WAITING, userId, gameMode);

        if (waitingGame.isPresent()) {
            Game game = waitingGame.get();
            game.setPlayer2(user);
            game.setStatus(GameStatus.PLACING);
            createBoardForPlayer(game, user);
            gameRepository.save(game);
            return buildGameResponse(game, userId);
        }

        Game game = new Game();
        game.setPlayer1(user);
        game.setStatus(GameStatus.WAITING);
        game.setGameMode(gameMode);
        game.setRanked(true);
        game = gameRepository.save(game);
        createBoardForPlayer(game, user);
        return buildGameResponse(game, userId);
    }

    @Transactional
    public PlaceShipsResponse placeShips(UUID gameId, UUID userId, PlaceShipsRequest request) {
        Game game = gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);
        validateParticipant(game, userId);

        if (game.getStatus() != GameStatus.PLACING) {
            throw new GameNotInPlacingException();
        }

        Board board = boardRepository.findByGameIdAndOwnerId(gameId, userId)
            .orElseThrow(GameNotFoundException::new);

        if (board.isReady()) {
            throw new BoardAlreadyReadyException();
        }

        board.getShips().size();
        board.getCells().size();

        placementService.validateAndPlaceShips(board, request.ships());
        boardRepository.saveAndFlush(board);

        List<Board> allBoards = boardRepository.findByGameId(gameId);
        boolean bothReady = allBoards.size() == 2 && allBoards.stream().allMatch(Board::isReady);

        if (bothReady) {
            game.setStatus(GameStatus.IN_PROGRESS);
            User player1 = userRepository.findById(game.getPlayer1().getId()).orElseThrow();
            game.setCurrentTurn(player1);

            if (game.getGameMode() == GameMode.STORM) {
                abilityService.initializeAbilities(game);
            }
        }

        gameRepository.save(game);

        return new PlaceShipsResponse("Fleet placed successfully", true, game.getStatus());
    }

    @Transactional(readOnly = true)
    public GameResponse getGameState(UUID gameId, UUID userId) {
        Game game = gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);
        validateParticipant(game, userId);
        return buildGameResponse(game, userId);
    }

    @Transactional(readOnly = true)
    public Optional<GameResponse> getActiveGame(UUID userId) {
        return gameRepository.findActiveGameByUserId(userId, List.of(GameStatus.WAITING, GameStatus.PLACING, GameStatus.IN_PROGRESS))
            .map(game -> buildGameResponse(game, userId));
    }

    @Transactional
    public Game surrender(UUID gameId, UUID userId) {
        Game game = gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);
        validateParticipant(game, userId);

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new GameNotInProgressException();
        }

        User winner = game.getPlayer1().getId().equals(userId)
            ? game.getPlayer2()
            : game.getPlayer1();

        game.setStatus(GameStatus.FINISHED);
        game.setWinner(winner);
        game.setCurrentTurn(null);
        eloService.updateElo(game);
        gameRepository.save(game);
        return game;
    }

    @Transactional
    public RematchResponse requestRematch(UUID originalGameId, UUID userId) {
        Game originalGame = gameRepository.findById(originalGameId).orElseThrow(GameNotFoundException::new);
        validateParticipant(originalGame, userId);

        if (originalGame.getStatus() != GameStatus.FINISHED) {
            throw new GameNotInProgressException();
        }

        gameRepository.findActiveGameByUserId(userId, List.of(GameStatus.WAITING, GameStatus.PLACING, GameStatus.IN_PROGRESS))
            .ifPresent(g -> { throw new PlayerAlreadyInGameException(); });

        User user = userRepository.findById(userId).orElseThrow();
        GameMode mode = originalGame.getGameMode();

        final UUID[] matchedWith = {null};

        UUID remaining = pendingRematches.compute(originalGameId, (key, existingRequesterId) -> {
            if (existingRequesterId == null) {
                return userId;
            }

            if (existingRequesterId.equals(userId)) {
                return userId;
            }

            boolean firstRequesterBusy = gameRepository.findActiveGameByUserId(
                existingRequesterId, List.of(GameStatus.WAITING, GameStatus.PLACING, GameStatus.IN_PROGRESS)
            ).isPresent();

            if (firstRequesterBusy) {
                return userId;
            }

            matchedWith[0] = existingRequesterId;
            return null;
        });

        if (matchedWith[0] != null) {
            User firstRequester = userRepository.findById(matchedWith[0]).orElseThrow();

            Game newGame = new Game();
            newGame.setPlayer1(firstRequester);
            newGame.setPlayer2(user);
            newGame.setStatus(GameStatus.PLACING);
            newGame.setGameMode(mode);
            newGame.setRanked(originalGame.isRanked());
            newGame = gameRepository.save(newGame);
            createBoardForPlayer(newGame, firstRequester);
            createBoardForPlayer(newGame, user);

            return new RematchResponse(RematchResponse.RematchStatus.MATCHED, newGame.getId());
        }

        return new RematchResponse(RematchResponse.RematchStatus.WAITING, null);
    }

    public void cancelPendingRematch(UUID originalGameId, UUID userId) {
        pendingRematches.remove(originalGameId, userId);
    }

    public Map<UUID, UUID> getPendingRematches() {
        return pendingRematches;
    }

    public UUID getOpponentId(UUID gameId, UUID userId) {
        Game game = gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);
        if (game.getPlayer1().getId().equals(userId)) {
            return game.getPlayer2() != null ? game.getPlayer2().getId() : null;
        }
        return game.getPlayer1().getId();
    }

    @Transactional(readOnly = true)
    public PageResponse<GameHistoryEntry> getGameHistory(UUID userId, int page, int size) {
        org.springframework.data.domain.Page<Game> gamePage = gameRepository.findFinishedGamesByUserId(
            userId, org.springframework.data.domain.PageRequest.of(page, size));

        List<GameHistoryEntry> entries = gamePage.getContent().stream().map(game -> {
            String opponentUsername;
            if (game.getPlayer1().getId().equals(userId)) {
                opponentUsername = game.getPlayer2() != null ? game.getPlayer2().getUsername() : "Desconhecido";
            } else {
                opponentUsername = game.getPlayer1().getUsername();
            }

            boolean won = game.getWinner() != null && game.getWinner().getId().equals(userId);
            long durationSeconds = java.time.Duration.between(game.getCreatedAt(), game.getUpdatedAt()).getSeconds();

            return new GameHistoryEntry(game.getId(), opponentUsername, game.getStatus(), won, durationSeconds, game.getUpdatedAt());
        }).toList();

        return new PageResponse<>(entries, page, size, gamePage.getTotalElements(), gamePage.getTotalPages());
    }

    @Transactional
    public void cancelGame(UUID gameId, UUID userId) {
        Game game = gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);

        if (!game.getPlayer1().getId().equals(userId)) {
            throw new NotGameParticipantException();
        }

        if (game.getStatus() == GameStatus.WAITING) {
            gameRepository.delete(game);
            gameRepository.flush();
            return;
        }

        if (game.getStatus() == GameStatus.PLACING) {
            Board player1Board = boardRepository.findByGameAndOwner(game, game.getPlayer1()).orElse(null);

            if (player1Board != null && !player1Board.isReady()) {
                User player2 = game.getPlayer2();

                boardRepository.delete(player1Board);

                game.setPlayer1(player2);
                game.setPlayer2(null);
                game.setStatus(GameStatus.WAITING);
                game.setCurrentTurn(null);
                gameRepository.save(game);
                gameRepository.flush();
                return;
            }
        }

        throw new GameCannotBeCancelledException();
    }

    private void createBoardForPlayer(Game game, User player) {
        Board board = new Board();
        board.setGame(game);
        board.setOwner(player);
        board.setReady(false);

        List<Cell> cells = new ArrayList<>();
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                Cell cell = new Cell();
                cell.setBoard(board);
                cell.setRow(r);
                cell.setCol(c);
                cell.setHasShip(false);
                cell.setHit(false);
                cells.add(cell);
            }
        }
        board.setCells(cells);
        boardRepository.save(board);
    }

    private void validateParticipant(Game game, UUID userId) {
        boolean isPlayer1 = game.getPlayer1().getId().equals(userId);
        boolean isPlayer2 = game.getPlayer2() != null && game.getPlayer2().getId().equals(userId);
        if (!isPlayer1 && !isPlayer2) {
            throw new NotGameParticipantException();
        }
    }

    private GameResponse buildGameResponse(Game game, UUID userId) {
        PlayerSummary p1 = new PlayerSummary(game.getPlayer1().getId(), game.getPlayer1().getUsername());
        PlayerSummary p2 = game.getPlayer2() != null
            ? new PlayerSummary(game.getPlayer2().getId(), game.getPlayer2().getUsername())
            : null;

        UUID currentTurnId = game.getCurrentTurn() != null ? game.getCurrentTurn().getId() : null;
        UUID winnerId = game.getWinner() != null ? game.getWinner().getId() : null;

        BoardResponse myBoard = buildMyBoard(game, userId);
        OpponentBoardResponse opponentBoard = buildOpponentBoard(game, userId);

        Integer eloDelta = calculateEloDelta(game, userId);

        return new GameResponse(game.getId(), game.getStatus(), game.getGameMode(), p1, p2, currentTurnId, winnerId, myBoard, opponentBoard, eloDelta, game.getCreatedAt());
    }

    private BoardResponse buildMyBoard(Game game, UUID userId) {
        Board board = boardRepository.findByGameIdAndOwnerId(game.getId(), userId).orElse(null);

        if (board == null) return null;

        List<ShipResponse> ships = board.getShips().stream()
            .map(s -> new ShipResponse(s.getShipType(), s.getOriginRow(), s.getOriginCol(), s.getOrientation(), s.getHits(), s.isSunk()))
            .toList();

        List<CellResponse> cells = board.getCells().stream()
            .map(c -> new CellResponse(c.getRow(), c.getCol(), c.isHasShip(), c.isHit()))
            .toList();

        return new BoardResponse(board.isReady(), ships, cells);
    }

    private OpponentBoardResponse buildOpponentBoard(Game game, UUID userId) {
        List<Shot> myShots = shotRepository.findAllByGameIdAndAttackerId(game.getId(), userId);
        List<ShotSummary> shots = myShots.stream()
            .map(s -> new ShotSummary(s.getRow(), s.getCol(), s.getResult(), s.getSunkShipType(), s.getSunkShipOriginRow(), s.getSunkShipOriginCol(), s.getSunkShipOrientation()))
            .toList();

        List<ShipResponse> opponentShips = null;
        if (game.getStatus() == GameStatus.FINISHED) {
            UUID opponentId = game.getPlayer1().getId().equals(userId)
                ? game.getPlayer2().getId()
                : game.getPlayer1().getId();
            Board opponentBoard = boardRepository.findByGameIdAndOwnerId(game.getId(), opponentId).orElse(null);
            if (opponentBoard != null) {
                opponentShips = opponentBoard.getShips().stream()
                    .map(s -> new ShipResponse(s.getShipType(), s.getOriginRow(), s.getOriginCol(), s.getOrientation(), s.getHits(), s.isSunk()))
                    .toList();
            }
        }

        return new OpponentBoardResponse(shots, opponentShips);
    }

    private Integer calculateEloDelta(Game game, UUID userId) {
        if (game.getStatus() != GameStatus.FINISHED || game.getWinner() == null) {
            return null;
        }

        boolean isPlayer1 = game.getPlayer1().getId().equals(userId);
        Integer eloBefore = isPlayer1 ? game.getPlayer1EloBefore() : game.getPlayer2EloBefore();

        if (eloBefore == null) {
            return null;
        }

        User player = isPlayer1 ? game.getPlayer1() : game.getPlayer2();
        return player.getEloRating() - eloBefore;
    }
}
