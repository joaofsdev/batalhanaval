package com.softexpert.batalhanaval_api.controller;

import com.softexpert.batalhanaval_api.config.SecurityConfig;
import com.softexpert.batalhanaval_api.domain.*;
import com.softexpert.batalhanaval_api.dto.response.*;
import com.softexpert.batalhanaval_api.exception.GameNotFoundException;
import com.softexpert.batalhanaval_api.repository.UserRepository;
import com.softexpert.batalhanaval_api.security.CustomUserDetailsService;
import com.softexpert.batalhanaval_api.security.JwtAuthenticationFilter;
import com.softexpert.batalhanaval_api.security.JwtService;
import com.softexpert.batalhanaval_api.service.AdminGameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminGameController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminGameControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminGameService adminGameService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService userDetailsService;

    private final UUID adminId = UUID.randomUUID();
    private final UUID gameId = UUID.randomUUID();
    private final UUID player1Id = UUID.randomUUID();
    private final UUID player2Id = UUID.randomUUID();

    private User mockAdminUser() {
        User admin = new User();
        admin.setId(adminId);
        admin.setUsername("admin");
        admin.setRole(UserRole.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        return admin;
    }

    @Test
    void revealBoards_noAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admin/games/" + gameId + "/boards"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "player1", roles = {"PLAYER"})
    void revealBoards_nonAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/admin/games/" + gameId + "/boards"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void revealBoards_admin_shouldReturn200WithBoards() throws Exception {
        User admin = mockAdminUser();

        List<ShipResponse> ships = List.of(
            new ShipResponse(ShipType.CARRIER, 0, 0, Orientation.HORIZONTAL, 0, false)
        );
        List<CellResponse> cells = List.of(
            new CellResponse(0, 0, true, false)
        );
        BoardResponse boardResponse = new BoardResponse(true, ships, cells);

        PlayerBoardSummary p1 = new PlayerBoardSummary(player1Id, "player1", boardResponse);
        PlayerBoardSummary p2 = new PlayerBoardSummary(player2Id, "player2", boardResponse);

        AdminGameBoardsResponse response = new AdminGameBoardsResponse(
            gameId, GameStatus.IN_PROGRESS, GameMode.CLASSIC, p1, p2
        );

        when(adminGameService.revealBoards(eq(gameId), any(User.class))).thenReturn(response);

        mockMvc.perform(get("/api/admin/games/" + gameId + "/boards"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gameId").value(gameId.toString()))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.gameMode").value("CLASSIC"))
            .andExpect(jsonPath("$.player1.playerId").value(player1Id.toString()))
            .andExpect(jsonPath("$.player1.username").value("player1"))
            .andExpect(jsonPath("$.player1.board.ready").value(true))
            .andExpect(jsonPath("$.player1.board.ships[0].shipType").value("CARRIER"))
            .andExpect(jsonPath("$.player1.board.ships[0].originRow").value(0))
            .andExpect(jsonPath("$.player1.board.ships[0].originCol").value(0))
            .andExpect(jsonPath("$.player1.board.cells[0].hasShip").value(true))
            .andExpect(jsonPath("$.player2.playerId").value(player2Id.toString()))
            .andExpect(jsonPath("$.player2.username").value("player2"))
            .andExpect(jsonPath("$.player2.board.ready").value(true));

        verify(adminGameService).revealBoards(eq(gameId), any(User.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void revealBoards_gameNotFound_shouldReturn404() throws Exception {
        mockAdminUser();
        when(adminGameService.revealBoards(eq(gameId), any(User.class)))
            .thenThrow(new GameNotFoundException());

        mockMvc.perform(get("/api/admin/games/" + gameId + "/boards"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("GAME_NOT_FOUND"));
    }
}
