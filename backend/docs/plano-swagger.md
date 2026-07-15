# Plano de Implementação — Documentação Swagger/OpenAPI

## Visão Geral

O projeto já possui a dependência `springdoc-openapi-starter-webmvc-ui 3.0.2` no `pom.xml` e os paths do Swagger estão liberados no `SecurityConfig`. Porém **nenhuma configuração ou anotação OpenAPI existe** atualmente. Este plano detalha todas as alterações necessárias.

---

## Prioridade 1 — Configuração Global

### Arquivo a criar: `config/OpenApiConfig.java`

**Pacote:** `com.softexpert.batalhanaval_api.config`

**Conteúdo necessário:**


```java
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Batalha Naval API",
        version = "1.0.0",
        description = "API do jogo Batalha Naval multiplayer online com modos Clássico e Tempestade, "
            + "sistema de ranking ELO, habilidades especiais e painel administrativo.",
        contact = @Contact(name = "Equipe Batalha Naval", email = "contato@batalhanaval.com")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Desenvolvimento local")
    },
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Token JWT obtido via POST /api/auth/login"
)
public class OpenApiConfig {
}
```


**Imports necessários:**
- `io.swagger.v3.oas.annotations.OpenAPIDefinition`
- `io.swagger.v3.oas.annotations.info.Info`
- `io.swagger.v3.oas.annotations.info.Contact`
- `io.swagger.v3.oas.annotations.servers.Server`
- `io.swagger.v3.oas.annotations.security.SecurityRequirement`
- `io.swagger.v3.oas.annotations.security.SecurityScheme`
- `io.swagger.v3.oas.annotations.enums.SecuritySchemeType`
- `org.springframework.context.annotation.Configuration`

### Propriedade opcional no `application-dev.yaml`

```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: method
  api-docs:
    path: /v3/api-docs
```

---

## Prioridade 2 — Organização por Tags

Cada controller recebe `@Tag` na classe. A tabela abaixo define o mapeamento:


| Controller | Tag `name` | Tag `description` |
|---|---|---|
| `AuthController` | `Autenticação` | `Registro e login de jogadores` |
| `GameController` | `Partidas` | `Criar, consultar e gerenciar partidas` |
| `RoomController` | `Salas` | `Salas privadas para partidas entre amigos` |
| `AbilityController` | `Habilidades` | `Habilidades especiais do modo Tempestade` |
| `StormController` | `Tempestade` | `Eventos de tempestade durante a partida` |
| `ProfileController` | `Perfil` | `Perfil e estatísticas do jogador` |
| `RankingController` | `Ranking` | `Classificação global por ELO` |
| `AdminUserController` | `Admin — Usuários` | `Gerenciamento de usuários (ban, suspensão)` |
| `AdminGameController` | `Admin — Partidas` | `Monitoramento e encerramento forçado de partidas` |
| `AdminAuditController` | `Admin — Auditoria` | `Log de ações administrativas` |

**Anotação a adicionar em cada controller (exemplo):**

```java
@Tag(name = "Autenticação", description = "Registro e login de jogadores")
```

**Import:** `io.swagger.v3.oas.annotations.tags.Tag`

---

## Prioridade 3 — Descrição de Endpoints

### AuthController (`/api/auth`)


**Não requer `@SecurityRequirement`** — endpoints públicos.

| Método | Anotação |
|---|---|
| `register` | `@Operation(summary = "Registrar novo jogador", description = "Cria uma conta com username, email e senha. Retorna token JWT para uso imediato.")` |
| | `@ApiResponse(responseCode = "201", description = "Jogador registrado com sucesso")` |
| | `@ApiResponse(responseCode = "409", description = "Username ou email já cadastrado")` |
| | `@ApiResponse(responseCode = "400", description = "Dados inválidos")` |
| `login` | `@Operation(summary = "Autenticar jogador", description = "Realiza login com username e senha. Retorna token JWT.")` |
| | `@ApiResponse(responseCode = "200", description = "Login realizado com sucesso")` |
| | `@ApiResponse(responseCode = "401", description = "Credenciais inválidas")` |

**Remover segurança global nestes métodos:** Adicionar `security = {}` dentro de `@Operation` para indicar que não exigem autenticação:
```java
@Operation(summary = "...", security = {})
```

---

### GameController (`/api/games`)

**Todos os métodos (exceto `getFleetConfig`) requerem autenticação — herdam a segurança global.**


