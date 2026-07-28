package com.softexpert.batalhanaval_api.repository;

import com.softexpert.batalhanaval_api.domain.Board;
import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoardRepository extends JpaRepository<Board, UUID> {

    Optional<Board> findByGameIdAndOwnerId(UUID gameId, UUID ownerId);

    @Query("SELECT b FROM Board b LEFT JOIN FETCH b.ships WHERE b.game.id = :gameId AND b.owner.id = :ownerId")
    Optional<Board> findByGameIdAndOwnerIdWithShips(@Param("gameId") UUID gameId, @Param("ownerId") UUID ownerId);

    @Query("SELECT b FROM Board b LEFT JOIN FETCH b.cells WHERE b.game.id = :gameId AND b.owner.id = :ownerId")
    Optional<Board> findByGameIdAndOwnerIdWithCells(@Param("gameId") UUID gameId, @Param("ownerId") UUID ownerId);

    Optional<Board> findByGameAndOwner(Game game, User owner);

    List<Board> findByGameId(UUID gameId);
}
