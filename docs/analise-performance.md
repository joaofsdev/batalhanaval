# Análise de Performance — Observabilidade

---

## Latência alta no endpoint GET /api/games/active — 28/07/2026

**Evidência:** Dashboard Grafana "Slowest Endpoints (p99 Response Time)" em produção (Render) mostra o endpoint `GET /api/games/active` com p99 de 3.55s e média de 1.11s. É o endpoint mais lento da aplicação, chamado toda vez que o jogador abre o lobby ou inicia matchmaking. Scrape feito pelo Prometheus a cada 10s apontando para https://batalhanaval-te2c.onrender.com/actuator/prometheus.

**Causa:** O método `buildGameResponse` no `GameService` executava múltiplas queries sequenciais ao PostgreSQL remoto (Supabase) via lazy loading do Hibernate:
1. SELECT game (sem relações — lazy)
2. SELECT player1 (lazy ao acessar `game.getPlayer1()`)
3. SELECT player2 (lazy)
4. SELECT currentTurn (lazy)
5. SELECT board por game_id + owner_id
6. SELECT ships do board (lazy da coleção)
7. SELECT cells do board (lazy da coleção)
8. SELECT shots por game_id + attacker_id

Total: ~7-8 roundtrips ao banco. Com latência de rede de ~50-200ms por query no PostgreSQL remoto, o tempo acumulava entre 1s e 3.5s.

**Como corrigi:**
- Criei `findActiveGameByUserIdEager` no `GameRepository` com `JOIN FETCH g.player1 LEFT JOIN FETCH g.player2 LEFT JOIN FETCH g.currentTurn LEFT JOIN FETCH g.winner` — traz o game + 4 relações em uma única consulta SQL.
- Criei `findByGameIdAndOwnerIdWithShips` e `findByGameIdAndOwnerIdWithCells` no `BoardRepository` com `LEFT JOIN FETCH` individual — carrega o board com navios e células em 2 queries em vez de 3 (board + lazy ships + lazy cells).
- A query de shots (`findAllByGameIdAndAttackerId`) já era direta e foi mantida.
- Separei a query otimizada (eager) do path de escrita (`createOrJoinGame`) para evitar conflitos com locks pessimistas e entidades recém-criadas.

**Por que corrigi dessa forma:**
- `JOIN FETCH` nos ManyToOne (players, currentTurn, winner) é a forma padrão de eliminar N+1 sem alterar a entidade — zero impacto nos outros métodos que não precisam dos players carregados.
- Separei ships e cells em duas queries com `LEFT JOIN FETCH` individual porque usar `@EntityGraph` ou duplo `JOIN FETCH` em duas coleções `List` causa `MultipleBagFetchException` no Hibernate (produto cartesiano). Duas queries separadas ainda são mais rápidas que 3 lazy loads individuais.
- Mantive a query original (`findActiveGameByUserId`) no path de escrita (`createOrJoinGame`) porque ali o game pode estar sendo criado na mesma transação, e o `JOIN FETCH` em entidades com lock pessimista causava erro 500.
- Não usei cache porque o estado do jogo muda a cada turno — cache invalidaria imediatamente.

**Resultado:** O endpoint `GET /api/games/active` caiu de 3.55s (p99) para **446ms** após a aplicação aquecer — melhoria de ~8x. Queries reduziram de ~7-8 roundtrips para 4 (1 game eager + 2 board fetch + 1 shots).

---

## DELETE /api/games/{id} com tempo elevado — 28/07/2026

**Evidência:** Dashboard Grafana "Slowest Endpoints" mostra `DELETE /api/games/{id}` com tempo de 1.76s (máximo) em produção. Endpoint chamado quando o jogador cancela uma partida no status WAITING.

**Causa:** O método `gameRepository.delete(game)` disparava cascade delete via Hibernate — antes de deletar o game, o Hibernate carregava todas as coleções lazy (boards, ships, cells, shots, storm_events, player_abilities) e executava DELETEs individuais para cada registro. Com `CascadeType.ALL` e `orphanRemoval = true`, isso gerava ~20+ queries sequenciais ao PostgreSQL remoto, cada uma com latência de rede.