| Método | Anotação |
|---|---|
| `getFleetConfig` | `@Operation(summary = "Configuração da frota", description = "Retorna os tipos de navios disponíveis com nome e tamanho. Endpoint público.", security = {})` |
| | `@ApiResponse(responseCode = "200", description = "Lista de tipos de navio")` |
| `getActiveGame` | `@Operation(summary = "Partida ativa", description = "Retorna a partida ativa do jogador autenticado, se existir.")` |
| | `@ApiResponse(responseCode = "200", description = "Partida ativa encontrada")` |
| | `@ApiResponse(responseCode = "204", description = "Nenhuma partida ativa")` |
| `getGameHistory` | `@Operation(summary = "Histórico de partidas", description = "Lista paginada das partidas finalizadas do jogador.")` |
| | `@ApiResponse(responseCode = "200", description = "Página do histórico")` |
| `createOrJoinGame` | `@Operation(summary = "Criar ou entrar em partida", description = "Cria uma nova partida ou entra em uma existente no modo especificado (matchmaking automático).")` |
| | `@ApiResponse(responseCode = "201", description = "Partida criada ou pareada")` |
| | `@ApiResponse(responseCode = "409", description = "Jogador já possui partida ativa")` |
| `getGameState` | `@Operation(summary = "Estado da partida", description = "Retorna o estado completo da partida incluindo tabuleiros do jogador.")` |
| | `@ApiResponse(responseCode = "200", description = "Estado da partida")` |
| | `@ApiResponse(responseCode = "404", description = "Partida não encontrada")` |
| | `@ApiResponse(responseCode = "403", description = "Jogador não participa desta partida")` |
| `placeShips` | `@Operation(summary = "Posicionar navios", description = "Envia o posicionamento dos 5 navios no tabuleiro. Deve ser feito na fase PLACING.")` |
| | `@ApiResponse(responseCode = "200", description = "Navios posicionados com sucesso")` |
| | `@ApiResponse(responseCode = "400", description = "Posicionamento inválido")` |
| | `@ApiResponse(responseCode = "409", description = "Navios já posicionados ou fase incorreta")` |
| `cancelGame` | `@Operation(summary = "Cancelar partida", description = "Cancela uma partida na fase de espera ou posicionamento. Não é possível cancelar partidas em andamento.")` |
| | `@ApiResponse(responseCode = "204", description = "Partida cancelada")` |
| | `@ApiResponse(responseCode = "409", description = "Partida não pode ser cancelada neste estado")` |
| `surrender` | `@Operation(summary = "Render-se", description = "Desiste da partida em andamento. O oponente é declarado vencedor.")` |
| | `@ApiResponse(responseCode = "204", description = "Rendição registrada")` |
| | `@ApiResponse(responseCode = "409", description = "Partida não está em andamento")` |
| `requestRematch` | `@Operation(summary = "Solicitar revanche", description = "Envia convite de revanche ao oponente. Se ambos solicitarem, uma nova partida é criada automaticamente.")` |
| | `@ApiResponse(responseCode = "200", description = "Convite enviado, aguardando oponente")` |
| | `@ApiResponse(responseCode = "201", description = "Revanche pareada — nova partida criada")` |
| | `@ApiResponse(responseCode = "409", description = "Revanche já solicitada ou partida inválida")` |

---

### RoomController (`/api/rooms`)

**Todos os métodos requerem autenticação.**


| Método | Anotação |
|---|---|
| `createRoom` | `@Operation(summary = "Criar sala privada", description = "Cria uma sala privada com token único para convidar um oponente.")` |
| | `@ApiResponse(responseCode = "201", description = "Sala criada com token")` |
| | `@ApiResponse(responseCode = "409", description = "Jogador já possui partida ativa")` |
| `joinRoom` | `@Operation(summary = "Entrar em sala", description = "Entra numa sala privada usando o token compartilhado pelo anfitrião.")` |
| | `@ApiResponse(responseCode = "200", description = "Entrou na sala com sucesso")` |
| | `@ApiResponse(responseCode = "404", description = "Token inválido ou sala não encontrada")` |
| | `@ApiResponse(responseCode = "409", description = "Sala cheia ou jogador já possui partida ativa")` |
| `confirmReady` | `@Operation(summary = "Confirmar prontidão", description = "Confirma que o jogador está pronto. Quando ambos confirmam, a partida inicia.")` |
| | `@ApiResponse(responseCode = "200", description = "Prontidão confirmada")` |
| | `@ApiResponse(responseCode = "409", description = "Jogador já confirmou ou sala em estado inválido")` |
| `getRoomState` | `@Operation(summary = "Estado da sala", description = "Retorna o estado atual da sala incluindo jogadores e status de prontidão.")` |
| | `@ApiResponse(responseCode = "200", description = "Estado da sala")` |
| | `@ApiResponse(responseCode = "404", description = "Sala não encontrada")` |
| `cancelRoom` | `@Operation(summary = "Cancelar sala", description = "Cancela a sala privada. Apenas o anfitrião pode cancelar antes do início.")` |
| | `@ApiResponse(responseCode = "204", description = "Sala cancelada")` |
| | `@ApiResponse(responseCode = "403", description = "Apenas o anfitrião pode cancelar")` |

