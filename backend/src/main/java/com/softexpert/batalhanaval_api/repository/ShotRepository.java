package com.softexpert.batalhanaval_api.repository;

import com.softexpert.batalhanaval_api.domain.Shot;
import com.softexpert.batalhanaval_api.domain.ShotResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ShotRepository extends JpaRepository<Shot, UUID> {

    List<Shot> findAllByGameIdAndAttackerId(UUID gameId, UUID attackerId);

    List<Shot> findAllByGameIdOrderByFiredAtAsc(UUID gameId);

    boolean existsByTargetBoardIdAndRowAndCol(UUID targetBoardId, int row, int col);

    @Query("SELECT COUNT(s) FROM Shot s WHERE s.attacker.id = :attackerId AND s.game.ranked = true")
    long countByAttackerId(@Param("attackerId") UUID attackerId);

    @Query("SELECT COUNT(s) FROM Shot s WHERE s.attacker.id = :attackerId AND s.result IN :results AND s.game.ranked = true")
    long countByAttackerIdAndResultIn(@Param("attackerId") UUID attackerId, @Param("results") List<ShotResult> results);

    @Query("SELECT COUNT(s) FROM Shot s WHERE s.attacker.id = :attackerId AND s.result = :result AND s.game.ranked = true")
    long countByAttackerIdAndResult(@Param("attackerId") UUID attackerId, @Param("result") ShotResult result);
}
