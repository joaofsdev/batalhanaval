package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.AbilityType;

public record AbilityStatusResponse(
    AbilityType abilityType,
    String name,
    String description,
    boolean used,
    Integer usedOnTurn
) {}