---

### AbilityController (`/api/games/{gameId}/ability`)

**Todos os métodos requerem autenticação.**


| Método | Anotação |
|---|---|
| `getAbility` | `@Operation(summary = "Consultar habilidade", description = "Retorna a habilidade sorteada para o jogador nesta partida (modo Tempestade). Inclui se já foi utilizada.")` |
| | `@ApiResponse(responseCode = "200", description = "Status da habilidade")` |
| | `@ApiResponse(responseCode = "404", description = "Partida não encontrada ou não é modo Tempestade")` |
| `useAbility` | `@Operation(summary = "Usar habilidade", description = "Ativa a habilidade especial do jogador. Cada habilidade tem parâmetros diferentes (coordenadas, eixo, índice).")` |
| | `@ApiResponse(responseCode = "200", description = "Resultado da habilidade")` |
| | `@ApiResponse(responseCode = "400", description = "Parâmetros inválidos para a habilidade")` |
| | `@ApiResponse(responseCode = "409", description = "Habilidade já utilizada ou não é seu turno")` |

---

### StormController (`/api/games/{gameId}/storm`)

**Requer autenticação.**

| Método | Anotação |
|---|---|
| `getNextStorm` | `@Operation(summary = "Próxima tempestade", description = "Retorna informações sobre a próxima tempestade: turno previsto, turno atual e turnos restantes.")` |
| | `@ApiResponse(responseCode = "200", description = "Informações da tempestade")` |
| | `@ApiResponse(responseCode = "404", description = "Partida não encontrada")` |

---

### ProfileController (`/api/users`)

**Requer autenticação.**


| Método | Anotação |
|---|---|
| `getMyProfile` | `@Operation(summary = "Meu perfil", description = "Retorna o perfil completo do jogador autenticado com estatísticas e partidas recentes.")` |
| | `@ApiResponse(responseCode = "200", description = "Perfil do jogador")` |
| `getProfile` | `@Operation(summary = "Perfil de jogador", description = "Retorna o perfil público de qualquer jogador pelo ID.")` |
| | `@ApiResponse(responseCode = "200", description = "Perfil do jogador")` |
| | `@ApiResponse(responseCode = "404", description = "Jogador não encontrado")` |

---

### RankingController (`/api/ranking`)

**Requer autenticação.**

| Método | Anotação |
|---|---|
| `getRanking` | `@Operation(summary = "Ranking global", description = "Retorna a classificação paginada por ELO. Inclui a posição do jogador autenticado. Filtro por período: all, weekly, monthly.")` |
| | `@ApiResponse(responseCode = "200", description = "Ranking com posição do jogador")` |

---

### AdminUserController (`/api/admin/users`)

**Requer autenticação + role ADMIN.**

| Método | Anotação |
|---|---|
| `listUsers` | `@Operation(summary = "Listar usuários", description = "Lista paginada de todos os usuários. Filtro opcional por status (ACTIVE, SUSPENDED, BANNED).")` |
| | `@ApiResponse(responseCode = "200", description = "Lista de usuários")` |
| | `@ApiResponse(responseCode = "403", description = "Acesso restrito a administradores")` |
| `ban` | `@Operation(summary = "Banir usuário", description = "Bane permanentemente um jogador. Ação registrada no log de auditoria.")` |
| | `@ApiResponse(responseCode = "200", description = "Usuário banido")` |
| | `@ApiResponse(responseCode = "404", description = "Usuário não encontrado")` |
| `suspend` | `@Operation(summary = "Suspender usuário", description = "Suspende um jogador até a data informada. Ação registrada no log de auditoria.")` |
| | `@ApiResponse(responseCode = "200", description = "Usuário suspenso")` |
| | `@ApiResponse(responseCode = "400", description = "Data inválida (deve ser futura)")` |
| | `@ApiResponse(responseCode = "404", description = "Usuário não encontrado")` |
| `reactivate` | `@Operation(summary = "Reativar usuário", description = "Remove ban ou suspensão de um jogador. Ação registrada no log de auditoria.")` |
| | `@ApiResponse(responseCode = "200", description = "Usuário reativado")` |
| | `@ApiResponse(responseCode = "404", description = "Usuário não encontrado")` |

