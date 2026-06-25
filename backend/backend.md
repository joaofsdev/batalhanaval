# Planejamento do Backend — Batalha Naval Multiplayer

---

## FASE 1 — Modelagem de Domínio

---

### 1.1 Entidades e Relacionamentos

#### Entidade: `User`

Representa um jogador registrado no sistema.

| Campo | Tipo Java | Constraints JPA | Notas |
|-------|-----------|-----------------|-------|
| `id` | `UUID` | `@Id @GeneratedValue(strategy = UUID)` | PK |
| `username` | `String` | `@Column(nullable = false, unique = true, length = 30)` | Login do jogador |
| `email` | `String` | `@Column(nullable = false, unique = true, length = 100)` | Email único |
| `passwordHash` | `String` | `@Column(nullable = false, length = 255)` | BCrypt hash |
| `createdAt` | `Instant` | `@Column(nullable = false, updatable = false)` | Timestamp de criação |

**Relacionamentos:**
- `@OneToMany(mappedBy = "player1", fetch = LAZY)` → `List<Game>` (partidas como player1)
- `@OneToMany(mappedBy = "player2", fetch = LAZY)` → `List<Game>` (partidas como player2)

**Índices:**
- Unique index em `username`
- Unique index em `email`

---

#### Entidade: `Game`

Representa uma partida entre dois jogadores.

| Campo | Tipo Java | Constraints JPA | Notas |
|-------|-----------|-----------------|-------|
| `id` | `UUID` | `@Id @GeneratedValue(strategy = UUID)` | PK |
| `player1` | `User` | `@ManyToOne(fetch = LAZY, optional = false)` | Quem criou a partida |
| `player2` | `User` | `@ManyToOne(fetch = LAZY)` | Pode ser null enquanto `WAITING` |
| `status` | `GameStatus` | `@Enumerated(STRING) @Column(nullable = false)` | Estado da partida |
| `currentTurn` | `User` | `@ManyToOne(fetch = LAZY)` | Jogador que deve jogar; null se não `IN_PROGRESS` |
| `winner` | `User` | `@ManyToOne(fetch = LAZY)` | Null até `FINISHED` |
| `createdAt` | `Instant` | `@Column(nullable = false, updatable = false)` | |
| `updatedAt` | `Instant` | `@Column(nullable = false)` | |

**Relacionamentos:**
- `@OneToMany(mappedBy = "game", fetch = LAZY, cascade = ALL, orphanRemoval = true)` → `List<Board>` (exatamente 2 boards por game)
- `@OneToMany(mappedBy = "game", fetch = LAZY, cascade = ALL, orphanRemoval = true)` → `List<Shot>` (histórico de tiros)

**Índices:**
- Index em `status` (para buscar partidas `WAITING` no matchmaking)
- Index em `player1_id`
- Index em `player2_id`

---

#### Entidade: `Board`

Um tabuleiro 10x10 pertence a um jogador em uma partida específica. **Nunca exposto ao oponente.**

| Campo | Tipo Java | Constraints JPA | Notas |
|-------|-----------|-----------------|-------|
| `id` | `UUID` | `@Id @GeneratedValue(strategy = UUID)` | PK |
| `game` | `Game` | `@ManyToOne(fetch = LAZY, optional = false)` | Partida a que pertence |
| `owner` | `User` | `@ManyToOne(fetch = LAZY, optional = false)` | Dono deste tabuleiro |
| `ready` | `boolean` | `@Column(nullable = false)` | `true` quando a frota completa foi posicionada |

**Relacionamentos:**
- `@OneToMany(mappedBy = "board", fetch = LAZY, cascade = ALL, orphanRemoval = true)` → `List<Ship>` (navios posicionados)
- `@OneToMany(mappedBy = "board", fetch = LAZY, cascade = ALL, orphanRemoval = true)` → `List<Cell>` (100 células)

**Índices:**
- Unique constraint composto em `(game_id, owner_id)` — cada jogador tem exatamente 1 board por game

**FetchType LAZY justificado:** Board carrega seus Ships e Cells apenas quando necessário (processamento de tiro). Evita carregar 100 células + navios em listagens de partida.


---

#### Entidade: `Ship`

Um navio posicionado no tabuleiro.

| Campo | Tipo Java | Constraints JPA | Notas |
|-------|-----------|-----------------|-------|
| `id` | `UUID` | `@Id @GeneratedValue(strategy = UUID)` | PK |
| `board` | `Board` | `@ManyToOne(fetch = LAZY, optional = false)` | Tabuleiro a que pertence |
| `shipType` | `ShipType` | `@Enumerated(STRING) @Column(nullable = false)` | Tipo do navio |
| `originRow` | `int` | `@Column(nullable = false)` | Linha da coordenada de origem (0-9) |
| `originCol` | `int` | `@Column(nullable = false)` | Coluna da coordenada de origem (0-9) |
| `orientation` | `Orientation` | `@Enumerated(STRING) @Column(nullable = false)` | `HORIZONTAL` ou `VERTICAL` |
| `hits` | `int` | `@Column(nullable = false)` | Quantidade de acertos recebidos |

**Campos derivados (não persistidos):**
- `isSunk()` → `hits >= shipType.getSize()`
- Coordenadas ocupadas calculadas a partir de `origin + orientation + size`

**Índices:**
- Index em `board_id`

---

#### Entidade: `Cell`

Cada célula do tabuleiro 10x10. Representa o estado individual de uma posição.

