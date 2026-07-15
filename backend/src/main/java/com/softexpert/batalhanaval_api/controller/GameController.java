package com.softexpert.batalhanaval_api.controller;

import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.domain.ShipType;
import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.dto.request.CreateGameRequest;
import com.softexpert.batalhanaval_api.dto.request.PlaceShipsRequest;
import com.softexpert.batalhanaval_api.dto.response.FleetConfigResponse;
import com.softexpert.batalhanaval_api.dto.response.GameHistoryEntry;
import com.softexpert.batalhanaval_api.dto.response.GameResponse;
import com.softexpert.batalhanaval_api.dto.response.PageResponse;
import com.softexpert.batalhanaval_api.dto.response.PlayerSummary;
import com.softexpert.batalhanaval_api.dto.response.PlaceShipsResponse;
import com.softexpert.batalhanaval_api.dto.response.RematchInvite;
import com.softexpert.batalhanaval_api.dto.response.RematchResponse;
import com.softexpert.batalhanaval_api.repository.UserRepository;
import com.softexpert.batalhanaval_api.service.GameService;
import com.softexpert.batalhanaval_api.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
@Tag(name = "Partidas")
@SecurityRequirement(name = "bearerAuth")
public class GameController {

    private final GameService gameService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping("/fleet-config")
    @Operation(
            summary = "Obter configuração da frota",
            description = "Retorna a lista de tipos de navio disponíveis com seus tamanhos e nomes.",
            security = {})
    @ApiResponse(responseCode = "200", description = "Configuração da frota retornada com sucesso")
    public List<FleetConfigResponse> getFleetConfig() {
        return Arrays.stream(ShipType.values())
            .map(s -> new FleetConfigResponse(s.name(), s.getSize(), s.getDisplayName()))
            .toList();
    }

    @GetMapping("/active")
    @Operation(
            summary = "Obter partida ativa",
            description = "Retorna a partida em andamento do jogador autenticado, se houver.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Partida ativa encontrada"),
        @ApiResponse(responseCode = "204", description = "Nenhuma partida ativa"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido")
    })
    public ResponseEntity<GameResponse> getActiveGame(@Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails);
        return gameService.getActiveGame(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/history")
    @Operation(
            summary = "Histórico de partidas",
            description = "Retorna o histórico de partidas do jogador autenticado com paginação.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido")
    })
    public PageResponse<GameHistoryEntry> getGameHistory(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = resolveUserId(userDetails);
        return gameService.getGameHistory(userId, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Criar ou entrar em partida",
            description = "Inicia o matchmaking: cria uma nova partida ou entra em uma existente aguardando oponente no modo selecionado.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Partida criada ou pareada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "409", description = "Jogador já possui partida ativa")
    })
    public GameResponse createOrJoinGame(
        @Valid @RequestBody CreateGameRequest request,
        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = resolveUserId(userDetails);
        GameResponse response = gameService.createOrJoinGame(userId, request.gameMode());
        if (response.player2() != null) {
            PlayerSummary joiner = response.player2().id().equals(userId)
                ? response.player2()
                : response.player1();
            notificationService.notifyPlayerJoined(response.id(), joiner);
        }
        return response;
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obter estado da partida",
            description = "Retorna o estado atual de uma partida específica visível para o jogador autenticado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado da partida retornado"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "404", description = "Partida não encontrada")
    })
    public GameResponse getGameState(@PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails);
        return gameService.getGameState(id, userId);
    }

    @PostMapping("/{id}/ships")
    @Operation(
            summary = "Posicionar frota",
            description = "Envia o posicionamento dos navios do jogador no tabuleiro. Validação server-side garante que não há sobreposições ou posições fora dos limites.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Frota posicionada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Posicionamento inválido"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "404", description = "Partida não encontrada"),
        @ApiResponse(responseCode = "409", description = "Frota já posicionada ou partida em estado inválido")
    })
    public PlaceShipsResponse placeShips(
        @PathVariable UUID id,
        @Valid @RequestBody PlaceShipsRequest request,
        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = resolveUserId(userDetails);
        return gameService.placeShips(id, userId, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Cancelar partida",
            description = "Cancela uma partida que ainda não iniciou (status WAITING ou PLACING).")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Partida cancelada"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "404", description = "Partida não encontrada"),
        @ApiResponse(responseCode = "409", description = "Partida não pode ser cancelada no estado atual")
    })
    public void cancelGame(@PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails);
        gameService.cancelGame(id, userId);
    }

    @PostMapping("/{id}/surrender")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Desistir da partida",
            description = "O jogador autenticado desiste da partida em andamento, concedendo a vitória ao oponente.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Desistência registrada"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "404", description = "Partida não encontrada"),
        @ApiResponse(responseCode = "409", description = "Partida não está em andamento")
    })
    public void surrender(@PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails);
        Game game = gameService.surrender(id, userId);
        notificationService.broadcastGameState(game);
    }

    @PostMapping("/{id}/rematch")
    @Operation(
            summary = "Solicitar revanche",
            description = "Solicita uma revanche ao oponente após o término da partida. Se ambos solicitarem, uma nova partida é criada automaticamente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Solicitação de revanche enviada"),
        @ApiResponse(responseCode = "201", description = "Revanche aceita — nova partida criada"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "404", description = "Partida não encontrada"),
        @ApiResponse(responseCode = "409", description = "Partida não está finalizada ou revanche já solicitada")
    })
    public ResponseEntity<RematchResponse> requestRematch(@PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails);
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        RematchResponse response = gameService.requestRematch(id, userId);

        UUID opponentId = gameService.getOpponentId(id, userId);

        if (response.status() == RematchResponse.RematchStatus.MATCHED) {
            if (opponentId != null) {
                notificationService.notifyRematchMatched(id, response.gameId());
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        if (opponentId != null) {
            notificationService.notifyRematchInvite(opponentId, new RematchInvite(id, user.getUsername()));
        }
        return ResponseEntity.ok(response);
    }

    private UUID resolveUserId(UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return user.getId();
    }
}
