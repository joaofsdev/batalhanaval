# Backend — Batalha Naval

API REST e WebSocket que implementa toda a lógica do jogo de Batalha Naval multiplayer. Responsável por autenticação, matchmaking, validação de posicionamento, processamento de tiros, controle de turno, modo Tempestade, ranking, salas privadas e administração.

## Stack

| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| Java | 21 | Linguagem |
| Spring Boot | 4.0.7 | Framework |
| Spring Security | (gerenciada) | Autenticação e autorização JWT |
| Spring WebSocket | (gerenciada) | STOMP + SockJS para tempo real |
| Spring Data JPA | (gerenciada) | Persistência e repositórios |
| Spring Validation | (gerenciada) | Validação de entrada (Jakarta Bean Validation) |
| Flyway | (gerenciada) | Migrações de banco (produção) |
| jjwt | 0.12.6 | Geração e validação de tokens JWT |
| Bucket4j | 8.10.1 | Rate limiting |
| SpringDoc OpenAPI | 3.0.2 | Documentação automática (Swagger UI) |
| H2 | (gerenciada) | Banco de dados embarcado (dev) |
| PostgreSQL | (gerenciada) | Banco de dados (produção) |
| Lombok | (gerenciada) | Redução de boilerplate |

## Estrutura de Pacotes

```
com.softexpert.batalhanaval_api
├── config/            → Configurações (Security, WebSocket, CORS, RateLimitFilter)
├── controller/        → REST controllers
│   ├── AuthController
│   ├── GameController
│   ├── RoomController
│   ├── AbilityController
│   ├── StormController
│   ├── ProfileController
│   ├── RankingController
│   ├── AdminUserController
│   ├── AdminGameController
│   └── AdminAuditController
├── websocket/         → Handler STOMP (@MessageMapping)
│   └── GameWebSocketHandler
├── domain/            → Entidades JPA e enums
│   ├── User, UserRole, UserStatus
│   ├── Game, GameStatus, GameMode
│   ├── Board, Cell, Ship, ShipType, Orientation
│   ├── Shot, ShotResult
│   ├── PlayerAbility, AbilityType
│   ├── StormEvent, StormEventType
│   ├── AdminAuditLog
│   └── CancellationReason
├── dto/
│   ├── request/       → DTOs de entrada (CreateGameRequest, PlaceShipsRequest, FireRequest, etc.)
│   └── response/      → DTOs de saída (GameResponse, ShotResultResponse, etc.)
├── exception/         → Exceções de domínio + GlobalExceptionHandler
├── repository/        → Interfaces JPA (Spring Data)
├── security/          → JwtService, JwtAuthenticationFilter, WebSocketAuthInterceptor
└── service/           → Lógica de negócio
    ├── AuthService, GameService, ShotService
    ├── PlacementService, RoomService
    ├── AbilityService, StormService
    ├── NotificationService (emissão WebSocket)
    ├── VictoryService, EloService, RankingService
    ├── ProfileService, DisconnectionService
    ├── TurnTimeoutScheduler, PlacementTimeoutScheduler
    ├── SuspensionExpirationScheduler
    └── AdminUserService, AdminGameService, AdminAuditService
```

## Como Executar

### Desenvolvimento (perfil dev, H2)

```bash
./mvnw spring-boot:run
```

- Porta: **8080**
- Banco: H2 em arquivo local (`./batalhanavaldbdev`)
- Console H2: `http://localhost:8080/h2-console`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Flyway desativado (DDL gerenciado por Hibernate `create-drop`)

### Docker

```bash
docker build -t batalhanaval-api .
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://host:5432/batalhanaval \
  -e DB_USERNAME=usuario \
  -e DB_PASSWORD=senha \
  -e JWT_SECRET=chave-hmac-sha256-min-32-caracteres \
  -e CORS_ALLOWED_ORIGINS=https://seudominio.com \
  batalhanaval-api
```

Imagem baseada em `eclipse-temurin:21-jre` com multi-stage build (Maven 3.9 no estágio de compilação).

## Perfis de Configuração

| Perfil | Banco | DDL | Flyway | Porta | Uso |
|--------|-------|-----|--------|-------|-----|
| `dev` (padrão) | H2 (arquivo) | `create-drop` | Desativado | 8080 | Desenvolvimento local |
| `prod` | PostgreSQL | `update` | Desativado | `${PORT:8080}` | Produção |

## Variáveis de Ambiente (Produção)

| Variável | Obrigatória | Descrição |
|----------|-------------|-----------|
| `SPRING_PROFILES_ACTIVE` | Sim | Perfil Spring ativo (`prod`) |
| `DB_URL` | Sim | JDBC URL do PostgreSQL |
| `DB_USERNAME` | Sim | Usuário do banco |
| `DB_PASSWORD` | Sim | Senha do banco |
| `JWT_SECRET` | Sim | Chave HMAC-SHA256 (mínimo 32 caracteres) |
| `CORS_ALLOWED_ORIGINS` | Sim | Origens permitidas, separadas por vírgula |
| `PORT` | Não | Porta do servidor (default: 8080) |

