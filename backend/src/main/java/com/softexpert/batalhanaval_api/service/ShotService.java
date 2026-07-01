package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.*;
import com.softexpert.batalhanaval_api.dto.response.ShotResultResponse;
import com.softexpert.batalhanaval_api.exception.*;
import com.softexpert.batalhanaval_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShotService {

    private final GameRepository gameRepository;
    private final BoardRepository boardRepository;
    private final CellRepository cellRepository;
    private final ShipRepository shipRepository;
    private final ShotRepository shotRepository;

    @Transactional
    public ShotResultResponse processShot(UUID gameId, UUID attackerId, int row, int col) {
        Game game = gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new GameNotInProgressException();
        }
        if (!game.getCurrentTurn().getId().equals(attackerId)) {
            throw new NotYourTurnException();
        }

        UUID defenderId = game.getPlayer1().getId().equals(attackerId)
            ? game.getPlayer2().getId()
            : game.getPlayer1().getId();

        Board targetBoard = boardRepository.findByGameIdAndOwnerId(gameId, defenderId)
            .orElseThrow(GameNotFoundException::new);

        Cell cell = cellRepository.findByBoardIdAndRowAndCol(targetBoard.getId(), row, col)
            .orElseThrow(() -> new InvalidShipPlacementException("Invalid coordinates", "INVALID_COORDINATES"));

        if (cell.isHit()) {
            throw new CellAlreadyAttackedException();
        }

        cell.setHit(true);

        ShotResult result;
        ShipType sunkShipType = null;

        if (cell.isHasShip()) {
            Ship ship = cell.getShip();
            ship.setHits(ship.getHits() + 1);
            shipRepository.saveAndFlush(ship);
            if (ship.isSunk()) {
                result = ShotResult.SUNK;
                sunkShipType = ship.getShipType();
            } else {
                result = ShotResult.HIT;
            }
        } else {
            result = ShotResult.MISS;
        }

        Shot shot = new Shot();
        shot.setGame(game);
        shot.setAttacker(game.getCurrentTurn());
        shot.setTargetBoard(targetBoard);
        shot.setRow(row);
        shot.setCol(col);
        shot.setResult(result);
        shot.setSunkShipType(sunkShipType);
        shotRepository.save(shot);

        // Alternate turn
        User nextTurn = game.getPlayer1().getId().equals(attackerId)
            ? game.getPlayer2()
            : game.getPlayer1();
        game.setCurrentTurn(nextTurn);
        game.setConsecutiveSkips(0);

        // Check victory - ensure ship hits are persisted before query
        boolean allSunk = shipRepository.findAllByBoardId(targetBoard.getId()).stream().allMatch(Ship::isSunk);
        if (allSunk) {
            game.setStatus(GameStatus.FINISHED);
            game.setWinner(game.getPlayer1().getId().equals(attackerId) ? game.getPlayer1() : game.getPlayer2());
            game.setCurrentTurn(null);
        }

        gameRepository.save(game);

        return new ShotResultResponse(gameId, row, col, result, sunkShipType);
    }

    public UUID getDefenderId(Game game, UUID attackerId) {
        return game.getPlayer1().getId().equals(attackerId)
            ? game.getPlayer2().getId()
            : game.getPlayer1().getId();
    }
}
