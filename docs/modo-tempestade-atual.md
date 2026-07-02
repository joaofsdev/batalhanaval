# Modo Tempestade — Documentação do Estado Atual

Documento gerado a partir do código-fonte em 2026-07-02.
Descreve exclusivamente o que está implementado — sem sugestões de melhoria.

---

## 1. Habilidades Implementadas

Cada jogador recebe **1 habilidade aleatória** ao iniciar uma partida STORM (após ambos posicionarem frota). A atribuição ocorre em `AbilityService.initializeAbilities()`, que sorteia independentemente para cada jogador entre os 4 tipos disponíveis (jogadores podem receber a mesma habilidade).

### 1.1 RADAR

| Campo | Valor |
|-------|-------|
| Nome de exibição | Radar |
| Descrição | Revela presença de navios em área 3x3 |
| Sorteio | Aleatório entre os 4 tipos (ThreadLocalRandom) |
| Parâmetros obrigatórios | `row` (centro), `col` (centro) |
| Efeito | Retorna grid booleano 3x3 indicando presença de navios nas células ao redor do ponto central no tabuleiro do oponente. Células fora do tabuleiro (0-9) retornam `false`. Não marca células como atacadas, apenas revela informação. |
| Validações | Jogo STORM, IN_PROGRESS, é o turno do jogador, não é storm turn, habilidade não usada, tipo corresponde à atribuída |
| Response | `AbilityResultResponse` com `abilityType=RADAR`, `radarGrid` (boolean[3][3]), `centerRow`, `centerCol` |

### 1.2 DOUBLE_TORPEDO

| Campo | Valor |
|-------|-------|
| Nome de exibição | Torpedo Duplo |
| Descrição | Dispara 2 tiros no mesmo turno |
| Sorteio | Aleatório entre os 4 tipos |
| Parâmetros obrigatórios | `row`, `col` (coordenada do primeiro tiro) |
| Efeito | Processa 2 tiros: o primeiro na coordenada fornecida, o segundo em `(row+1, col)` — se `row+1 > 9`, usa `(row-1, col)`. Ambos os tiros são registrados como Shot entities, marcam células como hit, incrementam hits de navios, e verificam afundamento. Células já atacadas retornam MISS sem efeito. |
| Validações | Mesmas do RADAR |
| Response | `AbilityResultResponse` com `abilityType=DOUBLE_TORPEDO`, `shotResults` (lista de 2 `ShotResultResponse`) |

### 1.3 SHIELD

| Campo | Valor |
|-------|-------|
| Nome de exibição | Escudo |
| Descrição | Anula o próximo tiro recebido |
| Sorteio | Aleatório entre os 4 tipos |
| Parâmetros obrigatórios | Nenhum (`row`, `col`, `axis`, `index` são ignorados) |
| Efeito | Ativa o escudo. No próximo tiro recebido (`ShotService.processShot`), se `isShieldActiveAndNotConsumed()` retorna true para o defensor, o tiro é convertido em MISS (a célula é marcada como hit mas nenhum dano é aplicado). O escudo é consumido setando `usedOnTurn = -1`. |
| Validações | Mesmas do RADAR |
| Detecção de escudo ativo | `PlayerAbility.abilityType == SHIELD && used == true && usedOnTurn >= 0` |
| Consumo de escudo | `usedOnTurn` é setado para `-1` (convenção para "consumido") |
| Response | `AbilityResultResponse` com `abilityType=SHIELD`, `message="Shield activated. Next incoming shot will be nullified."` |

### 1.4 LINE_BOMBARDMENT

| Campo | Valor |
|-------|-------|
| Nome de exibição | Bombardeio em Linha |
| Descrição | Ataca toda uma linha ou coluna |
| Sorteio | Aleatório entre os 4 tipos |
| Parâmetros obrigatórios | `axis` ("ROW" ou "COL"), `index` (0-9) |
| Efeito | Itera pelas 10 células da linha/coluna indicada no tabuleiro do oponente. Células já atacadas (`isHit`) são ignoradas. Para cada célula não atacada, processa um tiro completo (registra Shot, marca hit, verifica afundamento). |
| Validações | Mesmas do RADAR |
| Response | `AbilityResultResponse` com `abilityType=LINE_BOMBARDMENT`, `shotResults` (lista de N `ShotResultResponse`, onde N = células não-hit na linha/coluna) |

