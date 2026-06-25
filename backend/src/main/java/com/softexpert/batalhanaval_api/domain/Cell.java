package com.softexpert.batalhanaval_api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "cells", uniqueConstraints = {
    @UniqueConstraint(name = "uk_cell_board_row_col", columnNames = {"board_id", "cell_row", "cell_col"})
}, indexes = {
    @Index(name = "idx_cell_board", columnList = "board_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Cell {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(name = "cell_row", nullable = false)
    private int row;

    @Column(name = "cell_col", nullable = false)
    private int col;

    @Column(nullable = false)
    private boolean hasShip;

    @Column(nullable = false)
    private boolean hit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ship_id")
    private Ship ship;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cell cell = (Cell) o;
        return id != null && Objects.equals(id, cell.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
