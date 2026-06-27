package com.softexpert.batalhanaval_api.repository;

import com.softexpert.batalhanaval_api.domain.Board;
import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoardRepository extends JpaRepository<Board, UUID> {

    Optional<Board> findByGameIdAndOwnerId(UUID gameId, UUID ownerId);

    Optional<Board> findByGameAndOwner(Game game, User owner);

    List<Board> findByGameId(UUID gameId);
}