### Validações comuns a todas as habilidades

Verificadas em `AbilityService.useAbility()`:
1. `game.gameMode == STORM` → senão `NotStormModeException` (400)
2. `game.status == IN_PROGRESS` → senão `GameNotInProgressException` (409)
3. `game.currentTurn.id == userId` → senão `NotYourTurnException` (409)
4. Não é storm turn (via `StormService.isStormTurn`) → senão `AbilityBlockedByStormException` (409)
5. `ability.used == false` → senão `AbilityAlreadyUsedException` (409)
6. `ability.abilityType == requestedType` → senão `InvalidAbilityTypeException` (400)

---

## 2. Eventos Climáticos Implementados

Eventos são gerados pelo `StormService.generateNextStormEvent()` e resolvidos por `StormService.resolveStormEvent()`.

### Frequência de geração

- Primeiro evento: turno 3 (campo `Game.nextStormTurn` inicializado com valor 3).
- Eventos subsequentes: a cada 3 turnos após o último evento gerado (`nextStormTurn = currentTurnNumber + 3`).
- A geração ocorre em `ShotService.advanceTurn()` quando `currentTurnNumber == nextStormTurn`.

### Resolução

A resolução ocorre em `GameWebSocketHandler.fire()`: antes de processar o tiro, se o turno atual é um storm turn (tem evento não-resolvido), `resolveStormEvent()` é chamado. O evento é marcado como `resolved = true` e o efeito é aplicado.

### 2.1 FOG (Nevoeiro)

| Campo | Valor |
|-------|-------|
| Nome | Nevoeiro |
| Descrição | Resultados dos tiros ficam ocultos até o próximo turno |
| Efeito no backend | Seta `game.fogActive = true`. O fog é limpo no `advanceTurn()` seguinte (`game.setFogActive(false)`). |
| affectedAxis | null |
| Broadcast message | "Nevoeiro! Resultados dos tiros ficam ocultos até o próximo turno." |
| Frontend | `useStormWebSocket` seta `fogActive=true`, que é passado para `OpponentBoard` como prop. |

### 2.2 TIDE (Maré Alta)

| Campo | Valor |
|-------|-------|
| Nome | Maré Alta |
| Descrição | Uma linha do tabuleiro fica inacessível neste turno |
| Efeito no backend | Sorteia uma linha aleatória (0-9), grava `affectedAxis = "ROW_X"`. Em `ShotService.processShot()`, se `isShotBlockedByTide()` retorna true para a row do tiro, lança `StormBlocksShotException` (409). |
| affectedAxis | "ROW_X" (onde X é 0-9) |
| Broadcast message | "Maré Alta! Linha X está inacessível neste turno." |
| Frontend | `useStormWebSocket` seta `blockedRow`, passado para `OpponentBoard`. |

### 2.3 CURRENT (Corrente Marítima)

| Campo | Valor |
|-------|-------|
| Nome | Corrente Marítima |
| Descrição | Navios se deslocam 1 célula aleatoriamente |
| Efeito no backend | Para cada board da partida, seleciona 1 navio aleatório não-afundado e tenta movê-lo 1 célula em uma das 4 direções (up/down/left/right, embaralhadas). Validações do movimento: nova posição dentro do grid 0-9, sem sobreposição com outros navios. Se nenhuma direção é válida, o navio permanece. Células antigas são limpas (apenas não-hit), ship origin é atualizado, novas células são marcadas. |
| affectedAxis | null |
| Broadcast message | "Corrente Marítima! Navios se deslocaram 1 célula." |
| Frontend | Toast informativo via `setToast`. |

### 2.4 CALM (Calmaria)

| Campo | Valor |
|-------|-------|
| Nome | Calmaria |
| Descrição | Ambos os jogadores ganham um tiro bônus |
| Efeito no backend | Seta `game.bonusShot = true`. Em `ShotService.advanceTurn()`, se `bonusShot == true`, o turno NÃO é alternado — o jogador mantém a vez e `bonusShot` é setado para `false`. |
| affectedAxis | null |
| Broadcast message | "Calmaria! Ambos os jogadores ganham um tiro bônus neste turno." |
| Frontend | Toast informativo "Calmaria — turno bônus!". `GameStateNotification.bonusShot` é broadcasteado. |

