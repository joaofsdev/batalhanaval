package com.softexpert.batalhanaval_api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "storm_events", indexes = {
    @Index(name = "idx_storm_game", columnList = "game_id"),
    @Index(name = "idx_storm_game_turn", columnList = "game_id, turn_number")
})
@Getter
@Setter
@NoArgsConstructor
public class StormEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "turn_number", nullable = false)
    private int turnNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StormEventType eventType;

    @Column(name = "affected_axis")
    private String affectedAxis;

    @Column(nullable = false)
    private boolean resolved;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StormEvent that = (StormEvent) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
