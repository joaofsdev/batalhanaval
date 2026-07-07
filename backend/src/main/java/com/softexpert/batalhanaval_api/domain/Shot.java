package com.softexpert.batalhanaval_api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "shots", uniqueConstraints = {
    @UniqueConstraint(name = "uk_shot_board_row_col", columnNames = {"target_board_id", "shot_row", "shot_col"})
}, indexes = {
    @Index(name = "idx_shot_game", columnList = "game_id"),
    @Index(name = "idx_shot_attacker", columnList = "attacker_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Shot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attacker_id", nullable = false)
    private User attacker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_board_id", nullable = false)
    private Board targetBoard;

    @Column(name = "shot_row", nullable = false)
    private int row;

    @Column(name = "shot_col", nullable = false)
    private int col;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShotResult result;

    @Enumerated(EnumType.STRING)
    private ShipType sunkShipType;

    private Integer sunkShipOriginRow;

    private Integer sunkShipOriginCol;

    @Enumerated(EnumType.STRING)
    private Orientation sunkShipOrientation;

    @Column(nullable = false)
    private Instant firedAt;

    @PrePersist
    protected void onCreate() {
        this.firedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Shot shot = (Shot) o;
        return id != null && Objects.equals(id, shot.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
