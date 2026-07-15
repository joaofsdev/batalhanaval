package com.softexpert.batalhanaval_api.controller;

import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.dto.request.CreateRoomRequest;
import com.softexpert.batalhanaval_api.dto.request.JoinRoomRequest;
import com.softexpert.batalhanaval_api.dto.response.RoomResponse;
import com.softexpert.batalhanaval_api.repository.UserRepository;
import com.softexpert.batalhanaval_api.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Salas")
@SecurityRequirement(name = "bearerAuth")
public class RoomController {

    private final RoomService roomService;
    private final UserRepository userRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Criar sala privada",
            description = "Cria uma sala privada com token de convite. O criador aguarda um oponente entrar via token.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Sala criada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "409", description = "Jogador já possui partida ou sala ativa")
    })
    public RoomResponse createRoom(
        @Valid @RequestBody CreateRoomRequest request,
        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = resolveUserId(userDetails);
        return roomService.createRoom(userId, request.gameMode());
    }

    @PostMapping("/join")
    @Operation(
            summary = "Entrar em sala via token",
            description = "Entra em uma sala privada existente usando o token de convite compartilhado pelo criador.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entrou na sala com sucesso"),
        @ApiResponse(responseCode = "400", description = "Token inválido ou ausente"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "404", description = "Sala não encontrada"),
        @ApiResponse(responseCode = "409", description = "Sala já está cheia ou jogador já possui partida ativa")
    })
    public RoomResponse joinRoom(
        @Valid @RequestBody JoinRoomRequest request,
        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = resolveUserId(userDetails);
        return roomService.joinRoom(userId, request.token());
    }

    @PostMapping("/{id}/ready")
    @Operation(
            summary = "Confirmar prontidão",
            description = "O jogador confirma que está pronto para iniciar a partida. Quando ambos confirmam, a partida é criada.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Prontidão confirmada"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "404", description = "Sala não encontrada"),
        @ApiResponse(responseCode = "409", description = "Jogador não pertence a esta sala")
    })
    public RoomResponse confirmReady(
        @PathVariable UUID id,
        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = resolveUserId(userDetails);
        return roomService.confirmReady(id, userId);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obter estado da sala",
            description = "Retorna o estado atual da sala privada (jogadores, prontidão, token de convite).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado da sala retornado"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "404", description = "Sala não encontrada")
    })
    public RoomResponse getRoomState(
        @PathVariable UUID id,
        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = resolveUserId(userDetails);
        return roomService.getRoomState(id, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Cancelar sala",
            description = "Cancela uma sala privada. Apenas o criador pode cancelar antes do início da partida.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sala cancelada"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
        @ApiResponse(responseCode = "404", description = "Sala não encontrada"),
        @ApiResponse(responseCode = "409", description = "Sala não pode ser cancelada no estado atual")
    })
    public void cancelRoom(
        @PathVariable UUID id,
        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = resolveUserId(userDetails);
        roomService.cancelRoom(id, userId);
    }

    private UUID resolveUserId(UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return user.getId();
    }
}
