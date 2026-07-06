package com.softexpert.batalhanaval_api.repository;

import com.softexpert.batalhanaval_api.domain.Shot;
import com.softexpert.batalhanaval_api.domain.ShotResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShotRepository extends JpaRepository<Shot, UUID> {

    List<Shot> findAllByGameIdAndAttackerId(UUID gameId, UUID attackerId);

    List<Shot> findAllByGameIdOrderByFiredAtAsc(UUID gameId);

    boolean existsByTargetBoardIdAndRowAndCol(UUID targetBoardId, int row, int col);

    long countByAttackerId(UUID attackerId);

    long countByAttackerIdAndResultIn(UUID attackerId, List<ShotResult> results);

    long countByAttackerIdAndResult(UUID attackerId, ShotResult result);
}
