package com.softexpert.batalhanaval_api.repository;

import com.softexpert.batalhanaval_api.domain.PlayerAbility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerAbilityRepository extends JpaRepository<PlayerAbility, UUID> {

    Optional<PlayerAbility> findByGameIdAndUserId(UUID gameId, UUID userId);

    List<PlayerAbility> findByGameId(UUID gameId);
}
