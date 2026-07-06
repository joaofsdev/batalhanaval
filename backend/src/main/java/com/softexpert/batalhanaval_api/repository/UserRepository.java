package com.softexpert.batalhanaval_api.repository;

import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.domain.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.status = 'SUSPENDED' AND u.suspendedUntil < :now")
    List<User> findExpiredSuspensions(@Param("now") Instant now);
}
