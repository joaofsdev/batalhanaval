package com.softexpert.batalhanaval_api.dto.response;

public record OpponentConnectionEvent(
    String type,
    int gracePeriodSeconds
) {
    public static OpponentConnectionEvent disconnected(int gracePeriodSeconds) {
        return new OpponentConnectionEvent("DISCONNECTED", gracePeriodSeconds);
    }

    public static OpponentConnectionEvent reconnected() {
        return new OpponentConnectionEvent("RECONNECTED", 0);
    }
}