---

### AdminGameController (`/api/admin/games`)

**Requer autenticação + role ADMIN.**


| Método | Anotação |
|---|---|
| `listActiveGames` | `@Operation(summary = "Listar partidas ativas", description = "Lista paginada de partidas em andamento para monitoramento administrativo.")` |
| | `@ApiResponse(responseCode = "200", description = "Lista de partidas ativas")` |
| | `@ApiResponse(responseCode = "403", description = "Acesso restrito a administradores")` |
| `revealBoards` | `@Operation(summary = "Revelar tabuleiros", description = "Exibe ambos os tabuleiros completos de uma partida para investigação. Ação registrada no log de auditoria.")` |
| | `@ApiResponse(responseCode = "200", description = "Tabuleiros revelados")` |
| | `@ApiResponse(responseCode = "404", description = "Partida não encontrada")` |
| `forceEnd` | `@Operation(summary = "Encerrar partida forçadamente", description = "Força o encerramento de uma partida ativa sem declarar vencedor. Ação registrada no log de auditoria.")` |
| | `@ApiResponse(responseCode = "200", description = "Partida encerrada")` |
| | `@ApiResponse(responseCode = "404", description = "Partida não encontrada")` |
| | `@ApiResponse(responseCode = "409", description = "Partida já finalizada")` |

---

### AdminAuditController (`/api/admin/audit-log`)

**Requer autenticação + role ADMIN.**

| Método | Anotação |
|---|---|
| `listAuditLogs` | `@Operation(summary = "Log de auditoria", description = "Lista paginada de todas as ações administrativas (bans, suspensões, encerramentos forçados).")` |
| | `@ApiResponse(responseCode = "200", description = "Lista de logs")` |
| | `@ApiResponse(responseCode = "403", description = "Acesso restrito a administradores")` |

---

## Prioridade 4 — Exemplos nos DTOs

### Request DTOs

#### `LoginRequest`
```java
@Schema(description = "Credenciais de login")
public record LoginRequest(
    @Schema(description = "Nome de usuário", example = "almirante_nelson")
    @NotBlank String username,
    @Schema(description = "Senha do jogador", example = "Senha@123")
    @NotBlank String password
) {}
```


#### `RegisterRequest`
```java
@Schema(description = "Dados para registro de novo jogador")
public record RegisterRequest(
    @Schema(description = "Nome de usuário (letras, números e underscore)", example = "almirante_nelson")
    @NotBlank @Size(min = 3, max = 30) @Pattern(...) String username,

    @Schema(description = "Email do jogador", example = "nelson@marinha.com")
    @NotBlank @Email @Size(max = 100) @Pattern(...) String email,

    @Schema(description = "Senha (mínimo 6 caracteres, ao menos 1 símbolo)", example = "Frota@2025")
    @NotBlank @Size(min = 6, max = 100) @Pattern(...) String password
) {}
```

#### `CreateGameRequest`
```java
@Schema(description = "Configuração para criar ou entrar em partida")
public record CreateGameRequest(
    @Schema(description = "Modo de jogo", example = "CLASSIC")
    @NotNull GameMode gameMode
) {}
```

#### `PlaceShipsRequest`
```java
@Schema(description = "Posicionamento dos 5 navios no tabuleiro")
public record PlaceShipsRequest(
    @Schema(description = "Lista com exatamente 5 navios posicionados")
    @NotNull @Size(min = 5, max = 5) List<@Valid ShipPlacement> ships
) {}
```

