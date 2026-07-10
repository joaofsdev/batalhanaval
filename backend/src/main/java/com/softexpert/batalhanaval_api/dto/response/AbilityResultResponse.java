package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.AbilityType;

import java.util.List;

public record AbilityResultResponse(
    AbilityType abilityType,
    boolean[][] radarGrid,
    Integer centerRow,
    Integer centerCol,
    List<ShotResultResponse> shotResults,
    String message
) {
    public static AbilityResultResponse radar(boolean[][] grid, int centerRow, int centerCol) {
        return new AbilityResultResponse(AbilityType.RADAR, grid, centerRow, centerCol, null, null);
    }

    public static AbilityResultResponse doubleTorpedo(List<ShotResultResponse> results) {
        return new AbilityResultResponse(AbilityType.DOUBLE_TORPEDO, null, null, null, results, null);
    }

    public static AbilityResultResponse shield() {
        return new AbilityResultResponse(AbilityType.SHIELD, null, null, null, null, "Shield activated. Next incoming shot will be nullified.");
    }

    public static AbilityResultResponse lineBombardment(List<ShotResultResponse> results) {
        return new AbilityResultResponse(AbilityType.LINE_BOMBARDMENT, null, null, null, results, null);
    }
}