| Campo | Tipo Java | Constraints JPA | Notas |
|-------|-----------|-----------------|-------|
| `id` | `UUID` | `@Id @GeneratedValue(strategy = UUID)` | PK |
| `board` | `Board` | `@ManyToOne(fetch = LAZY, optional = false)` | Tabuleiro a que pertence |
| `row` | `int` | `@Column(nullable = false)` | Linha (0-9) |
| `col` | `int` | `@Column(nullable = false)` | Coluna (0-9) |
| `hasShip` | `boolean` | `@Column(nullable = false)` | Se há navio nesta célula |
| `hit` | `boolean` | `@Column(nullable = false)` | Se esta célula foi atacada |
| `ship` | `Ship` | `@ManyToOne(fetch = LAZY)` | Referência ao navio (null se vazio) |

**Índices:**
- Unique constraint composto em `(board_id, row, col)` — cada célula é única por posição
- Index em `board_id`

**FetchType LAZY em `ship`:** Só precisamos saber qual navio quando há hit, para verificar se afundou.

---

#### Entidade: `Shot`

Registro histórico de cada tiro disparado.

| Campo | Tipo Java | Constraints JPA | Notas |
|-------|-----------|-----------------|-------|
| `id` | `UUID` | `@Id @GeneratedValue(strategy = UUID)` | PK |
| `game` | `Game` | `@ManyToOne(fetch = LAZY, optional = false)` | Partida |
| `attacker` | `User` | `@ManyToOne(fetch = LAZY, optional = false)` | Quem disparou |
| `targetBoard` | `Board` | `@ManyToOne(fetch = LAZY, optional = false)` | Tabuleiro atingido (do oponente) |
| `row` | `int` | `@Column(nullable = false)` | Linha do tiro (0-9) |
| `col` | `int` | `@Column(nullable = false)` | Coluna do tiro (0-9) |
| `result` | `ShotResult` | `@Enumerated(STRING) @Column(nullable = false)` | `MISS`, `HIT`, `SUNK` |
| `sunkShipType` | `ShipType` | `@Enumerated(STRING)` | Preenchido apenas quando `result = SUNK` |
| `firedAt` | `Instant` | `@Column(nullable = false)` | Timestamp do tiro |

**Índices:**
- Index em `game_id` (para reconstruir histórico de partida)
- Unique constraint composto em `(target_board_id, row, col)` — não pode atirar duas vezes no mesmo lugar

---

#### Diagrama de Relacionamentos

```
User 1───────────* Game (como player1 ou player2)
User 1───────────* Board (como owner)
User 1───────────* Shot (como attacker)

Game 1───────────2 Board
Game 1───────────* Shot

Board 1──────────* Ship (max 5)
Board 1──────────* Cell (exatamente 100)

Ship 1───────────* Cell (via Cell.ship)
```

---

### 1.2 Regras de Domínio (Invariantes de Negócio)

#### Posicionamento de Navios

1. **Frota completa obrigatória** — Cada jogador deve posicionar exatamente 5 navios: 1×Carrier(5), 1×Battleship(4), 1×Cruiser(3), 1×Submarine(3), 1×Destroyer(2).
2. **Sem sobreposição** — Nenhum navio pode ocupar célula já ocupada por outro navio.
3. **Dentro dos limites** — Todas as células de um navio devem estar dentro do tabuleiro 10×10 (row e col entre 0 e 9).
4. **Orientação válida** — Apenas `HORIZONTAL` (expande em colunas) ou `VERTICAL` (expande em linhas).
5. **Posicionamento único** — Uma vez que o jogador confirma sua frota (`ready = true`), não pode reposicionar.
6. **Posicionamento atômico** — A frota inteira é enviada em uma única requisição. Ou toda é válida, ou toda é rejeitada.

#### Fluxo de Partida

7. **Estado WAITING** — Uma partida recém-criada espera por um segundo jogador. Apenas o criador está associado.
8. **Transição WAITING → PLACING** — Ocorre quando um segundo jogador entra na partida. Ambos os Boards são criados neste momento.
9. **Transição PLACING → IN_PROGRESS** — Ocorre quando ambos os boards estão com `ready = true`. O `currentTurn` é definido como o `player1` (quem criou a partida atira primeiro).
10. **Transição IN_PROGRESS → FINISHED** — Ocorre quando todos os navios de um jogador estão afundados.

#### Disparos

11. **Apenas na sua vez** — Um jogador só pode atirar quando `game.currentTurn` é ele. Tiro fora de turno lança exceção.
12. **Coordenada válida** — Row e col devem estar entre 0 e 9.
13. **Sem repetição** — Não é possível atirar em coordenada já atacada no tabuleiro do oponente (`cell.hit = true`).
14. **Alternância de turno** — Após cada tiro (independente do resultado), o turno passa para o outro jogador.
15. **Partida em andamento** — Tiros só são aceitos se `game.status = IN_PROGRESS`.

#### Resultado do Tiro

16. **MISS** — A célula não contém navio (`hasShip = false`). A célula é marcada como `hit = true`.
17. **HIT** — A célula contém navio. A célula é marcada como `hit = true`, e `ship.hits` é incrementado.
18. **SUNK** — Hit que resulta em `ship.hits == ship.shipType.size`. O resultado é `SUNK` e o `sunkShipType` é revelado ao atacante.

#### Vitória

19. **Condição de vitória** — Todos os 5 navios do oponente estão afundados (`isSunk() = true` para todos).
20. **Fim de jogo** — O `game.status` muda para `FINISHED`, `game.winner` é definido.

#### Casos de Borda

