package com.softexpert.batalhanaval_api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "ships", indexes = {
    @Index(name = "idx_ship_board", columnList = "board_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Ship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipType shipType;

    @Column(nullable = false)
    private int originRow;

    @Column(nullable = false)
    private int originCol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Orientation orientation;

    @Column(nullable = false)
    private int hits;

    public boolean isSunk() {
        return hits >= shipType.getSize();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ship ship = (Ship) o;
        return id != null && Objects.equals(id, ship.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
