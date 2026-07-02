package com.softexpert.batalhanaval_api.repository;

import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.domain.GameMode;
import com.softexpert.batalhanaval_api.domain.GameStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {

    @Query("SELECT g FROM Game g WHERE g.status = :status AND g.player1.id <> :userId")
    List<Game> findByStatusAndPlayer1IdNot(@Param("status") GameStatus status, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Game g WHERE g.status = :status AND g.player1.id <> :userId ORDER BY g.createdAt ASC LIMIT 1")
    Optional<Game> findFirstWaitingGameForUpdate(@Param("status") GameStatus status, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Game g WHERE g.status = :status AND g.player1.id <> :userId AND g.gameMode = :gameMode ORDER BY g.createdAt ASC LIMIT 1")
    Optional<Game> findFirstWaitingGameByModeForUpdate(@Param("status") GameStatus status, @Param("userId") UUID userId, @Param("gameMode") GameMode gameMode);

    @Query("SELECT g FROM Game g WHERE (g.player1.id = :userId OR g.player2.id = :userId) AND g.status IN :statuses")
    Optional<Game> findActiveGameByUserId(@Param("userId") UUID userId, @Param("statuses") List<GameStatus> statuses);

    @Query("SELECT g.winner.id, g.winner.username, COUNT(g) FROM Game g WHERE g.status = 'FINISHED' AND g.winner IS NOT NULL GROUP BY g.winner.id, g.winner.username ORDER BY COUNT(g) DESC")
    List<Object[]> findWinsRanking();

    @Query("SELECT COUNT(g) FROM Game g WHERE (g.player1.id = :userId OR g.player2.id = :userId) AND g.status = 'FINISHED'")
    long countFinishedGamesByUserId(@Param("userId") UUID userId);

    @Query("""
        SELECT u.id, u.username,
               COALESCE(SUM(CASE WHEN g.winner.id = u.id THEN 1 ELSE 0 END), 0),
               COUNT(g)
        FROM Game g
        JOIN User u ON (g.player1.id = u.id OR g.player2.id = u.id)
        WHERE g.status = 'FINISHED'
        GROUP BY u.id, u.username
        ORDER BY COALESCE(SUM(CASE WHEN g.winner.id = u.id THEN 1 ELSE 0 END), 0) DESC, COUNT(g) ASC
        """)
    List<Object[]> findFullRanking();

    @Query("""
        SELECT u.id, u.username,
               COALESCE(SUM(CASE WHEN g.winner.id = u.id THEN 1 ELSE 0 END), 0),
               COUNT(g)
        FROM Game g
        JOIN User u ON (g.player1.id = u.id OR g.player2.id = u.id)
        WHERE g.status = 'FINISHED' AND g.updatedAt >= :since
        GROUP BY u.id, u.username
        ORDER BY COALESCE(SUM(CASE WHEN g.winner.id = u.id THEN 1 ELSE 0 END), 0) DESC, COUNT(g) ASC
        """)
    List<Object[]> findFullRankingSince(@Param("since") java.time.Instant since);

    @Query("SELECT g FROM Game g WHERE g.status = :status AND g.currentTurn IS NOT NULL AND g.updatedAt < :cutoff")
    List<Game> findGamesWithExpiredTurn(@Param("status") GameStatus status, @Param("cutoff") java.time.Instant cutoff);

    @Query("SELECT g FROM Game g WHERE (g.player1.id = :userId OR g.player2.id = :userId) AND g.status = 'FINISHED' ORDER BY g.updatedAt DESC")
    org.springframework.data.domain.Page<Game> findFinishedGamesByUserId(@Param("userId") UUID userId, org.springframework.data.domain.Pageable pageable);
}
