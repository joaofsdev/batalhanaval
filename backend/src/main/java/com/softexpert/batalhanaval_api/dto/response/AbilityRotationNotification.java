package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.AbilityType;

public record AbilityRotationNotification(
    AbilityType newAbility,
    String newAbilityName,
    String newAbilityDescription,
    AbilityType previousAbility,
    boolean previousWasDiscarded
) {

    public static AbilityRotationNotification from(AbilityRotationResult result) {
        return new AbilityRotationNotification(
            result.newAbility(),
            result.newAbilityName(),
            result.newAbilityDescription(),
            result.previousAbility(),
            result.previousWasDiscarded()
        );
    }
}
