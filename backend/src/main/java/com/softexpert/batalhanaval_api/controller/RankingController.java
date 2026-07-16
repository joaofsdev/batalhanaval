package com.softexpert.batalhanaval_api.controller;

import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.dto.response.RankingResponse;
import com.softexpert.batalhanaval_api.security.UserResolver;
import com.softexpert.batalhanaval_api.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
@Tag(name = "Ranking")
@SecurityRequirement(name = "bearerAuth")
public class RankingController {

    private final RankingService rankingService;
    private final UserResolver userResolver;

    @GetMapping
    @Operation(
            summary = "Obter ranking de jogadores",
            description = "Retorna o ranking global paginado, incluindo a posição do jogador autenticado. Filtrável por período (all, week, month).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ranking retornado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido")
    })
    public RankingResponse getRanking(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "all") String period,
        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        User currentUser = userResolver.resolveUser(userDetails);
        return rankingService.getRanking(currentUser.getId(), currentUser.getUsername(), page, size, period);
    }
}
