package com.softexpert.batalhanaval_api.repository;

import com.softexpert.batalhanaval_api.domain.Cell;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CellRepository extends JpaRepository<Cell, UUID> {

    Optional<Cell> findByBoardIdAndRowAndCol(UUID boardId, int row, int col);
}
