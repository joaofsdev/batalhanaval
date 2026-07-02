package com.softexpert.batalhanaval_api.repository;

import com.softexpert.batalhanaval_api.domain.StormEvent;
import com.softexpert.batalhanaval_api.domain.StormEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StormEventRepository extends JpaRepository<StormEvent, UUID> {

    Optional<StormEvent> findByGameIdAndTurnNumber(UUID gameId, int turnNumber);

    Optional<StormEvent> findByGameIdAndResolvedFalse(UUID gameId);

    List<StormEvent> findByGameIdOrderByTurnNumberAsc(UUID gameId);

    @Query("SELECT e FROM StormEvent e WHERE e.game.id = :gameId AND e.eventType = :eventType AND e.resolved = true ORDER BY e.turnNumber DESC LIMIT 1")
    Optional<StormEvent> findLastResolvedByType(@Param("gameId") UUID gameId, @Param("eventType") StormEventType eventType);
}
