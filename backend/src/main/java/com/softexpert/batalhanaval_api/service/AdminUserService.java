package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.domain.UserStatus;
import com.softexpert.batalhanaval_api.dto.response.AdminUserResponse;
import com.softexpert.batalhanaval_api.dto.response.PageResponse;
import com.softexpert.batalhanaval_api.exception.UserNotFoundException;
import com.softexpert.batalhanaval_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final AdminAuditService adminAuditService;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> listUsers(UserStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "username"));

        Page<User> usersPage;
        if (status != null) {
            usersPage = userRepository.findByStatus(status, pageRequest);
        } else {
            usersPage = userRepository.findAll(pageRequest);
        }

        var content = usersPage.getContent().stream()
            .map(this::toResponse)
            .toList();

        return new PageResponse<>(content, usersPage.getNumber(), usersPage.getSize(),
            usersPage.getTotalElements(), usersPage.getTotalPages());
    }

    @Transactional
    public AdminUserResponse ban(UUID userId, User admin) {
        User user = findUser(userId);
        user.setStatus(UserStatus.BANNED);
        user.setSuspendedUntil(null);
        userRepository.save(user);

        adminAuditService.log(admin, "USER_BANNED", "USER", userId, null);

        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse suspend(UUID userId, Instant suspendedUntil, User admin) {
        User user = findUser(userId);
        user.setStatus(UserStatus.SUSPENDED);
        user.setSuspendedUntil(suspendedUntil);
        userRepository.save(user);

        adminAuditService.log(admin, "USER_SUSPENDED", "USER", userId,
            "suspendedUntil=" + suspendedUntil);

        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse reactivate(UUID userId, User admin) {
        User user = findUser(userId);
        user.setStatus(UserStatus.ACTIVE);
        user.setSuspendedUntil(null);
        userRepository.save(user);

        adminAuditService.log(admin, "USER_REACTIVATED", "USER", userId, null);

        return toResponse(user);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole(),
            user.getStatus(),
            user.getSuspendedUntil(),
            user.getCreatedAt()
        );
    }
}