21. **Jogador não pode jogar contra si mesmo** — O `player2` não pode ser igual ao `player1`.
22. **Partida WAITING pode ser cancelada** — Se o criador desistir antes de alguém entrar.
23. **Desconexão durante partida** — O tabuleiro persiste; o jogador pode reconectar. Partida não é cancelada automaticamente.
24. **Jogador só participa de uma partida ativa por vez** — Evita que o mesmo usuário entre em múltiplas partidas `IN_PROGRESS` ou `PLACING`.
25. **Tiro após vitória** — Rejeitado, pois `status ≠ IN_PROGRESS`.
26. **Posicionamento após ready** — Rejeitado, board já está confirmado.
27. **Posicionamento com tipo duplicado** — Rejeitado (ex: 2 Carriers é inválido).

---

### 1.3 Enums

#### `ShipType`

| Valor | Nome Display | Tamanho | Quantidade na frota |
|-------|-------------|---------|---------------------|
| `CARRIER` | Porta-aviões | 5 | 1 |
| `BATTLESHIP` | Encouraçado | 4 | 1 |
| `CRUISER` | Cruzador | 3 | 1 |
| `SUBMARINE` | Submarino | 3 | 1 |
| `DESTROYER` | Contratorpedeiro | 2 | 1 |

Cada valor do enum terá os campos `size` (int) e `displayName` (String).

#### `GameStatus`

| Valor | Descrição |
|-------|-----------|
| `WAITING` | Aguardando segundo jogador |
| `PLACING` | Ambos posicionando navios |
| `IN_PROGRESS` | Partida em andamento (tiros) |
| `FINISHED` | Partida encerrada |

#### `Orientation`

| Valor | Descrição |
|-------|-----------|
| `HORIZONTAL` | Navio expande para a direita (colunas crescentes) |
| `VERTICAL` | Navio expande para baixo (linhas crescentes) |

#### `ShotResult`

| Valor | Descrição |
|-------|-----------|
| `MISS` | Tiro na água |
| `HIT` | Acertou parte de um navio |
| `SUNK` | Acertou e afundou o navio |

---

## FASE 2 — Arquitetura e Estrutura de Pacotes

---

### 2.1 Decisões Arquiteturais

#### 1. Por que WebSocket com STOMP?

**STOMP (Simple Text Oriented Messaging Protocol)** sobre WebSocket oferece um modelo publish/subscribe nativo que resolve dois problemas centrais do jogo:

- **Comunicação bidirecional em tempo real** — Tiros, notificações de turno e fim de jogo precisam chegar instantaneamente ao outro jogador sem polling.
- **User Destinations (Fog of War)** — STOMP no Spring suporta `user destinations` (`/user/queue/...`), onde cada mensagem é roteada exclusivamente para a sessão WebSocket do usuário autenticado. Isso garante que o resultado detalhado de um tiro só chegue ao atacante, e a notificação genérica só chegue ao defensor.
- **SockJS como fallback** — Para navegadores/proxies que bloqueiam WebSocket puro, o SockJS faz fallback transparente para long-polling HTTP sem alterar o código do servidor.

#### 2. Canais STOMP

**Cliente → Servidor (`/app/...`):**

| Destino | Payload | Descrição |
|---------|---------|-----------|
| `/app/game/{gameId}/fire` | `{ "row": 3, "col": 7 }` | Jogador dispara um tiro |

**Servidor → Cliente (privados, via user destination):**

| Destino | Destinatário | Payload | Quando |
|---------|-------------|---------|--------|
| `/user/queue/game/shot-result` | Atacante | `{ "row", "col", "result", "sunkShipType?" }` | Após processar tiro |
| `/user/queue/game/opponent-shot` | Defensor | `{ "row", "col", "result", "sunkShipType?" }` | Após tiro do oponente |
| `/user/queue/game/started` | Ambos (individualmente) | `{ "gameId", "opponentName", "yourTurn" }` | Quando ambos confirmam frota |
| `/user/queue/errors` | Remetente | `{ "code", "message" }` | Erro de domínio |

**Servidor → Cliente (broadcast por partida):**

| Destino | Destinatário | Payload | Quando |
|---------|-------------|---------|--------|
| `/topic/game/{gameId}/state` | Ambos | `{ "status", "currentTurnPlayerId", "winner?" }` | Após cada tiro |
| `/topic/game/{gameId}/player-joined` | Ambos | `{ "opponentName" }` | Quando player2 entra |

**Regra de segurança:** Os canais `/user/queue/...` são privados. Os canais `/topic/game/{gameId}/...` contêm apenas informação pública (turno atual, status). Nunca incluem posições de navios.

#### 3. Separação REST vs WebSocket

| Operação | Protocolo | Justificativa |
|----------|-----------|---------------|
| Cadastro / Login | REST | Operação request-response simples. Retorna JWT. |
| Criar/Entrar em partida | REST | Ação pontual. Após, cliente abre WebSocket. |
| Posicionar frota | REST | Payload complexo, validação pesada. Melhor com HTTP response clara. |
| Consultar estado da partida | REST | GET idempotente para reconexão ou refresh. |
| Disparar tiro | WebSocket | Exige resposta em tempo real e notificação push ao oponente. |
| Notificações de turno | WebSocket | Push server → client. |
| Fim de jogo | WebSocket | Notificação instantânea a ambos. |

#### 4. Autorização no WebSocket

**Handshake (HTTP Upgrade):**
1. Cliente conecta via SockJS para `/ws?token={jwt}`.
2. `HandshakeInterceptor` extrai o token, valida com `JwtService`, armazena nos atributos da sessão.
3. Se token inválido → handshake rejeitado (HTTP 401).

**Mensagens STOMP (após conexão):**
1. `ChannelInterceptor` no inbound channel intercepta frame `CONNECT`.
2. Associa o `Principal` (extraído dos atributos do handshake) à sessão STOMP via `accessor.setUser()`.
3. Nos handlers `@MessageMapping`, o `Principal` está disponível como parâmetro.
4. O handler valida que o usuário autenticado é participante da partida.