### Payload de broadcast (StormEventNotification)

```json
{
  "eventType": "FOG|TIDE|CURRENT|CALM",
  "affectedAxis": "ROW_X" | null,
  "message": "Texto descritivo do evento"
}
```

---

## 3. Ordem de Resolução de Turno

Implementada em `GameWebSocketHandler.fire()` + `ShotService.processShot()` + `ShotService.advanceTurn()`.

### Fluxo passo a passo (mensagem WebSocket `/app/game/{gameId}/fire`):

1. **Recebe mensagem STOMP** com `{ row, col }` do jogador autenticado.
2. **Verifica storm turn**: Se `gameMode == STORM` e `isStormTurn(gameId, currentTurnNumber)` é true:
   - Chama `stormService.resolveStormEvent(gameId)`.
   - Se retornar notificação não-nula, faz broadcast via `notificationService.broadcastStormEvent()`.
3. **Processa tiro** (`shotService.processShot`):
   - Valida: jogo IN_PROGRESS, é o turno do jogador.
   - Se STORM mode: verifica se TIDE bloqueia a row → `StormBlocksShotException`.
   - Se STORM mode: verifica shield ativo do defensor → se sim, converte em MISS, consome shield, avança turno.
   - Caso normal: marca célula como hit, calcula resultado (MISS/HIT/SUNK).
   - Registra entidade Shot.
   - Verifica vitória (todos navios do defensor afundados) → se sim, FINISHED + define winner.
   - Chama `advanceTurn()`.
4. **advanceTurn()**:
   - Reseta consecutiveSkips para 0.
   - Se `bonusShot == true`: consome bonus (seta false), jogador mantém turno. **Retorna**.
   - Caso contrário: alterna currentTurn para o oponente.
   - Incrementa `currentTurnNumber`.
   - Limpa fog (`fogActive = false`) se estava ativo.
   - Se `currentTurnNumber == nextStormTurn`: gera próximo evento via `stormService.generateNextStormEvent()`.
5. **Notifica** (de volta no handler):
   - `notifyShotResult()` → atacante (queue privada).
   - `notifyOpponentShot()` → defensor (queue privada).
   - `broadcastGameState()` → ambos (tópico público da partida).
6. **Erro**: qualquer `DomainException` é enviada para `/user/queue/errors` do atacante.

---

## 4. Fluxo de Matchmaking por Modo

Implementado em `GameService.createOrJoinGame(UUID userId, GameMode gameMode)`.

### Processo:

1. Verifica se jogador já está em partida ativa (WAITING/PLACING/IN_PROGRESS) → se sim, `PlayerAlreadyInGameException`.
2. Busca primeira partida WAITING do **mesmo** `gameMode`, excluindo o próprio jogador, ordenada por `createdAt ASC`, com **lock pessimista** (`PESSIMISTIC_WRITE`):
   - Query: `findFirstWaitingGameByModeForUpdate(WAITING, userId, gameMode)`
3. **Se encontrou** partida WAITING:
   - Seta `player2 = user`.
   - Seta `status = PLACING`.
   - Cria board para player2.
   - Retorna game response.
4. **Se não encontrou**:
   - Cria nova Game com `player1 = user`, `status = WAITING`, `gameMode = gameMode`.
   - Cria board para player1.
   - Retorna game response.

### Separação de filas:

A separação é feita **exclusivamente** pelo filtro `AND g.gameMode = :gameMode` na query SQL. Jogadores CLASSIC só são pareados com CLASSIC, jogadores STORM só com STORM. Não há fila explícita em memória — a "fila" é a tabela `games` filtrada por status WAITING + gameMode.

### Frontend:

Em `LobbyPage.jsx`, o usuário seleciona CLASSIC ou STORM via cards. O modo selecionado é enviado como parâmetro em `gameApi.createOrJoinGame(selectedMode)` → `POST /api/games` com body `{ gameMode: "CLASSIC"|"STORM" }`.

---

## 5. Fluxo de Rematch

Implementado em `GameService.requestRematch(UUID originalGameId, UUID userId)`.

### Processo:

