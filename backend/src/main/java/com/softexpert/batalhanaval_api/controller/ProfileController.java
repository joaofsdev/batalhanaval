package com.softexpert.batalhanaval_api.controller;

import com.softexpert.batalhanaval_api.dto.response.PlayerProfileResponse;
import com.softexpert.batalhanaval_api.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{id}/profile")
    public PlayerProfileResponse getProfile(@PathVariable UUID id) {
        return profileService.getProfile(id);
    }
}
