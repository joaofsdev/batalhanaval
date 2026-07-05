package com.softexpert.batalhanaval_api.domain;

import lombok.Getter;

@Getter
public enum StormEventType {

    FOG("Nevoeiro", "Resultados dos tiros ficam ocultos por 4 turnos"),
    TIDE("Maré Alta", "Uma linha do tabuleiro fica inacessível neste turno"),
    CURRENT("Corrente Marítima", "Navios se deslocam 1 célula aleatoriamente"),
    CALM("Calmaria", "Ambos os jogadores ganham um tiro bônus");

    private final String displayName;
    private final String description;

    StormEventType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
