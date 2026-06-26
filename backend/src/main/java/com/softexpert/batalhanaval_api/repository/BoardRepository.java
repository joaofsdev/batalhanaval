package com.softexpert.batalhanaval_api.repository;

import com.softexpert.batalhanaval_api.domain.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoardRepository extends JpaRepository<Board, UUID> {

    Optional<Board> findByGameIdAndOwnerId(UUID gameId, UUID ownerId);

    List<Board> findByGameId(UUID gameId);
}