## Endpoints REST

### Autenticação (públicos)

| Método | Path | Descrição |
|--------|------|-----------|
| POST | `/api/auth/register` | Registro de novo jogador |
| POST | `/api/auth/login` | Login (retorna JWT) |

### Partida (autenticados)

| Método | Path | Descrição |
|--------|------|-----------|
| POST | `/api/games` | Criar ou entrar em partida (matchmaking) |
| GET | `/api/games/{id}` | Obter estado da partida |
| GET | `/api/games/active` | Obter partida ativa do jogador |
| GET | `/api/games/fleet-config` | Configuração da frota (tipos de navio) |
| GET | `/api/games/history` | Histórico de partidas (paginado) |
| POST | `/api/games/{id}/ships` | Posicionar frota |
| POST | `/api/games/{id}/surrender` | Desistir da partida |
| POST | `/api/games/{id}/rematch` | Solicitar revanche |
| DELETE | `/api/games/{id}` | Cancelar partida |

### Salas Privadas (autenticados)

| Método | Path | Descrição |
|--------|------|-----------|
| POST | `/api/rooms` | Criar sala privada |
| POST | `/api/rooms/join` | Entrar em sala via token |
| POST | `/api/rooms/{id}/ready` | Confirmar prontidão |
| GET | `/api/rooms/{id}` | Estado da sala |
| DELETE | `/api/rooms/{id}` | Cancelar sala |

### Modo Tempestade (autenticados)

| Método | Path | Descrição |
|--------|------|-----------|
| GET | `/api/games/{id}/ability` | Consultar habilidade disponível |
| POST | `/api/games/{id}/ability` | Usar habilidade especial |
| GET | `/api/games/{id}/storm/next` | Info do próximo evento de tempestade |

### Perfil e Ranking (autenticados)

| Método | Path | Descrição |
|--------|------|-----------|
| GET | `/api/users/me/profile` | Perfil do jogador autenticado |
| GET | `/api/users/{id}/profile` | Perfil de outro jogador |
| GET | `/api/ranking` | Ranking geral (paginado, filtrável por período) |

### Administração (autenticados, role ADMIN)

| Método | Path | Descrição |
|--------|------|-----------|
| GET/PUT/DELETE | `/api/admin/users/**` | Gestão de usuários |
| GET/DELETE | `/api/admin/games/**` | Gestão de partidas |
| GET | `/api/admin/audit` | Logs de auditoria |

## WebSocket (STOMP)

Conexão via SockJS em `/ws` com autenticação por query parameter (`?token={jwt}`).

### Client → Server

| Destino | Descrição |
|---------|-----------|
| `/app/game/{gameId}/fire` | Disparar tiro (row, col) |

### Server → Client (mensagens privadas)

| Queue | Descrição |
|-------|-----------|
| `/user/queue/game/shot-result` | Resultado do tiro para o atacante |
| `/user/queue/game/opponent-shot` | Tiro recebido pelo defensor |
| `/user/queue/game/ability-result` | Resultado de habilidade especial |
| `/user/queue/game/ability-rotated` | Notificação de rotação de habilidade |
| `/user/queue/game/rematch-invite` | Convite de revanche |
| `/user/queue/errors` | Erros de domínio |

### Server → Client (broadcast por partida)

| Tópico | Descrição |
|--------|-----------|
| `/topic/game/{gameId}/state` | Estado atualizado da partida (turno, status, fog) |
| `/topic/game/{gameId}/storm` | Evento climático disparado |
| `/topic/game/{gameId}/player-joined` | Oponente entrou na partida |
| `/topic/game/{gameId}/rematch` | Revanche aceita (nova partida criada) |
| `/topic/game/{gameId}/opponent-disconnected` | Desconexão/reconexão do oponente |
| `/topic/room/{gameId}` | Atualizações de estado da sala privada |

## Segurança

- JWT stateless (HMAC-SHA256, expiração 24h)
- Rate limiting em `/api/auth/**` (5 req/min em produção, 10 req/min em dev)
- Fog of War: posições de navios do oponente nunca são expostas em nenhum endpoint ou mensagem WebSocket
- CSRF desabilitado (API stateless, sem cookies de sessão)
- CORS configurável por perfil via propriedade `cors.allowed-origins`
- Autenticação WebSocket via interceptor STOMP (valida JWT no CONNECT)
- Detecção de AFK: 3 skips consecutivos resultam em cancelamento automático

## Testes

```bash
./mvnw test
```

- Framework: JUnit 5
- Banco de teste: H2
- Dependências de teste inclusas: Spring Security Test, WebSocket Test, JPA Test, Flyway Test

## Documentação da API

Swagger UI disponível em ambiente de desenvolvimento:

```
http://localhost:8080/swagger-ui.html
```
