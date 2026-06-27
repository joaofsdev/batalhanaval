package com.softexpert.batalhanaval_api.repository;

import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.domain.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {

    @Query("SELECT g FROM Game g WHERE g.status = :status AND g.player1.id <> :userId")
    List<Game> findByStatusAndPlayer1IdNot(@Param("status") GameStatus status, @Param("userId") UUID userId);

    @Query("SELECT g FROM Game g WHERE (g.player1.id = :userId OR g.player2.id = :userId) AND g.status IN :statuses")
    Optional<Game> findActiveGameByUserId(@Param("userId") UUID userId, @Param("statuses") List<GameStatus> statuses);

    @Query("SELECT g.winner.id, g.winner.username, COUNT(g) FROM Game g WHERE g.status = 'FINISHED' AND g.winner IS NOT NULL GROUP BY g.winner.id, g.winner.username ORDER BY COUNT(g) DESC")
    List<Object[]> findWinsRanking();

    @Query("SELECT COUNT(g) FROM Game g WHERE (g.player1.id = :userId OR g.player2.id = :userId) AND g.status = 'FINISHED'")
    long countFinishedGamesByUserId(@Param("userId") UUID userId);
}
