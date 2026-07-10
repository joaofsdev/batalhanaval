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
    private final EloService eloService;

    @Transactional
    public boolean checkVictoryCondition(Game game, UUID attackerId, Board targetBoard) {
        boolean allSunk = shipRepository.findAllByBoardId(targetBoard.getId()).stream().allMatch(Ship::isSunk);
        if (allSunk) {
            game.setStatus(GameStatus.FINISHED);
            game.setWinner(game.getPlayer1().getId().equals(attackerId) ? game.getPlayer1() : game.getPlayer2());
            game.setCurrentTurn(null);
            eloService.updateElo(game);
            gameRepository.save(game);
            return true;
        }
        return false;
    }
}
