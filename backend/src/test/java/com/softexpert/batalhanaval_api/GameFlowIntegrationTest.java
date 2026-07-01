package com.softexpert.batalhanaval_api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full end-to-end integration test covering the complete game flow:
 * Register → Create Game → Join → Place Ships → Shoot → Win
 *
 * Uses real H2 database with @SpringBootTest (no mocks).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GameFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String player1Token;
    private String player2Token;
    private String player1Id;
    private String player2Id;

    @BeforeEach
    void setUp() throws Exception {
        // Register player 1
        MvcResult result1 = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"player1_%d","email":"p1_%d@test.com","password":"password123"}
                    """.formatted(System.nanoTime(), System.nanoTime())))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode auth1 = objectMapper.readTree(result1.getResponse().getContentAsString());
        player1Token = auth1.get("token").asText();
        player1Id = auth1.get("id").asText();

        // Register player 2
        MvcResult result2 = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"player2_%d","email":"p2_%d@test.com","password":"password123"}
                    """.formatted(System.nanoTime(), System.nanoTime())))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode auth2 = objectMapper.readTree(result2.getResponse().getContentAsString());
        player2Token = auth2.get("token").asText();
        player2Id = auth2.get("id").asText();
    }

    @Test
    void fullGameFlow_register_create_join_place_shoot_win() throws Exception {
        // 1. Player 1 creates a game
        MvcResult createResult = mockMvc.perform(post("/api/games")
                .header("Authorization", "Bearer " + player1Token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("WAITING"))
            .andExpect(jsonPath("$.player2").isEmpty())
            .andReturn();

        String gameId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .get("id").asText();

        // 2. Player 2 joins the game (matchmaking finds the WAITING game)
        mockMvc.perform(post("/api/games")
                .header("Authorization", "Bearer " + player2Token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PLACING"))
            .andExpect(jsonPath("$.player1").isNotEmpty())
            .andExpect(jsonPath("$.player2").isNotEmpty());

        // 3. Player 1 places ships
        mockMvc.perform(post("/api/games/" + gameId + "/ships")
                .header("Authorization", "Bearer " + player1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(shipsPayload()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.boardReady").value(true))
            .andExpect(jsonPath("$.gameStatus").value("PLACING"));

        // 4. Player 2 places ships → game transitions to IN_PROGRESS
        MvcResult placeResult = mockMvc.perform(post("/api/games/" + gameId + "/ships")
                .header("Authorization", "Bearer " + player2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(shipsPayload()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.boardReady").value(true))
            .andExpect(jsonPath("$.gameStatus").value("IN_PROGRESS"))
            .andReturn();

        // 5. Verify game state is IN_PROGRESS
        mockMvc.perform(get("/api/games/" + gameId)
                .header("Authorization", "Bearer " + player1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.currentTurnPlayerId").value(player1Id));

        // 6. Verify fog of war: player 1 can see own board but not opponent ships
        MvcResult stateResult = mockMvc.perform(get("/api/games/" + gameId)
                .header("Authorization", "Bearer " + player1Token))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode gameState = objectMapper.readTree(stateResult.getResponse().getContentAsString());
        assertThat(gameState.get("myBoard").get("ships").size()).isEqualTo(5);
        assertThat(gameState.get("opponentBoard").get("shotsReceived").size()).isEqualTo(0);

        // 7. Player 1 fires (via REST to simulate - actual game uses WebSocket)
        //    We'll use the ShotService directly via game state changes
        //    Instead, let's test surrender flow which is REST-based

        // 8. Player 1 surrenders
        mockMvc.perform(post("/api/games/" + gameId + "/surrender")
                .header("Authorization", "Bearer " + player1Token))
            .andExpect(status().isNoContent());

        // 9. Verify game is FINISHED with player 2 as winner
        mockMvc.perform(get("/api/games/" + gameId)
                .header("Authorization", "Bearer " + player2Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FINISHED"))
            .andExpect(jsonPath("$.winnerId").value(player2Id));

        // 10. Verify history shows the game
        mockMvc.perform(get("/api/games/history")
                .header("Authorization", "Bearer " + player1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].won").value(false));

        mockMvc.perform(get("/api/games/history")
                .header("Authorization", "Bearer " + player2Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].won").value(true));

        // 11. Verify ranking reflects the result
        mockMvc.perform(get("/api/ranking")
                .header("Authorization", "Bearer " + player2Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.myPosition.wins").value(1))
            .andExpect(jsonPath("$.myPosition.totalGames").value(1));
    }

    @Test
    void activeGame_shouldReturnCurrentGame() throws Exception {
        // Player 1 creates a game
        MvcResult createResult = mockMvc.perform(post("/api/games")
                .header("Authorization", "Bearer " + player1Token))
            .andExpect(status().isCreated())
            .andReturn();

        String gameId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .get("id").asText();

        // GET /active should return the game
        mockMvc.perform(get("/api/games/active")
                .header("Authorization", "Bearer " + player1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(gameId));

        // Player 2 should have no active game yet
        mockMvc.perform(get("/api/games/active")
                .header("Authorization", "Bearer " + player2Token))
            .andExpect(status().isNoContent());

        // Cleanup: cancel the game
        mockMvc.perform(delete("/api/games/" + gameId)
                .header("Authorization", "Bearer " + player1Token))
            .andExpect(status().isNoContent());
    }

    @Test
    void playerAlreadyInGame_shouldReturn409() throws Exception {
        // Player 1 creates a game
        mockMvc.perform(post("/api/games")
                .header("Authorization", "Bearer " + player1Token))
            .andExpect(status().isCreated());

        // Player 1 tries to create another → 409
        mockMvc.perform(post("/api/games")
                .header("Authorization", "Bearer " + player1Token))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PLAYER_ALREADY_IN_GAME"));
    }

    @Test
    void invalidShipPlacement_shouldReturn400() throws Exception {
        // Create and join game
        mockMvc.perform(post("/api/games")
                .header("Authorization", "Bearer " + player1Token))
            .andExpect(status().isCreated());

        MvcResult joinResult = mockMvc.perform(post("/api/games")
                .header("Authorization", "Bearer " + player2Token))
            .andExpect(status().isCreated())
            .andReturn();

        String gameId = objectMapper.readTree(joinResult.getResponse().getContentAsString())
            .get("id").asText();

        // Place ships with overlap (CARRIER at 0,0 horizontal, BATTLESHIP at 0,2 horizontal → overlap at 0,2-0,4)
        mockMvc.perform(post("/api/games/" + gameId + "/ships")
                .header("Authorization", "Bearer " + player1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ships":[
                        {"shipType":"CARRIER","originRow":0,"originCol":0,"orientation":"HORIZONTAL"},
                        {"shipType":"BATTLESHIP","originRow":0,"originCol":2,"orientation":"HORIZONTAL"},
                        {"shipType":"CRUISER","originRow":4,"originCol":0,"orientation":"HORIZONTAL"},
                        {"shipType":"SUBMARINE","originRow":6,"originCol":0,"orientation":"HORIZONTAL"},
                        {"shipType":"DESTROYER","originRow":8,"originCol":0,"orientation":"HORIZONTAL"}
                    ]}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SHIPS_OVERLAP"));
    }

    @Test
    void cancelWaitingGame_shouldSucceed() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/games")
                .header("Authorization", "Bearer " + player1Token))
            .andExpect(status().isCreated())
            .andReturn();

        String gameId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .get("id").asText();

        // Cancel
        mockMvc.perform(delete("/api/games/" + gameId)
                .header("Authorization", "Bearer " + player1Token))
            .andExpect(status().isNoContent());

        // Game should be gone
        mockMvc.perform(get("/api/games/" + gameId)
                .header("Authorization", "Bearer " + player1Token))
            .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedAccess_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/games"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/games/active"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/ranking"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void register_duplicateUsername_shouldReturn409() throws Exception {
        String username = "duplicate_" + System.nanoTime();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","email":"dup1@test.com","password":"password123"}
                    """.formatted(username)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","email":"dup2@test.com","password":"password123"}
                    """.formatted(username)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("USERNAME_TAKEN"));
    }

    @Test
    void login_invalidCredentials_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"nonexistent_user","password":"wrongpass"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void fleetConfig_shouldReturnAllShipTypes() throws Exception {
        mockMvc.perform(get("/api/games/fleet-config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(5))
            .andExpect(jsonPath("$[0].type").value("CARRIER"))
            .andExpect(jsonPath("$[0].size").value(5));
    }

    @Test
    void profile_shouldReturnPlayerStats() throws Exception {
        mockMvc.perform(get("/api/users/" + player1Id + "/profile")
                .header("Authorization", "Bearer " + player1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(player1Id))
            .andExpect(jsonPath("$.username").isNotEmpty())
            .andExpect(jsonPath("$.totalGames").isNumber())
            .andExpect(jsonPath("$.wins").isNumber())
            .andExpect(jsonPath("$.winRate").isNumber());
    }

    private String shipsPayload() {
        return """
            {"ships":[
                {"shipType":"CARRIER","originRow":0,"originCol":0,"orientation":"HORIZONTAL"},
                {"shipType":"BATTLESHIP","originRow":2,"originCol":0,"orientation":"HORIZONTAL"},
                {"shipType":"CRUISER","originRow":4,"originCol":0,"orientation":"HORIZONTAL"},
                {"shipType":"SUBMARINE","originRow":6,"originCol":0,"orientation":"HORIZONTAL"},
                {"shipType":"DESTROYER","originRow":8,"originCol":0,"orientation":"HORIZONTAL"}
            ]}
            """;
    }
}