#### `ShipPlacement`
```java
@Schema(description = "Posição de um navio individual no tabuleiro 10x10")
public record ShipPlacement(
    @Schema(description = "Tipo do navio", example = "CARRIER")
    @NotNull ShipType shipType,
    @Schema(description = "Linha de origem (0-9)", example = "2")
    @Min(0) @Max(9) int originRow,
    @Schema(description = "Coluna de origem (0-9)", example = "3")
    @Min(0) @Max(9) int originCol,
    @Schema(description = "Orientação do navio", example = "HORIZONTAL")
    @NotNull Orientation orientation
) {}
```

#### `UseAbilityRequest`
```java
@Schema(description = "Ativação de habilidade especial (modo Tempestade)")
public record UseAbilityRequest(
    @Schema(description = "Tipo da habilidade a usar", example = "RADAR")
    @NotNull AbilityType abilityType,
    @Schema(description = "Linha alvo (para RADAR e DOUBLE_TORPEDO)", example = "4")
    Integer row,
    @Schema(description = "Coluna alvo (para RADAR e DOUBLE_TORPEDO)", example = "5")
    Integer col,
    @Schema(description = "Eixo para LINE_BOMBARDMENT: ROW ou COL", example = "ROW")
    String axis,
    @Schema(description = "Índice da linha/coluna para LINE_BOMBARDMENT (0-9)", example = "3")
    Integer index
) {}
```


#### `CreateRoomRequest`
```java
@Schema(description = "Configuração para criar sala privada")
public record CreateRoomRequest(
    @Schema(description = "Modo de jogo da sala", example = "STORM")
    @NotNull GameMode gameMode
) {}
```

#### `JoinRoomRequest`
```java
@Schema(description = "Token para entrar em sala privada")
public record JoinRoomRequest(
    @Schema(description = "Token único da sala (compartilhado pelo anfitrião)", example = "ABC123XY")
    @NotBlank String token
) {}
```

#### `FireRequest`
```java
@Schema(description = "Coordenadas do disparo")
public record FireRequest(
    @Schema(description = "Linha alvo (0-9)", example = "7")
    @Min(0) @Max(9) int row,
    @Schema(description = "Coluna alvo (0-9)", example = "4")
    @Min(0) @Max(9) int col
) {}
```

#### `SuspendRequest`
```java
@Schema(description = "Dados para suspensão de usuário")
public record SuspendRequest(
    @Schema(description = "Data/hora até quando a suspensão é válida (ISO-8601)", example = "2025-08-15T23:59:59Z")
    @NotNull @Future Instant suspendedUntil
) {}
```

---

### Response DTOs

#### `AuthResponse`
```java
@Schema(description = "Resposta de autenticação com token JWT")
public record AuthResponse(
    @Schema(description = "ID do jogador", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,
    @Schema(description = "Nome de usuário", example = "almirante_nelson")
    String username,
    @Schema(description = "Email", example = "nelson@marinha.com")
    String email,
    @Schema(description = "Papel do usuário", example = "PLAYER")
    UserRole role,
    @Schema(description = "Token JWT para autenticação", example = "eyJhbGciOiJIUzI1NiJ9...")
    String token
) {}
```


#### `GameResponse`
```java
@Schema(description = "Estado completo de uma partida")
public record GameResponse(
    @Schema(description = "ID da partida") UUID id,
    @Schema(description = "Status atual da partida", example = "IN_PROGRESS") GameStatus status,
    @Schema(description = "Modo de jogo", example = "CLASSIC") GameMode gameMode,
    @Schema(description = "Jogador 1") PlayerSummary player1,
    @Schema(description = "Jogador 2 (null se aguardando)") PlayerSummary player2,
    @Schema(description = "ID do jogador com turno atual") UUID currentTurnPlayerId,
    @Schema(description = "ID do vencedor (null se não finalizada)") UUID winnerId,
    @Schema(description = "Tabuleiro do jogador autenticado") BoardResponse myBoard,
    @Schema(description = "Tabuleiro do oponente (visão parcial)") OpponentBoardResponse opponentBoard,
    @Schema(description = "Variação de ELO após fim da partida") Integer eloDelta,
    @Schema(description = "Data de criação") Instant createdAt,
    @Schema(description = "Prazo para posicionar navios") Instant placementDeadline
) {}
```

