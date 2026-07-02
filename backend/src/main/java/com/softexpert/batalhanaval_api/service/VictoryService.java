package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.*;
import com.softexpert.batalhanaval_api.repository.GameRepository;
import com.softexpert.batalhanaval_api.repository.ShipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VictoryService {

    private final ShipRepository shipRepository;
    private final GameRepository gameRepository;

    /**
     * Check if all ships on the target board are sunk.
     * If so, mark the game as FINISHED and set the winner.
     *
     * @return true if the game is now finished (all defender ships sunk)
     */
    @Transactional
    public boolean checkVictoryCondition(Game game, UUID attackerId, Board targetBoard) {
        boolean allSunk = shipRepository.findAllByBoardId(targetBoard.getId()).stream().allMatch(Ship::isSunk);
        if (allSunk) {
            game.setStatus(GameStatus.FINISHED);
            game.setWinner(game.getPlayer1().getId().equals(attackerId) ? game.getPlayer1() : game.getPlayer2());
            game.setCurrentTurn(null);
            gameRepository.save(game);
            return true;
        }
        return false;
    }
}
