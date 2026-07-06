package com.softexpert.batalhanaval_api.controller;

import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.domain.UserStatus;
import com.softexpert.batalhanaval_api.dto.request.SuspendRequest;
import com.softexpert.batalhanaval_api.dto.response.AdminUserResponse;
import com.softexpert.batalhanaval_api.dto.response.PageResponse;
import com.softexpert.batalhanaval_api.repository.UserRepository;
import com.softexpert.batalhanaval_api.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final UserRepository userRepository;

    @GetMapping
    public PageResponse<AdminUserResponse> listUsers(
        @RequestParam(required = false) UserStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return adminUserService.listUsers(status, page, size);
    }

    @PatchMapping("/{id}/ban")
    public AdminUserResponse ban(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        User admin = resolveAdmin(userDetails);
        return adminUserService.ban(id, admin);
    }

    @PatchMapping("/{id}/suspend")
    public AdminUserResponse suspend(@PathVariable UUID id, @Valid @RequestBody SuspendRequest request,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        User admin = resolveAdmin(userDetails);
        return adminUserService.suspend(id, request.suspendedUntil(), admin);
    }

    @PatchMapping("/{id}/reactivate")
    public AdminUserResponse reactivate(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        User admin = resolveAdmin(userDetails);
        return adminUserService.reactivate(id, admin);
    }

    private User resolveAdmin(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
    }
}
