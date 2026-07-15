package com.softexpert.batalhanaval_api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info =
                @Info(
                        title = "Batalha Naval API",
                        version = "1.0.0",
                        description =
                                "API REST do jogo Batalha Naval multiplayer em tempo real. "
                                    + "Gerencia autenticação, matchmaking, partidas, habilidades especiais (modo Tempestade), "
                                    + "ranking e administração. A comunicação em tempo real (tiros, eventos de turno) "
                                    + "ocorre via WebSocket STOMP e não está coberta nesta documentação.",
                        contact = @Contact(name = "Equipe Batalha Naval", url = "https://github.com/softexpert")),
        servers = {@Server(url = "http://localhost:8080", description = "Desenvolvimento local")},
        tags = {
            @Tag(name = "Autenticação", description = "Registro e login de jogadores"),
            @Tag(name = "Partidas", description = "Criação, gerenciamento e histórico de partidas"),
            @Tag(name = "Salas", description = "Salas privadas com convite por token"),
            @Tag(name = "Habilidades", description = "Habilidades especiais do modo Tempestade"),
            @Tag(name = "Tempestade", description = "Eventos climáticos do modo Tempestade"),
            @Tag(name = "Perfil", description = "Perfil e estatísticas de jogadores"),
            @Tag(name = "Ranking", description = "Ranking global de jogadores"),
            @Tag(name = "Admin — Usuários", description = "Gestão administrativa de usuários"),
            @Tag(name = "Admin — Partidas", description = "Gestão administrativa de partidas"),
            @Tag(name = "Admin — Auditoria", description = "Logs de auditoria do painel administrativo")
        })
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token JWT obtido via POST /api/auth/login",
        in = SecuritySchemeIn.HEADER)
public class OpenApiConfig {}