1. Valida que o jogador é participante da partida original e que ela está FINISHED.
2. Verifica se jogador já está em partida ativa → se sim, `PlayerAlreadyInGameException`.
3. Usa `ConcurrentHashMap<UUID, UUID> pendingRematches` (key = originalGameId, value = userId do primeiro solicitante).
4. **Operação atômica** via `pendingRematches.compute()`:
   - Se não há pedido pendente para esta partida → registra userId e retorna `WAITING`.
   - Se o mesmo jogador clicou de novo → mantém `WAITING`.
   - Se é o oponente:
     - Verifica se o primeiro solicitante ainda está disponível (não entrou em outra partida).
     - Se primeiro solicitante está busy → substitui pelo jogador atual, retorna `WAITING`.
     - Se ambos disponíveis → **MATCH**:
       - Cria nova Game com `player1 = firstRequester`, `player2 = currentUser`.
       - `status = PLACING`, `gameMode` = mesmo da partida original.
       - Cria boards para ambos.
       - Remove entrada do mapa.
       - Retorna `MATCHED` com `newGameId`.
5. **Cancelamento**: `cancelPendingRematch(originalGameId, userId)` remove do mapa usando `ConcurrentHashMap.remove(key, value)`.

### Notificação:

- Quando um jogador solicita rematch, `NotificationService.notifyRematchInvite()` envia para o oponente via `/user/queue/game/rematch-invite`.
- Quando o match acontece, `NotificationService.notifyRematchMatched()` envia para o tópico `/topic/game/{originalGameId}/rematch` com payload `{ status: "MATCHED", gameId: newGameId }`.

### Response:

```java
public record RematchResponse(RematchStatus status, UUID gameId) {
    enum RematchStatus { WAITING, MATCHED }
}
```

---

## 6. Endpoints REST e Tópicos WebSocket

### 6.1 Endpoints REST

| Método | Path | Descrição | Request Body | Response |
|--------|------|-----------|--------------|----------|
| POST | `/api/games` | Criar/entrar em partida | `{ "gameMode": "CLASSIC"\|"STORM" }` | `GameResponse` |
| GET | `/api/games/{gameId}/ability` | Consultar habilidade do jogador | — | `AbilityStatusResponse { abilityType, used, usedOnTurn }` |
| POST | `/api/games/{gameId}/ability` | Usar habilidade | `UseAbilityRequest { abilityType, row?, col?, axis?, index? }` | `AbilityResultResponse { abilityType, radarGrid?, centerRow?, centerCol?, shotResults?, message? }` |
| GET | `/api/games/{gameId}/storm/next` | Consultar próximo storm turn | — | `StormInfoResponse { nextStormTurn, currentTurn, turnsUntilStorm }` |

### 6.2 Tópicos WebSocket (STOMP)

#### Destinos de envio (client → server)

| Destino | Payload | Descrição |
|---------|---------|-----------|
| `/app/game/{gameId}/fire` | `{ row, col }` | Disparar tiro (resolve storm se aplicável, processa shot) |

#### Tópicos broadcast (server → todos inscritos na partida)

| Tópico | Payload | Descrição |
|--------|---------|-----------|
| `/topic/game/{gameId}/state` | `GameStateNotification { status, currentTurnPlayerId, winnerId, turnNumber, isStormTurn, turnsUntilStorm, bonusShot, fogActive }` | Estado atualizado da partida |
| `/topic/game/{gameId}/storm` | `StormEventNotification { eventType, affectedAxis, message }` | Evento climático resolvido |
| `/topic/game/{gameId}/rematch` | `{ status: "MATCHED", gameId: "uuid" }` | Rematch aceito — nova partida criada |
| `/topic/game/{gameId}/opponent-disconnected` | `{ type: "DISCONNECTED"\|"RECONNECTED", gracePeriodSeconds? }` | Desconexão/reconexão do oponente |

#### Queues privadas (server → jogador específico)

| Queue | Payload | Descrição |
|-------|---------|-----------|
| `/user/queue/game/shot-result` | `ShotResultResponse { gameId, row, col, result, sunkShipType }` | Resultado do próprio tiro |
| `/user/queue/game/opponent-shot` | `OpponentShotNotification { gameId, row, col, result, sunkShipType }` | Tiro recebido do oponente |
| `/user/queue/game/ability-result` | `AbilityResultResponse` | Resultado do uso de habilidade |
| `/user/queue/game/rematch-invite` | `RematchInvite` | Convite de rematch recebido |
| `/user/queue/errors` | `ErrorResponse { code, message }` | Erro de domínio |

