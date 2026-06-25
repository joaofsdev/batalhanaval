package com.softexpert.batalhanaval_api.repository;

import com.softexpert.batalhanaval_api.domain.Ship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShipRepository extends JpaRepository<Ship, UUID> {

    List<Ship> findAllByBoardId(UUID boardId);
}