#### `RoomResponse`
```java
@Schema(description = "Estado de uma sala privada")
public record RoomResponse(
    @Schema(description = "ID da partida associada (quando iniciada)") UUID gameId,
    @Schema(description = "Token para compartilhar com oponente", example = "ABC123XY") String token,
    @Schema(description = "Modo de jogo", example = "CLASSIC") GameMode gameMode,
    @Schema(description = "Username do anfitrião", example = "almirante_nelson") String hostUsername,
    @Schema(description = "Username do convidado", example = "capitao_hook") String guestUsername,
    @Schema(description = "Anfitrião confirmou prontidão") boolean hostReady,
    @Schema(description = "Convidado confirmou prontidão") boolean guestReady,
    @Schema(description = "Status da sala", example = "WAITING_OPPONENT") RoomStatus status
) {}
```

#### `PlayerProfileResponse`
```java
@Schema(description = "Perfil completo do jogador com estatísticas")
public record PlayerProfileResponse(
    @Schema(description = "ID do jogador") UUID id,
    @Schema(description = "Username", example = "almirante_nelson") String username,
    @Schema(description = "Rating ELO atual", example = "1250") int eloRating,
    @Schema(description = "Total de partidas jogadas", example = "47") long totalGames,
    @Schema(description = "Vitórias", example = "28") long wins,
    @Schema(description = "Derrotas", example = "19") long losses,
    @Schema(description = "Taxa de vitória (0.0 a 1.0)", example = "0.5957") double winRate,
    @Schema(description = "Posição no ranking global", example = "12") int rank,
    @Schema(description = "Data de criação da conta") Instant memberSince,
    @Schema(description = "Total de tiros disparados", example = "523") long totalShots,
    @Schema(description = "Tiros que acertaram", example = "187") long shotsHit,
    @Schema(description = "Navios afundados", example = "42") long shipsSunk,
    @Schema(description = "Precisão de tiro (0.0 a 1.0)", example = "0.3576") double accuracy,
    @Schema(description = "Últimas partidas jogadas") List<GameHistoryEntry> recentGames
) {}
```


#### `RankingResponse`
```java
@Schema(description = "Ranking global com posição do jogador")
public record RankingResponse(
    @Schema(description = "Entradas do ranking (página atual)") List<RankingEntry> ranking,
    @Schema(description = "Posição do jogador autenticado") RankingEntry myPosition,
    @Schema(description = "Número da página atual", example = "0") int page,
    @Schema(description = "Tamanho da página", example = "20") int size,
    @Schema(description = "Total de jogadores no ranking", example = "150") long totalElements,
    @Schema(description = "Total de páginas", example = "8") int totalPages
) {}
```

#### `ErrorResponse`
```java
@Schema(description = "Resposta de erro padrão da API")
public record ErrorResponse(
    @Schema(description = "Código do erro", example = "GAME_NOT_FOUND") String code,
    @Schema(description = "Mensagem descritiva", example = "Partida não encontrada") String message,
    @Schema(description = "Momento do erro") Instant timestamp
) {}
```

#### `PageResponse<T>`
```java
@Schema(description = "Resposta paginada genérica")
public record PageResponse<T>(
    @Schema(description = "Itens da página atual") List<T> content,
    @Schema(description = "Número da página (zero-based)", example = "0") int page,
    @Schema(description = "Tamanho da página", example = "10") int size,
    @Schema(description = "Total de elementos", example = "42") long totalElements,
    @Schema(description = "Total de páginas", example = "5") int totalPages
) {}
```

#### `AbilityStatusResponse`
```java
@Schema(description = "Status da habilidade do jogador na partida")
public record AbilityStatusResponse(
    @Schema(description = "Tipo da habilidade sorteada", example = "RADAR") AbilityType abilityType,
    @Schema(description = "Nome de exibição", example = "Radar") String name,
    @Schema(description = "Descrição da habilidade", example = "Revela presença de navios em área 3x3") String description,
    @Schema(description = "Se já foi utilizada nesta partida") boolean used,
    @Schema(description = "Turno em que foi usada (null se não usada)") Integer usedOnTurn
) {}
```


#### `StormInfoResponse`
```java
@Schema(description = "Informações sobre a próxima tempestade")
public record StormInfoResponse(
    @Schema(description = "Turno em que a próxima tempestade ocorrerá", example = "8") int nextStormTurn,
    @Schema(description = "Turno atual da partida", example = "5") int currentTurn,
    @Schema(description = "Turnos restantes até a tempestade", example = "3") int turnsUntilStorm
) {}
```