---

### 2.2 Estrutura de Pacotes

```
com.softexpert.batalhanaval_api
├── config/                  # Configurações Spring
│   ├── SecurityConfig       # SecurityFilterChain, rotas públicas/privadas
│   ├── WebSocketConfig      # STOMP broker, SockJS endpoint, interceptors
│   └── CorsConfig           # CORS por perfil (dev/prod)
│
├── domain/                  # Entidades JPA e Enums
│   ├── User
│   ├── Game
│   ├── Board
│   ├── Ship
│   ├── Cell
│   ├── Shot
│   ├── GameStatus (enum)
│   ├── ShipType (enum)
│   ├── Orientation (enum)
│   └── ShotResult (enum)
│
├── repository/              # Interfaces Spring Data JPA
│   ├── UserRepository
│   ├── GameRepository
│   ├── BoardRepository
│   ├── ShipRepository
│   ├── CellRepository
│   └── ShotRepository
│
├── service/                 # Lógica de negócio
│   ├── AuthService          # Registro, login, geração de JWT
│   ├── GameService          # Criação, entrada, estado da partida
│   ├── PlacementService     # Validação e posicionamento da frota
│   ├── ShotService          # Processamento de tiros, cálculo de resultado
│   └── NotificationService  # Envia mensagens STOMP via SimpMessagingTemplate
│
├── controller/              # REST Controllers
│   ├── AuthController       # POST /api/auth/register, /api/auth/login
│   └── GameController       # POST/GET /api/games/...
│
├── websocket/               # Handlers STOMP
│   └── GameWebSocketHandler # /app/game/{gameId}/fire
│
├── dto/                     # Request/Response DTOs
│   ├── request/
│   │   ├── RegisterRequest
│   │   ├── LoginRequest
│   │   ├── PlaceShipsRequest
│   │   └── FireRequest
│   └── response/
│       ├── AuthResponse
│       ├── GameResponse
│       ├── BoardResponse
│       ├── ShotResultResponse
│       ├── OpponentShotNotification
│       ├── GameStateNotification
│       └── ErrorResponse
│
├── security/                # Infraestrutura de segurança
│   ├── JwtService           # Geração e validação de tokens
│   ├── JwtAuthenticationFilter  # OncePerRequestFilter para REST
│   ├── WebSocketAuthInterceptor # ChannelInterceptor para STOMP
│   └── CustomUserDetailsService
│
└── exception/               # Tratamento global de erros
    ├── GlobalExceptionHandler
    ├── GameNotFoundException
    ├── NotYourTurnException
    ├── CellAlreadyAttackedException
    ├── InvalidShipPlacementException
    ├── GameNotInProgressException
    └── PlayerAlreadyInGameException
```

---

### 2.3 Fluxo Completo de uma Rodada (Tiro)

1. **Jogador A** envia STOMP para `/app/game/{gameId}/fire` com `{ "row": 3, "col": 7 }`.
2. O **`GameWebSocketHandler`** recebe a mensagem, extrai o `Principal` e delega ao `ShotService`.
3. O **`ShotService.processShot(gameId, attackerId, row, col)`** executa:
   - Busca o `Game` e valida `status = IN_PROGRESS`
   - Valida que `currentTurn.id == attackerId`
   - Busca o `Board` do oponente
   - Busca a `Cell` na posição `(row, col)`
   - Valida que `cell.hit == false`
   - Marca `cell.hit = true`
   - Se `cell.hasShip`: incrementa `ship.hits`, calcula se `SUNK` ou `HIT`
   - Se não tem navio: resultado = `MISS`
   - Persiste o `Shot`
   - Alterna o turno: `game.currentTurn = oponente`
   - Verifica condição de vitória
   - Se vitória: `game.status = FINISHED`, `game.winner = atacante`
4. O **`NotificationService`** envia:
   - Ao Jogador A (`/user/queue/game/shot-result`): resultado completo
   - Ao Jogador B (`/user/queue/game/opponent-shot`): notificação do ataque
   - A ambos (`/topic/game/{gameId}/state`): estado público atualizado

```
┌──────────┐         ┌──────────────────────┐         ┌─────────────┐
│ Jogador A│         │       SERVIDOR       │         │  Jogador B  │
│ (ataca)  │         │                      │         │  (defende)  │
└────┬─────┘         └──────────┬───────────┘         └──────┬──────┘
     │                          │                             │
     │  STOMP SEND              │                             │
     │  /app/game/{id}/fire     │                             │
     │  {"row":3,"col":7}       │                             │
     │─────────────────────────►│                             │
     │                          │                             │
     │                ┌─────────┴──────────┐                  │
     │                │   ShotService      │                  │
     │                │ 1. Valida turno    │                  │
     │                │ 2. Busca Cell      │                  │
     │                │ 3. Calcula result  │                  │
     │                │ 4. Persiste Shot   │                  │
     │                │ 5. Alterna turno   │                  │
     │                │ 6. Checa vitória   │                  │
     │                └─────────┬──────────┘                  │
     │                          │                             │
     │  /user/queue/game/       │   /user/queue/game/         │
     │    shot-result           │     opponent-shot           │
     │◄─────────────────────────│────────────────────────────►│
     │                          │                             │
     │  /topic/game/{id}/state  │   /topic/game/{id}/state    │
     │◄─────────────────────────│────────────────────────────►│
     │                          │                             │
```

---

## FASE 3 — Endpoints e Contratos de API

---

### 3.1 REST Endpoints

#### `POST /api/auth/register`

