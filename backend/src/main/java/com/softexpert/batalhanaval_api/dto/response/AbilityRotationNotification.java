package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.AbilityType;

/**
 * Notification sent to each player individually when their ability is rotated.
 * Sent via /user/queue/game/ability-rotated (private per player).
 */
public record AbilityRotationNotification(
    AbilityType newAbility,
    String newAbilityName,
    String newAbilityDescription,
    AbilityType previousAbility,
    boolean previousWasDiscarded
) {

    /**
     * Creates a notification from an internal rotation result.
     */
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