#### `FleetConfigResponse`
```java
@Schema(description = "Configuração de um tipo de navio")
public record FleetConfigResponse(
    @Schema(description = "Identificador do tipo", example = "CARRIER") String type,
    @Schema(description = "Tamanho em células", example = "5") int size,
    @Schema(description = "Nome de exibição", example = "Porta-aviões") String name
) {}
```

#### `GameHistoryEntry`
```java
@Schema(description = "Entrada resumida de partida no histórico")
public record GameHistoryEntry(
    @Schema(description = "ID da partida") UUID id,
    @Schema(description = "Username do oponente", example = "capitao_hook") String opponentUsername,
    @Schema(description = "Status final da partida", example = "FINISHED") GameStatus status,
    @Schema(description = "Se o jogador venceu") boolean won,
    @Schema(description = "Duração em segundos", example = "324") long durationSeconds,
    @Schema(description = "Data/hora da partida") Instant playedAt
) {}
```

#### `AdminUserResponse`
```java
@Schema(description = "Dados de usuário para painel administrativo")
public record AdminUserResponse(
    @Schema(description = "ID do usuário") UUID id,
    @Schema(description = "Username", example = "almirante_nelson") String username,
    @Schema(description = "Email", example = "nelson@marinha.com") String email,
    @Schema(description = "Papel", example = "PLAYER") UserRole role,
    @Schema(description = "Status atual", example = "ACTIVE") UserStatus status,
    @Schema(description = "Suspenso até (null se não suspenso)") Instant suspendedUntil,
    @Schema(description = "Data de cadastro") Instant createdAt
) {}
```

#### `AdminGameResponse`
```java
@Schema(description = "Dados de partida para painel administrativo")
public record AdminGameResponse(
    @Schema(description = "ID da partida") UUID id,
    @Schema(description = "Status", example = "IN_PROGRESS") GameStatus status,
    @Schema(description = "Modo de jogo", example = "STORM") GameMode gameMode,
    @Schema(description = "Jogador 1") PlayerSummary player1,
    @Schema(description = "Jogador 2") PlayerSummary player2,
    @Schema(description = "Data de criação") Instant createdAt,
    @Schema(description = "Última atualização") Instant updatedAt
) {}
```


#### `AdminAuditLogResponse`
```java
@Schema(description = "Registro de ação administrativa")
public record AdminAuditLogResponse(
    @Schema(description = "ID do log") UUID id,
    @Schema(description = "ID do admin que executou") UUID adminId,
    @Schema(description = "Username do admin", example = "admin_master") String adminUsername,
    @Schema(description = "Ação realizada", example = "BAN_USER") String action,
    @Schema(description = "Tipo do alvo", example = "USER") String targetType,
    @Schema(description = "ID do alvo") UUID targetId,
    @Schema(description = "Detalhes adicionais", example = "Motivo: comportamento abusivo") String details,
    @Schema(description = "Data/hora da ação") Instant createdAt
) {}
```

#### `RematchResponse`
```java
@Schema(description = "Resultado de solicitação de revanche")
public record RematchResponse(
    @Schema(description = "Status da revanche", example = "WAITING") RematchStatus status,
    @Schema(description = "ID da nova partida (quando MATCHED)") UUID gameId
) {}
```

#### `PlaceShipsResponse`
```java
@Schema(description = "Resultado do posicionamento de navios")
public record PlaceShipsResponse(
    @Schema(description = "Mensagem de confirmação", example = "Navios posicionados com sucesso") String message,
    @Schema(description = "Tabuleiro está pronto") boolean boardReady,
    @Schema(description = "Status atualizado da partida", example = "IN_PROGRESS") GameStatus gameStatus
) {}
```

---

## Resumo de Arquivos a Modificar

