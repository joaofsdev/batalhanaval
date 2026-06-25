package com.softexpert.batalhanaval_api.controller;

import com.softexpert.batalhanaval_api.config.SecurityConfig;
import com.softexpert.batalhanaval_api.domain.*;
import com.softexpert.batalhanaval_api.dto.response.*;
import com.softexpert.batalhanaval_api.exception.GameNotFoundException;
import com.softexpert.batalhanaval_api.exception.PlayerAlreadyInGameException;
import com.softexpert.batalhanaval_api.repository.UserRepository;
import com.softexpert.batalhanaval_api.security.CustomUserDetailsService;
import com.softexpert.batalhanaval_api.security.JwtAuthenticationFilter;
import com.softexpert.batalhanaval_api.security.JwtService;
import com.softexpert.batalhanaval_api.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GameController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class GameControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private GameService gameService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService userDetailsService;

    private final UUID userId = UUID.randomUUID();
    private final UUID gameId = UUID.randomUUID();

    private void mockUserResolution() {
        User user = new User();
        user.setId(userId);
        user.setUsername("player1");
        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(user));
    }

    @Test
    void createGame_noAuth_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/games"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "player1")
    void createGame_authenticated_shouldReturn201() throws Exception {
        mockUserResolution();
        GameResponse response = new GameResponse(gameId, GameStatus.WAITING,
            new PlayerSummary(userId, "player1"), null, null, null, null, new OpponentBoardResponse(List.of()), Instant.now());
        when(gameService.createOrJoinGame(userId)).thenReturn(response);

        mockMvc.perform(post("/api/games").with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @WithMockUser(username = "player1")
    void createGame_alreadyInGame_shouldReturn409() throws Exception {
        mockUserResolution();
        when(gameService.createOrJoinGame(userId)).thenThrow(new PlayerAlreadyInGameException());

        mockMvc.perform(post("/api/games").with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PLAYER_ALREADY_IN_GAME"));
    }

    @Test
    @WithMockUser(username = "player1")
    void getGameState_shouldReturn200() throws Exception {
        mockUserResolution();
        GameResponse response = new GameResponse(gameId, GameStatus.IN_PROGRESS,
            new PlayerSummary(userId, "player1"), new PlayerSummary(UUID.randomUUID(), "player2"),
            userId, null, null, new OpponentBoardResponse(List.of()), Instant.now());
        when(gameService.getGameState(gameId, userId)).thenReturn(response);

        mockMvc.perform(get("/api/games/" + gameId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser(username = "player1")
    void getGameState_notFound_shouldReturn404() throws Exception {
        mockUserResolution();
        when(gameService.getGameState(eq(gameId), eq(userId))).thenThrow(new GameNotFoundException());

        mockMvc.perform(get("/api/games/" + gameId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("GAME_NOT_FOUND"));
    }

    @Test
    @WithMockUser(username = "player1")
    void placeShips_shouldReturn200() throws Exception {
        mockUserResolution();
        PlaceShipsResponse response = new PlaceShipsResponse("Fleet placed successfully", true, GameStatus.PLACING);
        when(gameService.placeShips(eq(gameId), eq(userId), any())).thenReturn(response);

        String shipsJson = "{\"ships\":[" +
            "{\"shipType\":\"CARRIER\",\"originRow\":0,\"originCol\":0,\"orientation\":\"HORIZONTAL\"}," +
            "{\"shipType\":\"BATTLESHIP\",\"originRow\":2,\"originCol\":0,\"orientation\":\"HORIZONTAL\"}," +
            "{\"shipType\":\"CRUISER\",\"originRow\":4,\"originCol\":0,\"orientation\":\"HORIZONTAL\"}," +
            "{\"shipType\":\"SUBMARINE\",\"originRow\":6,\"originCol\":0,\"orientation\":\"HORIZONTAL\"}," +
            "{\"shipType\":\"DESTROYER\",\"originRow\":8,\"originCol\":0,\"orientation\":\"HORIZONTAL\"}" +
            "]}";

        mockMvc.perform(post("/api/games/" + gameId + "/ships").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(shipsJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.boardReady").value(true));
    }
}
