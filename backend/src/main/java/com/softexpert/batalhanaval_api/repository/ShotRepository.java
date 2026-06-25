package com.softexpert.batalhanaval_api.repository;

import com.softexpert.batalhanaval_api.domain.Shot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShotRepository extends JpaRepository<Shot, UUID> {

    List<Shot> findAllByGameIdAndAttackerId(UUID gameId, UUID attackerId);

    List<Shot> findAllByGameIdOrderByFiredAtAsc(UUID gameId);
}
