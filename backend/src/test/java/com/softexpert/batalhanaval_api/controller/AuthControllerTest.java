package com.softexpert.batalhanaval_api.controller;

import com.softexpert.batalhanaval_api.config.SecurityConfig;
import com.softexpert.batalhanaval_api.domain.UserRole;
import com.softexpert.batalhanaval_api.dto.response.AuthResponse;
import com.softexpert.batalhanaval_api.exception.InvalidCredentialsException;
import com.softexpert.batalhanaval_api.exception.UsernameAlreadyTakenException;
import com.softexpert.batalhanaval_api.security.CustomUserDetailsService;
import com.softexpert.batalhanaval_api.security.JwtAuthenticationFilter;
import com.softexpert.batalhanaval_api.security.JwtService;
import com.softexpert.batalhanaval_api.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuthService authService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService userDetailsService;

    @Test
    void register_shouldReturn201() throws Exception {
        AuthResponse response = new AuthResponse(UUID.randomUUID(), "jogador1", "j1@email.com", UserRole.PLAYER, "token123");
        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"jogador1\",\"email\":\"j1@email.com\",\"password\":\"senha@123\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("jogador1"))
            .andExpect(jsonPath("$.token").value("token123"));
    }

    @Test
    void register_duplicateUsername_shouldReturn409() throws Exception {
        when(authService.register(any())).thenThrow(new UsernameAlreadyTakenException());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"jogador1\",\"email\":\"j1@email.com\",\"password\":\"senha@123\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("USERNAME_TAKEN"));
    }

    @Test
    void register_invalidFields_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"\",\"email\":\"invalid\",\"password\":\"short\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void login_shouldReturn200() throws Exception {
        AuthResponse response = new AuthResponse(UUID.randomUUID(), "jogador1", "j1@email.com", UserRole.PLAYER, "token123");
        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"jogador1\",\"password\":\"senha1234\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("token123"));
    }

    @Test
    void login_invalidCredentials_shouldReturn401() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"jogador1\",\"password\":\"wrongpass\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }
}
