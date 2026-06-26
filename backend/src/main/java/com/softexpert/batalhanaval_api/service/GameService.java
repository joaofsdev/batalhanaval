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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final ShotRepository shotRepository;
    private final PlacementService placementService;

    @Transactional
    public GameResponse createOrJoinGame(UUID userId) {
        gameRepository.findActiveGameByUserId(userId, List.of(GameStatus.WAITING, GameStatus.PLACING, GameStatus.IN_PROGRESS))
            .ifPresent(g -> { throw new PlayerAlreadyInGameException(); });

        User user = userRepository.findById(userId).orElseThrow();

        List<Game> waitingGames = gameRepository.findByStatusAndPlayer1IdNot(GameStatus.WAITING, userId);

        if (!waitingGames.isEmpty()) {
            Game game = waitingGames.getFirst();
            game.setPlayer2(user);
            game.setStatus(GameStatus.PLACING);
            createBoardForPlayer(game, user);
            gameRepository.save(game);
            return buildGameResponse(game, userId);
        }

        Game game = new Game();
        game.setPlayer1(user);
        game.setStatus(GameStatus.WAITING);
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

        // Force initialize lazy collections before passing to placement service
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
            gameRepository.save(game);
        }

        return new PlaceShipsResponse("Fleet placed successfully", true, game.getStatus());
    }

    @Transactional(readOnly = true)
    public GameResponse getGameState(UUID gameId, UUID userId) {
        Game game = gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);
        validateParticipant(game, userId);
        return buildGameResponse(game, userId);
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

        return new GameResponse(game.getId(), game.getStatus(), p1, p2, currentTurnId, winnerId, myBoard, opponentBoard, game.getCreatedAt());
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
            .map(s -> new ShotSummary(s.getRow(), s.getCol(), s.getResult(), s.getSunkShipType()))
            .toList();
        return new OpponentBoardResponse(shots);
    }
}
