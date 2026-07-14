package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.*;
import com.softexpert.batalhanaval_api.dto.response.StormEventNotification;
import com.softexpert.batalhanaval_api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class StormService {

    private final GameRepository gameRepository;
    private final StormEventRepository stormEventRepository;
    private final BoardRepository boardRepository;
    private final ShipRepository shipRepository;
    private final CellRepository cellRepository;

    @Transactional
    public StormEvent generateNextStormEvent(UUID gameId) {
        Game game = gameRepository.findById(gameId).orElseThrow();

        StormEventType eventType = randomStormType();
        String affectedAxis = null;

        if (eventType == StormEventType.TIDE) {
            int row = ThreadLocalRandom.current().nextInt(10);
            affectedAxis = "ROW_" + row;
        }

        StormEvent event = new StormEvent();
        event.setGame(game);
        event.setTurnNumber(game.getCurrentTurnNumber());
        event.setEventType(eventType);
        event.setAffectedAxis(affectedAxis);
        event.setResolved(false);

        stormEventRepository.save(event);

        int nextInterval = 3 + ThreadLocalRandom.current().nextInt(4);
        game.setNextStormTurn(game.getCurrentTurnNumber() + nextInterval);
        gameRepository.save(game);

        log.info("Evento de tempestade gerado: partida={}, turno={}, tipo={}, eixo={}, próximaTempestadeEm={}",
            gameId, game.getCurrentTurnNumber(), eventType, affectedAxis, nextInterval);

        return event;
    }

    @Transactional
    public StormEventNotification resolveStormEvent(UUID gameId) {
        Game game = gameRepository.findById(gameId).orElseThrow();

        StormEvent event = stormEventRepository.findByGameIdAndResolvedFalse(gameId)
            .orElse(null);

        if (event == null) return null;

        Boolean shipMoved = null;

        switch (event.getEventType()) {
            case FOG -> resolveFog(game);
            case TIDE -> resolveTide(game, event);
            case CURRENT -> shipMoved = resolveCurrent(game);
            case CALM -> resolveCalm(game);
        }

        event.setResolved(true);
        stormEventRepository.save(event);
        gameRepository.save(game);

        String message = buildStormMessage(event, shipMoved);
        log.info("Tempestade resolvida: partida={}, tipo={}, mensagem={}", gameId, event.getEventType(), message);

        return new StormEventNotification(event.getEventType(), event.getAffectedAxis(), message, shipMoved);
    }

    public boolean isStormTurn(UUID gameId, int turnNumber) {
        return stormEventRepository.findByGameIdAndTurnNumber(gameId, turnNumber).isPresent();
    }

    public boolean isShotBlockedByTide(UUID gameId, int row) {
        Game game = gameRepository.findById(gameId).orElseThrow();
        int currentTurn = game.getCurrentTurnNumber();

        Optional<StormEvent> unresolved = stormEventRepository.findByGameIdAndResolvedFalse(gameId)
            .filter(e -> e.getEventType() == StormEventType.TIDE)
            .filter(e -> e.getAffectedAxis() != null && e.getAffectedAxis().equals("ROW_" + row));
        if (unresolved.isPresent()) return true;

        return stormEventRepository.findLastResolvedByType(gameId, StormEventType.TIDE)
            .filter(e -> e.getAffectedAxis() != null && e.getAffectedAxis().equals("ROW_" + row))
            .filter(e -> currentTurn <= e.getTurnNumber() + 1)
            .isPresent();
    }

    public void clearExpiredEffects(Game game) {
        int currentTurn = game.getCurrentTurnNumber();

        if (game.isFogActive()) {
            stormEventRepository.findLastResolvedByType(game.getId(), StormEventType.FOG)
                .ifPresent(event -> {
                    if (currentTurn > event.getTurnNumber() + 3) {
                        game.setFogActive(false);
                    }
                });
        }
    }

    private void resolveFog(Game game) {
        game.setFogActive(true);
    }

    private void resolveTide(Game game, StormEvent event) {
    }

    private boolean resolveCurrent(Game game) {
        boolean anyMoved = false;
        List<Board> boards = boardRepository.findByGameId(game.getId());
        for (Board board : boards) {
            List<Ship> ships = shipRepository.findAllByBoardId(board.getId());
            List<Ship> eligibleShips = ships.stream()
                .filter(s -> !s.isSunk())
                .filter(s -> s.getHits() == 0)
                .toList();
            if (eligibleShips.isEmpty()) continue;

            Ship shipToMove = eligibleShips.get(ThreadLocalRandom.current().nextInt(eligibleShips.size()));
            if (tryMoveShip(board, shipToMove, ships)) {
                anyMoved = true;
            }
        }
        return anyMoved;
    }

    private void resolveCalm(Game game) {
        game.setBonusShot(true);
    }

    private boolean tryMoveShip(Board board, Ship ship, List<Ship> allShips) {
        List<int[]> currentCoords = getShipCoordinates(ship);

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        List<int[]> shuffledDirs = new ArrayList<>(Arrays.asList(directions));
        Collections.shuffle(shuffledDirs);

        for (int[] dir : shuffledDirs) {
            List<int[]> newCoords = currentCoords.stream()
                .map(c -> new int[]{c[0] + dir[0], c[1] + dir[1]})
                .toList();

            if (isValidMove(newCoords, ship, allShips, board)) {
                applyShipMove(board, ship, currentCoords, newCoords, dir);
                return true;
            }
        }
        return false;
    }

    private List<int[]> getShipCoordinates(Ship ship) {
        List<int[]> coords = new ArrayList<>();
        int size = ship.getShipType().getSize();
        for (int i = 0; i < size; i++) {
            int row = ship.getOriginRow() + (ship.getOrientation() == Orientation.VERTICAL ? i : 0);
            int col = ship.getOriginCol() + (ship.getOrientation() == Orientation.HORIZONTAL ? i : 0);
            coords.add(new int[]{row, col});
        }
        return coords;
    }

    private boolean isValidMove(List<int[]> newCoords, Ship movingShip, List<Ship> allShips, Board board) {
        Set<String> otherShipCells = new HashSet<>();
        for (Ship s : allShips) {
            if (s.getId().equals(movingShip.getId())) continue;
            for (int[] c : getShipCoordinates(s)) {
                otherShipCells.add(c[0] + "," + c[1]);
            }
        }

        for (int[] coord : newCoords) {
            if (coord[0] < 0 || coord[0] > 9 || coord[1] < 0 || coord[1] > 9) return false;
            if (otherShipCells.contains(coord[0] + "," + coord[1])) return false;

            Optional<Cell> cell = cellRepository.findByBoardIdAndRowAndCol(board.getId(), coord[0], coord[1]);
            if (cell.isPresent() && cell.get().isHit()) return false;
        }
        return true;
    }

    private void applyShipMove(Board board, Ship ship, List<int[]> oldCoords, List<int[]> newCoords, int[] direction) {
        for (int[] coord : oldCoords) {
            cellRepository.findByBoardIdAndRowAndCol(board.getId(), coord[0], coord[1])
                .ifPresent(cell -> {
                    cell.setHasShip(false);
                    cell.setShip(null);
                });
        }

        ship.setOriginRow(ship.getOriginRow() + direction[0]);
        ship.setOriginCol(ship.getOriginCol() + direction[1]);
        shipRepository.save(ship);

        for (int[] coord : newCoords) {
            cellRepository.findByBoardIdAndRowAndCol(board.getId(), coord[0], coord[1])
                .ifPresent(cell -> {
                    cell.setHasShip(true);
                    cell.setShip(ship);
                });
        }
    }

    private StormEventType randomStormType() {
        StormEventType[] types = StormEventType.values();
        return types[ThreadLocalRandom.current().nextInt(types.length)];
    }

    private String buildStormMessage(StormEvent event, Boolean shipMoved) {
        return switch (event.getEventType()) {
            case FOG -> "Nevoeiro! Resultados dos tiros ficam ocultos por 4 turnos.";
            case TIDE -> "Maré Alta! Linha " + (char) ('A' + Integer.parseInt(event.getAffectedAxis().replace("ROW_", ""))) + " está inacessível por 2 turnos.";
            case CURRENT -> Boolean.TRUE.equals(shipMoved)
                ? "Corrente Marítima! Navios se deslocaram 1 célula."
                : "Corrente Marítima passou, mas nenhum navio pôde se mover.";
            case CALM -> "Calmaria! Ambos os jogadores ganham um tiro bônus neste turno.";
        };
    }
}