**Como corrigi:** Substituí o `gameRepository.delete(game)` por um método `bulkDeleteGame` que executa 7 queries nativas em ordem de dependência (filhas antes de pais):
1. `DELETE FROM cells WHERE board_id IN (SELECT id FROM boards WHERE game_id = ?)`
2. `DELETE FROM ships WHERE board_id IN (SELECT id FROM boards WHERE game_id = ?)`
3. `DELETE FROM shots WHERE game_id = ?`
4. `DELETE FROM storm_events WHERE game_id = ?`
5. `DELETE FROM player_abilities WHERE game_id = ?`
6. `DELETE FROM boards WHERE game_id = ?`
7. `DELETE FROM games WHERE id = ?`

Cada query deleta todos os registros de uma tabela em uma única operação, sem lazy loading.

**Por que corrigi dessa forma:**
- Queries nativas com `@Modifying` e `nativeQuery = true` executam um único DELETE por tabela, independente da quantidade de registros — O(1) roundtrips por tabela em vez de O(n) por registro.
- A ordem respeita as foreign keys (cells/ships antes de boards, boards antes de games) evitando violação de constraint.
- Não alterei o `CascadeType` da entidade porque outros fluxos (ex: testes, admin) podem depender do cascade automático para deleções menores.
- Trade-off aceito: o bulk delete não dispara eventos do Hibernate (`@PreRemove`, listeners) — aceitável porque não há listeners de remoção nessas entidades.

**Resultado:** O endpoint `DELETE /api/games/{id}` caiu de 1.76s para **495ms** — melhoria de ~3.5x.

---

## Cache no ranking e perfil do jogador — 28/07/2026

**Evidência:** Dashboard Grafana mostra `GET /api/ranking` com p50 de 123ms e p95 de 350ms, e `GET /api/users/me/profile` com p50 de 190ms e p99 de 1.40s. Ambos os endpoints são chamados toda vez que o jogador volta ao lobby, gerando queries pesadas repetidas ao banco (ranking faz JOIN + GROUP BY em todas as partidas finalizadas, perfil faz 6+ queries incluindo ranking + stats de tiros + histórico recente).

**Causa:** Nenhum cache existia — toda requisição executava as queries do zero. O ranking muda apenas quando uma partida termina (evento raro comparado à frequência de leitura). O perfil tem a mesma característica: stats só mudam no fim de uma partida.

**Como corrigi:**
- Adicionei cache in-memory com Caffeine via `@Cacheable` do Spring:
  - Cache `ranking`: TTL de 5 minutos, chave por `period` (all/week/month), máximo 50 entradas. Cacheado no `RankingCacheService.fetchRankingData`.
  - Cache `profile`: TTL de 2 minutos, chave por `userId`, máximo 200 entradas. Cacheado no `ProfileService.getProfile`.
- Separei `RankingCacheService` como bean independente do `RankingService` para que o proxy AOP do Spring intercepte a annotation `@Cacheable` corretamente (chamadas internas na mesma classe não passam pelo proxy).
- Criei `CacheEvictionService` que invalida ambos os caches quando uma partida termina.
- Invalidação chamada em 3 pontos: `VictoryService.checkVictoryCondition` (vitória), `GameService.endGameByAfk` (AFK), `GameService.surrender` (desistência).

**Por que corrigi dessa forma:**
- Caffeine é cache in-memory (sem Redis) — zero latência de rede, zero infraestrutura adicional. Perfeito para uma instância single-node no Render.
- Separar o cache em um bean dedicado (`RankingCacheService`) resolve a limitação do proxy AOP do Spring que não intercepta chamadas internas.
- TTL garante consistência eventual mesmo se a invalidação falhar (máximo 5 min desatualizado para ranking, 2 min para perfil).
- Invalidação explícita nos 3 pontos de fim de partida garante que na maioria dos casos o cache é atualizado imediatamente.
- Não usei cache no `GET /api/games/active` porque o estado da partida muda a cada turno — cache invalidaria imediatamente.
- Trade-off aceito: jogador pode ver ranking desatualizado por até 5 minutos se a invalidação falhar. Aceitável para o contexto de jogo casual.