| # | Arquivo | Alteração |
|---|---|---|
| 1 | **CRIAR** `config/OpenApiConfig.java` | Classe com `@OpenAPIDefinition` + `@SecurityScheme` |
| 2 | `application-dev.yaml` | Adicionar bloco `springdoc:` |
| 3 | `controller/AuthController.java` | `@Tag` na classe + `@Operation`/`@ApiResponse` nos métodos |
| 4 | `controller/GameController.java` | `@Tag` na classe + `@Operation`/`@ApiResponse` nos métodos |
| 5 | `controller/RoomController.java` | `@Tag` na classe + `@Operation`/`@ApiResponse` nos métodos |
| 6 | `controller/AbilityController.java` | `@Tag` na classe + `@Operation`/`@ApiResponse` nos métodos |
| 7 | `controller/StormController.java` | `@Tag` na classe + `@Operation`/`@ApiResponse` nos métodos |
| 8 | `controller/ProfileController.java` | `@Tag` na classe + `@Operation`/`@ApiResponse` nos métodos |
| 9 | `controller/RankingController.java` | `@Tag` na classe + `@Operation`/`@ApiResponse` nos métodos |
| 10 | `controller/AdminUserController.java` | `@Tag` na classe + `@Operation`/`@ApiResponse` nos métodos |
| 11 | `controller/AdminGameController.java` | `@Tag` na classe + `@Operation`/`@ApiResponse` nos métodos |
| 12 | `controller/AdminAuditController.java` | `@Tag` na classe + `@Operation`/`@ApiResponse` nos métodos |
| 13 | `dto/request/LoginRequest.java` | `@Schema` na classe e campos |
| 14 | `dto/request/RegisterRequest.java` | `@Schema` na classe e campos |
| 15 | `dto/request/CreateGameRequest.java` | `@Schema` na classe e campos |
| 16 | `dto/request/PlaceShipsRequest.java` | `@Schema` na classe e campos |
| 17 | `dto/request/ShipPlacement.java` | `@Schema` na classe e campos |
| 18 | `dto/request/UseAbilityRequest.java` | `@Schema` na classe e campos |
| 19 | `dto/request/CreateRoomRequest.java` | `@Schema` na classe e campos |
| 20 | `dto/request/JoinRoomRequest.java` | `@Schema` na classe e campos |
| 21 | `dto/request/FireRequest.java` | `@Schema` na classe e campos |
| 22 | `dto/request/SuspendRequest.java` | `@Schema` na classe e campos |
| 23 | `dto/response/AuthResponse.java` | `@Schema` na classe e campos |
| 24 | `dto/response/GameResponse.java` | `@Schema` na classe e campos |
| 25 | `dto/response/RoomResponse.java` | `@Schema` na classe e campos |
| 26 | `dto/response/PlayerProfileResponse.java` | `@Schema` na classe e campos |
| 27 | `dto/response/RankingResponse.java` | `@Schema` na classe e campos |
| 28 | `dto/response/ErrorResponse.java` | `@Schema` na classe e campos |
| 29 | `dto/response/PageResponse.java` | `@Schema` na classe e campos |
| 30 | `dto/response/AbilityStatusResponse.java` | `@Schema` na classe e campos |
| 31 | `dto/response/StormInfoResponse.java` | `@Schema` na classe e campos |
| 32 | `dto/response/FleetConfigResponse.java` | `@Schema` na classe e campos |
| 33 | `dto/response/GameHistoryEntry.java` | `@Schema` na classe e campos |
| 34 | `dto/response/AdminUserResponse.java` | `@Schema` na classe e campos |
| 35 | `dto/response/AdminGameResponse.java` | `@Schema` na classe e campos |
| 36 | `dto/response/AdminAuditLogResponse.java` | `@Schema` na classe e campos |
| 37 | `dto/response/RematchResponse.java` | `@Schema` na classe e campos |
| 38 | `dto/response/PlaceShipsResponse.java` | `@Schema` na classe e campos |

---

## Regras de Implementação

1. Todo texto visível (summary, description, exemplos) em **pt-BR**
2. Não adicionar comentários no código — anotações Swagger são metadados, não comentários
3. Não alterar comportamento funcional (apenas adicionar anotações)
4. `@Schema` com `description` e `example` em campos primitivos; apenas `description` em campos complexos
5. Ocultar o parâmetro `@AuthenticationPrincipal UserDetails` do Swagger com `@Parameter(hidden = true)`
6. Para endpoints públicos (`/api/auth/**` e `/api/games/fleet-config`), usar `security = {}` no `@Operation`
7. Para endpoints admin, a segurança global (`bearerAuth`) já cobre — não precisa repetir `@SecurityRequirement`

---

## Import Padrão por Tipo de Anotação

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
```

---

## Ordem de Execução Recomendada

1. Criar `OpenApiConfig.java` + configurar `application-dev.yaml`
2. Adicionar `@Tag` em todos os controllers
3. Adicionar `@Operation`/`@ApiResponse`/`@Parameter(hidden=true)` nos métodos dos controllers
4. Adicionar `@Schema` nos DTOs de request
5. Adicionar `@Schema` nos DTOs de response
6. Testar acessando `http://localhost:8080/swagger-ui.html`
