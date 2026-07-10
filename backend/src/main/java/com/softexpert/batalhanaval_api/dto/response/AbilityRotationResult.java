package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.AbilityType;

import java.util.UUID;

public record AbilityRotationResult(
    UUID playerId,
    AbilityType newAbility,
    String newAbilityName,
    String newAbilityDescription,
    AbilityType previousAbility,
    boolean previousWasDiscarded
) {}