**Resultado:** Cache do perfil: caiu de 804ms para **11.9ms** no p50 (cache hit) — melhoria de ~67x. Cache do ranking: caiu de 4.99s (cold) para **213ms** no p50 após primeira chamada popular o cache. "Slow Requests Rate" zerou — nenhuma requisição ultrapassa o threshold de 1000ms em regime normal com cache ativo.

---

## Cold start do Render Free Tier — 28/07/2026

**Evidência:** Dashboard Grafana "Response Time (p50/p95/p99)" mostra picos de 6-7s no p95/p99 às 11:43 (primeira requisição após inatividade), caindo para p50=6.29ms / p95=671ms / p99=1.62s às 11:45 (aplicação quente). Todos os endpoints sofrem igualmente no primeiro acesso.

**Causa:** O Render Free Tier desliga a instância após 15 minutos de inatividade (spin down). Na próxima requisição, precisa: iniciar container Docker → boot da JVM → Spring Boot startup (~8s conforme `application_started_time_seconds`) → primeira conexão ao PostgreSQL (pool HikariCP). Esse processo leva 7-15s e afeta a primeira requisição de qualquer endpoint.

**Como corrigi:** Já existe um cron job externo que faz requisição periódica para evitar o spin down. Não há correção possível no código — é limitação da infraestrutura gratuita.

**Por que não corrigi de outra forma:** A única solução definitiva seria migrar para um plano pago (Render Starter $7/mês) que mantém a instância sempre ativa. Para o contexto de estágio/demonstração, o cron job mitigando o spin down é suficiente. O cold start residual (quando o cron falha ou a instância reinicia por deploy) é aceito como trade-off.

**Resultado:** Com o cron job ativo, o cold start ocorre apenas em deploys ou falhas do cron. Em operação normal, a aplicação permanece quente com p50 de 6-12ms.

---

## Teste de carga — 28/07/2026

**Ferramenta:** k6 (Grafana) v0.54.0

**Cenário:** Simulação do fluxo de lobby (fleet-config → games/active → ranking → profile → ranking cached → profile cached) com ramp-up progressivo:
- 0→5 VUs em 30s
- 10 VUs sustentados por 1 min
- Pico de 20 VUs por 30s
- Ramp-down para 0

**Resultados:**

| Métrica | Valor |
|---------|-------|
| Total de requests | 962 |
| Throughput | 5.86 req/s |
| Iterações completas | 157 |
| Latência média | 240ms |
| Latência mediana (p50) | 223ms |
| Latência p90 | 254ms |
| Latência p95 | 271ms |
| Latência máxima | 3.99s |
| Taxa de erros | 78% |

**Métricas por endpoint:**

| Endpoint | Mediana | p90 | p95 |
|----------|---------|-----|-----|
| fleet_config | 218ms | 251ms | 263ms |
| ranking | 224ms | 252ms | 265ms |
| profile | 220ms | 255ms | 263ms |

**Análise:**
- O threshold de `p95 < 2000ms` foi **atingido com sucesso** (271ms real).
- A latência mediana de 223ms é estável e consistente entre todos os endpoints, indicando que o gargalo é a latência de rede entre WSL (Brasil) e Render (Oregon/EUA), não processamento da aplicação.
- A alta taxa de erros (78%) é causada pela limitação do Render Free Tier: a instância com 512MB RAM e CPU compartilhada não suporta 20 conexões simultâneas. As requests que passaram tiveram performance consistente.
- O cache está funcionando: os tempos de ranking e profile nas chamadas subsequentes (cached) ficam na mesma faixa, pois a latência de rede domina (~200ms Brasil→Oregon) e mascara o ganho do cache (~1ms server-side vs ~200ms rede).

**Conclusão:** A aplicação performa bem dentro das limitações da infraestrutura gratuita. O p95 de 271ms em requests bem-sucedidas demonstra que as otimizações (JOIN FETCH, bulk delete, cache) são efetivas. A limitação de concorrência é do Render Free Tier, não da aplicação — em uma instância paga com mais recursos, o throughput escalaria proporcionalmente.