---

## 7. Limitações e Simplificações Conhecidas

### 7.1 Habilidade — 1 por jogador, uso único

Cada jogador recebe exatamente 1 habilidade e só pode usá-la 1 vez na partida inteira. Não há sistema de cooldown, recarga, ou múltiplas habilidades.

### 7.2 Sorteio sem balanceamento

Ambos jogadores podem receber a mesma habilidade. O sorteio é totalmente aleatório (`ThreadLocalRandom.nextInt(types.length)`), sem lógica de evitar duplicatas ou balancear vantagens.

### 7.3 DOUBLE_TORPEDO — segundo tiro fixo

O segundo tiro é sempre em `(row+1, col)` ou `(row-1, col)` se estiver no limite. Não há escolha do jogador para a posição do segundo tiro.

### 7.4 SHIELD — convenção de consumo via valor negativo

O consumo do escudo é rastreado pela convenção `usedOnTurn = -1`. Não há campo booleano dedicado (`shieldConsumed`). Comentário no código: *"We track 'shield consumed' by setting usedOnTurn to a negative value (convention)."*

### 7.5 CALM — bonusShot é flag global

O campo `Game.bonusShot` é um único boolean. Quando CALM é resolvido, seta `true`. O próximo jogador a disparar consome o bonus. A descrição diz "Ambos ganham um tiro bônus", mas na prática apenas o jogador do turno seguinte se beneficia (um único bonus é consumido na primeira vez que `advanceTurn` é chamado).

### 7.6 FOG — lógica de ocultação apenas no frontend

O backend seta `fogActive = true` e envia no `GameStateNotification`, mas não altera o payload de `ShotResultResponse`. A ocultação visual dos resultados é responsabilidade exclusiva do frontend (prop `fogActive` no `OpponentBoard`).

### 7.7 CURRENT — movimento sem notificação individual

Quando navios se movem por CURRENT, o backend move fisicamente ship e cells, mas não informa ao jogador quais navios específicos foram movidos ou para onde. O frontend recebe apenas um toast genérico "Navios se moveram — posições alteradas".

### 7.8 CURRENT — limpa apenas células não-hit

Ao mover um navio, `applyShipMove` só limpa (`hasShip=false, ship=null`) células antigas que NÃO foram hit. Células hit do navio movido mantêm `hasShip=true` na posição antiga (inconsistência potencial).

### 7.9 Storm turn resolve no primeiro tiro

O storm event é resolvido dentro do handler `fire()`, imediatamente antes do processamento do tiro. Isso significa que o efeito é aplicado no mesmo momento em que o jogador dispara, e não no início do turno de forma isolada.

### 7.10 Habilidades bloqueadas em storm turns

Se o turno atual é um storm turn (tem evento não-resolvido), o uso de habilidades é bloqueado (`AbilityBlockedByStormException`). Isso ocorre mesmo que o evento seja CALM (que beneficia o jogador).

### 7.11 Frontend — parsing de alvo no AbilityPanel

O frontend envia `target` como string (ex: "E5" para RADAR, "A" ou "3" para LINE_BOMBARDMENT) via `gameApi.useAbility(id, payload)`. O controller recebe `UseAbilityRequest` com campos `row`, `col`, `axis`, `index` separados. Há uma desconexão: o frontend envia `{ type, target }` mas o backend espera campos separados. A conversão de `target` para os campos corretos não está visível no código do frontend (possível incompatibilidade ou tratamento em camada não identificada).

### 7.12 Sem verificação de vitória nos tiros de habilidade

`AbilityService.processSingleShot()` (usado por DOUBLE_TORPEDO e LINE_BOMBARDMENT) não verifica vitória após afundar navios. A verificação de vitória só acontece em `ShotService.processShot()`. Se todos os navios forem afundados por uma habilidade, o jogo pode continuar até o próximo tiro normal.

### 7.13 Rematch preserva o gameMode

Ao aceitar rematch, a nova partida é criada com o mesmo `gameMode` da partida original. Não há opção de mudar de modo no rematch.
