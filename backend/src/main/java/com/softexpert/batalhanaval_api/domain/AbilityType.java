package com.softexpert.batalhanaval_api.domain;

import lombok.Getter;

@Getter
public enum AbilityType {

    RADAR("Radar", "Revela presença de navios em área 3x3"),
    DOUBLE_TORPEDO("Torpedo Duplo", "Dispara 2 tiros no mesmo turno"),
    SHIELD("Escudo", "Anula o próximo tiro recebido"),
    LINE_BOMBARDMENT("Bombardeio em Linha", "Ataca toda uma linha ou coluna");

    private final String displayName;
    private final String description;

    AbilityType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
