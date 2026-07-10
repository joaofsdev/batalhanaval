package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.*;
import com.softexpert.batalhanaval_api.dto.response.RoomResponse;
import com.softexpert.batalhanaval_api.exception.*;
import com.softexpert.batalhanaval_api.repository.BoardRepository;
import com.softexpert.batalhanaval_api.repository.GameRepository;
import com.softexpert.batalhanaval_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final GameRepository gameRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public RoomResponse createRoom(UUID userId, GameMode gameMode) {
        gameRepository.findActiveGameByUserId(userId, List.of(GameStatus.WAITING, GameStatus.PLACING, GameStatus.IN_PROGRESS))
            .ifPresent(g -> { throw new PlayerAlreadyInGameException(); });

        User user = userRepository.findById(userId).orElseThrow();

        String token = generateToken();

        Game game = new Game();
        game.setPlayer1(user);
        game.setStatus(GameStatus.WAITING);
        game.setGameMode(gameMode);
        game.setPrivateRoom(true);
        game.setRanked(false);
        game.setRoomToken(token);
        game = gameRepository.save(game);

        createBoardForPlayer(game, user);

        return buildRoomResponse(game);
    }

    @Transactional
    public RoomResponse joinRoom(UUID userId, String token) {
        gameRepository.findActiveGameByUserId(userId, List.of(GameStatus.WAITING, GameStatus.PLACING, GameStatus.IN_PROGRESS))
            .ifPresent(g -> { throw new PlayerAlreadyInGameException(); });

        Game game = gameRepository.findByRoomToken(token.toUpperCase())
            .orElseThrow(() -> new RoomException("Sala não encontrada com este token.", "ROOM_NOT_FOUND"));

        if (game.getStatus() != GameStatus.WAITING) {
            throw new RoomException("Esta sala já está cheia ou a partida já começou.", "ROOM_FULL");
        }

        if (game.getPlayer1().getId().equals(userId)) {
            throw new RoomException("Você não pode entrar na sua própria sala.", "ROOM_OWN");
        }

        User user = userRepository.findById(userId).orElseThrow();
        game.setPlayer2(user);
        game.setStatus(GameStatus.PLACING);
        createBoardForPlayer(game, user);
        gameRepository.save(game);

        RoomResponse response = buildRoomResponse(game);
        notificationService.notifyRoomUpdate(game.getId(), response);

        return response;
    }

    @Transactional
    public RoomResponse confirmReady(UUID gameId, UUID userId) {
        Game game = gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);

        if (!game.isPrivateRoom()) {
            throw new RoomException("Esta partida não é uma sala privada.", "NOT_PRIVATE_ROOM");
        }

        boolean isPlayer1 = game.getPlayer1().getId().equals(userId);
        boolean isPlayer2 = game.getPlayer2() != null && game.getPlayer2().getId().equals(userId);

        if (!isPlayer1 && !isPlayer2) {
            throw new NotGameParticipantException();
        }

        if (game.getPlayer2() == null) {
            throw new RoomException("Aguardando oponente entrar na sala.", "ROOM_NOT_FULL");
        }

        if (isPlayer1) {
            game.setPlayer1Ready(true);
        } else {
            game.setPlayer2Ready(true);
        }

        gameRepository.save(game);

        RoomResponse response = buildRoomResponse(game);
        notificationService.notifyRoomUpdate(game.getId(), response);

        return response;
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomState(UUID gameId, UUID userId) {
        Game game = gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);

        boolean isPlayer1 = game.getPlayer1().getId().equals(userId);
        boolean isPlayer2 = game.getPlayer2() != null && game.getPlayer2().getId().equals(userId);

        if (!isPlayer1 && !isPlayer2) {
            throw new NotGameParticipantException();
        }

        return buildRoomResponse(game);
    }

    @Transactional
    public void cancelRoom(UUID gameId, UUID userId) {
        Game game = gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);

        if (!game.isPrivateRoom()) {
            throw new RoomException("Esta partida não é uma sala privada.", "NOT_PRIVATE_ROOM");
        }

        if (!game.getPlayer1().getId().equals(userId)) {
            throw new RoomException("Apenas o criador da sala pode cancelá-la.", "NOT_HOST");
        }

        if (game.getStatus() == GameStatus.IN_PROGRESS) {
            throw new RoomException("A partida já começou e não pode ser cancelada.", "GAME_STARTED");
        }

        gameRepository.delete(game);
        gameRepository.flush();

        notificationService.notifyRoomCancelled(gameId);
    }

    private RoomResponse buildRoomResponse(Game game) {
        String hostUsername = game.getPlayer1().getUsername();
        String guestUsername = game.getPlayer2() != null ? game.getPlayer2().getUsername() : null;

        RoomResponse.RoomStatus status;
        if (game.getPlayer2() == null) {
            status = RoomResponse.RoomStatus.WAITING_OPPONENT;
        } else if (!game.isPlayer1Ready() || !game.isPlayer2Ready()) {
            status = RoomResponse.RoomStatus.WAITING_CONFIRMATION;
        } else {
            status = RoomResponse.RoomStatus.STARTING;
        }

        return new RoomResponse(
            game.getId(),
            game.getRoomToken(),
            game.getGameMode(),
            hostUsername,
            guestUsername,
            game.isPlayer1Ready(),
            game.isPlayer2Ready(),
            status
        );
    }

    private String generateToken() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
        }
        String token = sb.toString();

        if (gameRepository.findByRoomToken(token).isPresent()) {
            return generateToken();
        }
        return token;
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
}
