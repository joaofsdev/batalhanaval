package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.AbilityType;

import java.util.UUID;

/**
 * Data returned per player after ability rotation.
 * Used internally by AbilityService and will be used by NotificationService in Phase 2.
 */
public record AbilityRotationResult(
    UUID playerId,
    AbilityType newAbility,
    String newAbilityName,
    String newAbilityDescription,
    AbilityType previousAbility,
    boolean previousWasDiscarded
) {}