**Acesso:** `ANONYMOUS`

**Request Body:**
```json
{
  "username": "jogador1",
  "email": "jogador1@email.com",
  "password": "senhaSegura123"
}
```

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "jogador1",
  "email": "jogador1@email.com",
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

| Código | Descrição |
|--------|-----------|
| 201 | Usuário criado com sucesso |
| 400 | Validação falhou |
| 409 | Username ou email já em uso |

---

#### `POST /api/auth/login`

**Acesso:** `ANONYMOUS`

**Request Body:**
```json
{
  "username": "jogador1",
  "password": "senhaSegura123"
}
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "jogador1",
  "email": "jogador1@email.com",
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

| Código | Descrição |
|--------|-----------|
| 200 | Login bem-sucedido |
| 401 | Credenciais inválidas |

---

#### `POST /api/games`

**Descrição:** Criar nova partida ou entrar em partida `WAITING` existente.
**Acesso:** `USER`

**Request Body:** Nenhum.

**Response (201 Created — nova partida):**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "status": "WAITING",
  "player1": { "id": "...", "username": "jogador1" },
  "player2": null,
  "currentTurnPlayerId": null,
  "createdAt": "2026-06-25T11:50:00Z"
}
```

**Response (200 OK — entrou em partida existente):**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "status": "PLACING",
  "player1": { "id": "...", "username": "jogador1" },
  "player2": { "id": "...", "username": "jogador2" },
  "currentTurnPlayerId": null,
  "createdAt": "2026-06-25T11:50:00Z"
}
```

| Código | Descrição |
|--------|-----------|
| 200 | Entrou em partida existente |
| 201 | Nova partida criada |
| 409 | Jogador já está em uma partida ativa |

---

#### `GET /api/games/{id}`

**Descrição:** Estado público da partida. Inclui board do jogador autenticado, mas nunca o board do oponente.
**Acesso:** `USER` (participante da partida)

**Response (200 OK):**
```json
{
  "id": "...",
  "status": "IN_PROGRESS",
  "player1": { "id": "...", "username": "jogador1" },
  "player2": { "id": "...", "username": "jogador2" },
  "currentTurnPlayerId": "...",
  "winnerId": null,
  "myBoard": {
    "ready": true,
    "ships": [
      { "shipType": "CARRIER", "originRow": 0, "originCol": 0, "orientation": "HORIZONTAL", "hits": 2, "sunk": false }
    ],
    "cells": [
      { "row": 0, "col": 0, "hasShip": true, "hit": true }
    ]
  },
  "opponentBoard": {
    "shotsReceived": [
      { "row": 3, "col": 7, "result": "MISS" },
      { "row": 5, "col": 2, "result": "SUNK", "sunkShipType": "DESTROYER" }
    ]
  },
  "createdAt": "2026-06-25T11:50:00Z"
}
```

| Código | Descrição |
|--------|-----------|
| 200 | Sucesso |
| 403 | Jogador não é participante |
| 404 | Partida não encontrada |

---

#### `POST /api/games/{id}/ships`

**Descrição:** Posicionar frota completa. Envio atômico.
**Acesso:** `USER` (participante, partida em `PLACING`)

**Request Body:**
```json
{
  "ships": [
    { "shipType": "CARRIER", "originRow": 0, "originCol": 0, "orientation": "HORIZONTAL" },
    { "shipType": "BATTLESHIP", "originRow": 2, "originCol": 1, "orientation": "VERTICAL" },
    { "shipType": "CRUISER", "originRow": 4, "originCol": 5, "orientation": "HORIZONTAL" },
    { "shipType": "SUBMARINE", "originRow": 6, "originCol": 3, "orientation": "VERTICAL" },
    { "shipType": "DESTROYER", "originRow": 8, "originCol": 8, "orientation": "HORIZONTAL" }
  ]
}
```

**Response (200 OK):**
```json
{
  "message": "Fleet placed successfully",
  "boardReady": true,
  "gameStatus": "PLACING"
}
```

| Código | Descrição |
|--------|-----------|
| 200 | Frota posicionada |
| 400 | Posicionamento inválido |
| 403 | Jogador não é participante |
| 404 | Partida não encontrada |
| 409 | Board já confirmado / Partida não está em PLACING |

---

### 3.2 WebSocket Endpoints (STOMP)

#### Cliente → Servidor

| Destino STOMP | Payload JSON | Quando |
|---------------|-------------|--------|
| `/app/game/{gameId}/fire` | `{ "row": 3, "col": 7 }` | Jogador dispara tiro na sua vez |

#### Servidor → Cliente (privados)

| Destino STOMP | Destinatário | Payload JSON | Quando |
|---------------|-------------|-------------|--------|
| `/user/queue/game/shot-result` | Atacante | `{ "gameId", "row", "col", "result", "sunkShipType?" }` | Após processar tiro |
| `/user/queue/game/opponent-shot` | Defensor | `{ "gameId", "row", "col", "result", "sunkShipType?" }` | Após tiro do oponente |
| `/user/queue/game/started` | Ambos (individual) | `{ "gameId", "opponentName", "yourTurn" }` | Partida inicia |
| `/user/queue/errors` | Remetente | `{ "code", "message" }` | Erro de domínio |

#### Servidor → Cliente (broadcast)

| Destino STOMP | Payload JSON | Quando |
|---------------|-------------|--------|
| `/topic/game/{gameId}/state` | `{ "status", "currentTurnPlayerId", "winnerId?" }` | Após cada tiro |
| `/topic/game/{gameId}/player-joined` | `{ "playerId", "username" }` | Player2 entra |

---

### 3.3 DTOs

#### Request DTOs

**RegisterRequest**

| Campo | Tipo | Validações |
|-------|------|-----------|
| `username` | `String` | `@NotBlank`, `@Size(min=3, max=30)`, `@Pattern(regexp="^[a-zA-Z0-9_]+$")` |
| `email` | `String` | `@NotBlank`, `@Email`, `@Size(max=100)` |
| `password` | `String` | `@NotBlank`, `@Size(min=8, max=100)` |

**LoginRequest**

| Campo | Tipo | Validações |
|-------|------|-----------|
| `username` | `String` | `@NotBlank` |
| `password` | `String` | `@NotBlank` |

**PlaceShipsRequest**

| Campo | Tipo | Validações |
|-------|------|-----------|
| `ships` | `List<ShipPlacement>` | `@NotNull`, `@Size(min=5, max=5)` |

**ShipPlacement** (nested)

| Campo | Tipo | Validações |
|-------|------|-----------|
| `shipType` | `ShipType` | `@NotNull` |
| `originRow` | `int` | `@Min(0)`, `@Max(9)` |
| `originCol` | `int` | `@Min(0)`, `@Max(9)` |
| `orientation` | `Orientation` | `@NotNull` |

**FireRequest**

| Campo | Tipo | Validações |
|-------|------|-----------|
| `row` | `int` | `@Min(0)`, `@Max(9)` |
| `col` | `int` | `@Min(0)`, `@Max(9)` |

#### Response DTOs

**AuthResponse:** `id` (UUID), `username` (String), `email` (String), `token` (String)

**GameResponse:** `id`, `status`, `player1` (PlayerSummary), `player2`, `currentTurnPlayerId`, `winnerId`, `myBoard` (BoardResponse), `opponentBoard` (OpponentBoardResponse), `createdAt`

**PlayerSummary:** `id` (UUID), `username` (String)

**BoardResponse:** `ready` (boolean), `ships` (List\<ShipResponse\>), `cells` (List\<CellResponse\>)

**ShipResponse:** `shipType`, `originRow`, `originCol`, `orientation`, `hits`, `sunk`

**CellResponse:** `row`, `col`, `hasShip`, `hit`

**OpponentBoardResponse:** `shotsReceived` (List\<ShotSummary\>) — apenas tiros feitos pelo jogador

**ShotSummary:** `row`, `col`, `result`, `sunkShipType?`

**ShotResultResponse (WS):** `gameId`, `row`, `col`, `result`, `sunkShipType?`

**OpponentShotNotification (WS):** `gameId`, `row`, `col`, `result`, `sunkShipType?`

**GameStateNotification (WS):** `status`, `currentTurnPlayerId`, `winnerId?`

**PlaceShipsResponse:** `message`, `boardReady`, `gameStatus`

**ErrorResponse:** `code`, `message`, `timestamp`

**Campos OMITIDOS por segurança (nunca no response para o oponente):**
- `Board.ships` do oponente
- `Cell.hasShip` do oponente
- `Ship.originRow/originCol/orientation` do oponente

---

### 3.4 Tratamento de Erros

**Formato padrão:**
```json
{
  "code": "NOT_YOUR_TURN",
  "message": "It's not your turn to fire",
  "timestamp": "2026-06-25T11:55:00Z"
}
```

**Erros de domínio:**

| Código | HTTP | Descrição |
|--------|------|-----------|
| `GAME_NOT_FOUND` | 404 | Partida não existe |
| `NOT_GAME_PARTICIPANT` | 403 | Jogador não participa da partida |
| `NOT_YOUR_TURN` | 409 | Não é a vez deste jogador |
| `CELL_ALREADY_ATTACKED` | 409 | Coordenada já atacada |
| `INVALID_SHIP_PLACEMENT` | 400 | Posicionamento inválido (genérico) |
| `SHIPS_OVERLAP` | 400 | Navios se sobrepõem |
| `SHIP_OUT_OF_BOUNDS` | 400 | Navio ultrapassa limites |
| `INVALID_FLEET_COMPOSITION` | 400 | Frota incorreta |
| `BOARD_ALREADY_READY` | 409 | Frota já confirmada |
| `GAME_NOT_IN_PLACING` | 409 | Partida não está em fase de posicionamento |
| `GAME_NOT_IN_PROGRESS` | 409 | Partida não está em andamento |
| `PLAYER_ALREADY_IN_GAME` | 409 | Jogador já em partida ativa |
| `USERNAME_TAKEN` | 409 | Username em uso |
| `EMAIL_TAKEN` | 409 | Email em uso |
| `INVALID_CREDENTIALS` | 401 | Credenciais incorretas |
| `INVALID_COORDINATES` | 400 | Coordenadas fora de 0-9 |

Para WebSocket, erros são enviados via `/user/queue/errors`.

---

## FASE 4 — Segurança e Configuração

---

### 4.1 Spring Security — SecurityFilterChain

**Rotas públicas:**

| Padrão | Motivo |
|--------|--------|
| `POST /api/auth/**` | Cadastro e login |
| `/ws/**` | Handshake WebSocket (auth via token no interceptor) |
| `/swagger-ui/**`, `/v3/api-docs/**` | OpenAPI (apenas dev) |
| `/h2-console/**` | Console H2 (apenas dev) |

**Rotas autenticadas:** `/api/games/**` e qualquer outra `/api/**`.

**Cadeia de filtros:**

```
Request → CorsFilter → JwtAuthenticationFilter → AuthorizationFilter → Controller
```

- `JwtAuthenticationFilter` (OncePerRequestFilter): extrai token do header `Authorization: Bearer <token>`, valida, seta `SecurityContext`.
- CSRF desabilitado (API stateless com JWT).
- Session management: `STATELESS`.

---

### 4.2 JWT

**Claims:**

| Claim | Tipo | Descrição |
|-------|------|-----------|
| `sub` | `String` (UUID) | ID do usuário |
| `username` | `String` | Username para display |
| `iat` | `long` | Issued at (epoch seconds) |
| `exp` | `long` | Expiration (epoch seconds) |

**Expiração:** 24 horas.

**Algoritmo:** HMAC-SHA256 (HS256). Secret via env var `JWT_SECRET` (mínimo 256 bits).

**Validação no WebSocket handshake:**
1. Cliente conecta em `/ws?token=<jwt>`
2. `HandshakeInterceptor` extrai e valida token
3. Se válido: armazena userId nos atributos da sessão
4. Se inválido: rejeita handshake

**ChannelInterceptor (STOMP):**
1. No frame `CONNECT`: cria `Principal` a partir dos atributos do handshake, associa via `accessor.setUser()`
2. Nos `@MessageMapping`: `Principal` disponível como parâmetro

---

### 4.3 Fog of War — Garantias do Servidor

| # | Regra | Onde é aplicada |
|---|-------|-----------------|
| 1 | `opponentBoard` contém APENAS `shotsReceived` (tiros e resultados). Nunca `ships`, `cells`, `hasShip`. | GameController |
| 2 | `myBoard` inclui tudo (é do próprio jogador). | GameController |
| 3 | User destinations são individuais via `convertAndSendToUser()`. | NotificationService |
| 4 | Broadcast `/topic/game/{id}/state` contém apenas `status`, `currentTurnPlayerId`, `winnerId`. | NotificationService |
| 5 | Entidades nunca são serializadas diretamente. Sempre passam por DTOs. | Toda a aplicação |
| 6 | `ShotResult.SUNK` revela `ShipType` — única info de navio revelada. | ShotService |
| 7 | `GET /api/games/{id}` valida que Principal é participante. | GameController |

**Campos que NUNCA são serializados para o oponente:**
- `Board.ships` (coleção)
- `Cell.hasShip`
- `Cell.ship` (referência)
- `Ship.originRow`, `originCol`, `orientation`
- `Ship.hits` (do oponente)

---

### 4.4 Configuração de Ambientes

#### Perfil `dev` (H2)

```
spring.datasource.url=jdbc:h2:mem:batalhanaval
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

jwt.secret=dev-secret-key-min-256-bits-long-enough-for-hmac-sha256
jwt.expiration=86400000

cors.allowed-origins=http://localhost:3000,http://localhost:5173
```

#### Perfil `prod` (MySQL)

```
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

jwt.secret=${JWT_SECRET}
jwt.expiration=86400000

cors.allowed-origins=${CORS_ALLOWED_ORIGINS}
server.port=${PORT:8080}
```

**Variáveis de ambiente em produção:**

| Variável | Descrição |
|----------|-----------|
| `DB_URL` | JDBC URL do MySQL |
| `DB_USERNAME` | Usuário do banco |
| `DB_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Chave HMAC-SHA256 (min 32 chars) |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas |
| `PORT` | Porta do servidor (default 8080) |
| `SPRING_PROFILES_ACTIVE` | Perfil ativo (`prod`) |

---

## FASE 5 — Estratégia de Testes

---

### 5.1 Testes Unitários de Domínio

Testes de lógica pura, sem Spring context, sem banco. JUnit 5 + AssertJ + Mockito.

#### PlacementService — Posicionamento de Navios

| # | Caso | Dado | Quando | Então |
|---|------|------|--------|-------|
| 1 | Frota completa válida | Board vazio, 5 navios válidos | `placeShips()` | Board ready, Ships criados, Cells marcadas |
| 2 | Navio horizontal ultrapassa borda | CARRIER em col=7 HORIZONTAL | `placeShips()` | `SHIP_OUT_OF_BOUNDS` |
| 3 | Navio vertical ultrapassa borda | CARRIER em row=8 VERTICAL | `placeShips()` | `SHIP_OUT_OF_BOUNDS` |
| 4 | Coordenada negativa | originRow=-1 | `placeShips()` | `INVALID_COORDINATES` |
| 5 | Navios se sobrepõem | CARRIER em (0,0)H e BATTLESHIP em (0,2)H | `placeShips()` | `SHIPS_OVERLAP` |
| 6 | Tipo duplicado | 2× CARRIER | `placeShips()` | `INVALID_FLEET_COMPOSITION` |
| 7 | Frota incompleta (4 navios) | Lista com 4 | `placeShips()` | `INVALID_FLEET_COMPOSITION` |
| 8 | Frota com 6 navios | Lista com extra | `placeShips()` | `INVALID_FLEET_COMPOSITION` |
| 9 | Board já confirmado | ready = true | `placeShips()` | `BOARD_ALREADY_READY` |
| 10 | Navio na borda exata (válido) | DESTROYER em (9,8)H | `placeShips()` | Sucesso |
| 11 | Navio em (0,0) vertical (válido) | CARRIER em (0,0)V | `placeShips()` | Sucesso |

#### ShotService — Resultado de Tiro

| # | Caso | Dado | Quando | Então |
|---|------|------|--------|-------|
| 12 | MISS | Cell sem navio | `processShot()` | MISS, cell.hit=true, turno alterna |
| 13 | HIT | Cell com CRUISER (0 hits) | `processShot()` | HIT, ship.hits++, turno alterna |
| 14 | SUNK | DESTROYER com 1 hit, última célula | `processShot()` | SUNK, sunkShipType=DESTROYER |
| 15 | Vitória | Último navio, última célula | `processShot()` | SUNK, status=FINISHED, winner setado |
| 16 | Fora de turno | currentTurn ≠ attacker | `processShot()` | `NotYourTurnException` |
| 17 | Célula já atacada | cell.hit=true | `processShot()` | `CellAlreadyAttackedException` |
| 18 | Partida não IN_PROGRESS | status=FINISHED | `processShot()` | `GameNotInProgressException` |
| 19 | Coordenada inválida | row=10 | `processShot()` | `INVALID_COORDINATES` |
| 20 | Turno alterna após MISS | currentTurn=A | MISS | currentTurn=B |
| 21 | Turno alterna após HIT | currentTurn=A | HIT | currentTurn=B |
| 22 | Turno alterna após SUNK | currentTurn=A | SUNK (não último) | currentTurn=B |

#### GameService — Fluxo de Partida

| # | Caso | Dado | Quando | Então |
|---|------|------|--------|-------|
| 23 | Criar partida (sem WAITING) | Nenhuma WAITING | `createOrJoinGame()` | Nova partida WAITING |
| 24 | Entrar em partida WAITING | Partida WAITING de outro | `createOrJoinGame()` | player2 setado, status=PLACING |
| 25 | Jogador já em partida ativa | Já em IN_PROGRESS | `createOrJoinGame()` | `PlayerAlreadyInGameException` |
| 26 | Não entra na própria partida | WAITING com player1=self | `createOrJoinGame()` | Cria nova ou busca outra |
| 27 | PLACING → IN_PROGRESS | Ambos boards ready | Segundo `placeShips()` | status=IN_PROGRESS, currentTurn=player1 |
| 28 | Apenas um posicionou | Só player1 ready | Verificação | Status permanece PLACING |

#### ShipType — Cálculos

| # | Caso | Dado | Quando | Então |
|---|------|------|--------|-------|
| 29 | Células HORIZONTAL | CARRIER em (2,3)H | `calculateOccupiedCells()` | [(2,3),(2,4),(2,5),(2,6),(2,7)] |
| 30 | Células VERTICAL | DESTROYER em (5,1)V | `calculateOccupiedCells()` | [(5,1),(6,1)] |
| 31 | isSunk true | hits == size | `isSunk()` | true |
| 32 | isSunk false | hits < size | `isSunk()` | false |

---

### 5.2 Testes de Integração de Serviço

`@SpringBootTest` com H2. Testam Service → Repository → Banco.

| # | Caso | Descrição |
|---|------|-----------|
| 33 | Fluxo completo feliz | Criar → entrar → posicionar → tiros → vitória |
| 34 | Posicionamento persiste | Posicionar frota, consultar banco, verificar Ships/Cells |
| 35 | Shot persiste correto | Disparar, buscar Shot, verificar campos |
| 36 | Transições de estado | WAITING → PLACING → IN_PROGRESS → FINISHED |
| 37 | Unique constraint Cell | 2 Cells mesmo (board,row,col) → DataIntegrityViolation |
| 38 | Unique constraint Shot | 2 Shots mesmo (board,row,col) → violação |
| 39 | Query matchmaking | 3 partidas, buscar WAITING excluindo próprias |
| 40 | Cascade orphanRemoval | Deletar Board remove Ships e Cells |

---

### 5.3 Testes de Controller REST

`@WebMvcTest` + `MockMvc` + Mockito.

#### AuthController

| # | Caso | Descrição |
|---|------|-----------|
| 41 | Registro sucesso | POST /api/auth/register → 201 + AuthResponse |
| 42 | Username duplicado | → 409 + ErrorResponse |
| 43 | Campos inválidos | → 400 + detalhes validação |
| 44 | Login sucesso | POST /api/auth/login → 200 + AuthResponse |
| 45 | Credenciais inválidas | → 401 |

#### GameController

| # | Caso | Descrição |
|---|------|-----------|
| 46 | Criar partida autenticado | POST /api/games → 201 |
| 47 | Sem autenticação | POST /api/games → 401 |
| 48 | Consultar como participante | GET /api/games/{id} → 200 |
| 49 | Consultar como não-participante | → 403 |
| 50 | Partida inexistente | → 404 |
| 51 | Posicionar frota sucesso | POST /api/games/{id}/ships → 200 |
| 52 | Navios sobrepostos | → 400 |
| 53 | Partida não em PLACING | → 409 |
| 54 | Board já ready | → 409 |
| 55 | Validação request body | originRow=15 → 400 |

#### Segurança

| # | Caso | Descrição |
|---|------|-----------|
| 56 | Token expirado | → 401 |
| 57 | Assinatura inválida | → 401 |
| 58 | Token ausente em rota protegida | → 401 |
| 59 | Rotas públicas acessíveis | /api/auth/login sem token → não 401 |

---

### 5.4 Ferramentas e Organização

| Ferramenta | Uso |
|-----------|-----|
| JUnit 5 | Framework de testes |
| AssertJ | Assertions fluentes |
| Mockito | Mocks |
| H2 | Banco para integração |
| `@SpringBootTest` | Contexto completo |
| `@WebMvcTest` | Controllers |
| MockMvc | Simula HTTP |

**Estrutura de testes:**
```
src/test/java/com/softexpert/batalhanaval_api/
├── service/
│   ├── PlacementServiceTest.java
│   ├── ShotServiceTest.java
│   ├── GameServiceTest.java
│   └── integration/
│       ├── GameFlowIntegrationTest.java
│       └── PlacementIntegrationTest.java
├── controller/
│   ├── AuthControllerTest.java
│   └── GameControllerTest.java
└── domain/
    └── ShipTypeTest.java
```

**Execução:** `./mvnw test`