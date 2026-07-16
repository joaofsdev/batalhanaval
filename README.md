# Batalha Naval — Multiplayer Online

Jogo de Batalha Naval multiplayer em tempo real, desenvolvido como projeto do programa de estágio SoftExpert. Suporta dois modos de jogo: **Clássico** e **Tempestade** (com habilidades especiais e eventos climáticos).

## Arquitetura

```
┌──────────────────┐        REST + WebSocket (STOMP)        ┌──────────────────────┐
│     Frontend     │ ◄────────────────────────────────────► │       Backend        │
│    React SPA     │                                        │   Spring Boot API    │
│    (Vercel)      │                                        │   (Docker/Railway)   │
└──────────────────┘                                        └──────────┬───────────┘
                                                                       │
                                                                       ▼
                                                            ┌──────────────────────┐
                                                            │     PostgreSQL       │
                                                            │     (produção)       │
                                                            └──────────────────────┘
```

- **Frontend**: SPA React servindo a interface do jogo. Deploy via Vercel.
- **Backend**: API REST + WebSocket STOMP que implementa toda a lógica de negócio. Deploy containerizado (Docker).
- **Comunicação**: REST para ações pontuais (auth, criação de partida, posicionamento) e WebSocket para interação em tempo real (tiros, notificações de turno, eventos de tempestade).
- **Estado**: 100% server-authoritative — o frontend apenas renderiza o estado recebido do servidor.

## Estrutura do Repositório

```
batalhanaval/
├── backend/          → API Spring Boot (Java 21)
├── frontend/         → SPA React (Vite + Tailwind)
├── docs/             → Documentação adicional (Modo Tempestade)
└── README.md         → Este arquivo
```

## Stack

| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| Frontend | React | 18.3 |
| Build tool | Vite | 5.4 |
| Estilização | Tailwind CSS | 3.4 |
| Backend | Spring Boot (Java) | 4.0.7 / Java 21 |
| Tempo Real | WebSocket STOMP + SockJS | — |
| Banco (dev) | H2 | embarcado |
| Banco (prod) | PostgreSQL | — |
| Autenticação | JWT (HMAC-SHA256) | jjwt 0.12.6 |
| Rate Limiting | Bucket4j | 8.10.1 |
| Documentação API | SpringDoc OpenAPI | 3.0.2 |

## Funcionalidades

- Registro e autenticação de jogadores (JWT)
- Matchmaking automático por modo de jogo (Clássico / Tempestade)
- Sala privada com convite por token
- Posicionamento de frota com validação server-side
- Combate em tempo real via WebSocket
- Fog of War — posições do oponente nunca são expostas
- Modo Tempestade: habilidades especiais (Radar, Torpedo Duplo, Escudo, Bombardeio em Linha) e eventos climáticos (Nevoeiro, Maré, Corrente, Calmaria)
- Detecção de AFK com cancelamento automático
- Sistema de desconexão com grace period e reconexão
- Sistema de rematch
- Ranking com filtro por período
- Perfil de jogador com estatísticas
- Painel administrativo (gestão de usuários, partidas, audit logs)
- Rate limiting em endpoints de autenticação

## Como Rodar Localmente

### Pré-requisitos

- Java 21 (JDK)
- Node.js 18+
- npm

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

O servidor inicia na porta **8080** com perfil `dev` (banco H2 em arquivo local, sem necessidade de configuração externa).

### Frontend

```bash
cd frontend
npm install
npm run dev
```

O dev server inicia na porta **5173**. A variável `VITE_API_BASE_URL` no arquivo `.env` já aponta para `http://localhost:8080`.

### Verificação

Abrir `http://localhost:5173` em duas abas/navegadores diferentes, registrar dois usuários distintos e iniciar uma partida.

## Decisões de Design

| Decisão | Alternativas consideradas | Motivo |
|---------|--------------------------|--------|
| **STOMP sobre WebSocket puro** | WebSocket raw (frames custom), Server-Sent Events | STOMP oferece roteamento de mensagens por destino (`/topic/game/{id}/state`, `/user/queue/...`), permitindo broadcast seletivo para subscribers de uma partida sem lógica manual de dispatch. Integra nativamente com a abstração `SimpMessagingTemplate` do Spring, eliminando boilerplate de serialização e routing. WebSocket raw exigiria implementar um protocolo de mensagens ad-hoc; SSE não suporta comunicação bidirecional. |
| **BCrypt para hash de senha** | SHA-256, Argon2, PBKDF2 | BCrypt é um hash lento por design — inclui salt automático por invocação e fator de custo ajustável (padrão 10 = ~100ms), tornando ataques de brute-force via GPU inviáveis. SHA-256 é um hash rápido (~bilhões/s em GPU), inadequado para senhas. Argon2 seria superior em teoria, mas BCrypt é o padrão consolidado no ecossistema Spring Security (`BCryptPasswordEncoder`) com suporte zero-config. |
| **Optimistic locking (`@Version`)** | Pessimistic locking (`SELECT FOR UPDATE`), mutex em memória | Contenção real é baixa: cada partida tem exatamente 2 jogadores e turnos alternados, então conflitos (double-fire) são raros e causados por race conditions de rede, não por alta concorrência. Optimistic locking evita o overhead de locks no banco em 99.9% das requests e trata o caso raro via catch de `ObjectOptimisticLockingFailureException`, retornando erro amigável ao cliente. Pessimistic locking adicionaria latência desnecessária em toda operação de tiro. |
| **H2 em dev, PostgreSQL em prod** | PostgreSQL em todos os ambientes, SQLite | H2 embarcado oferece zero-config (sem instalar/rodar serviço externo), inicialização instantânea e modo de compatibilidade PostgreSQL para as queries. Isso acelera o onboarding: `./mvnw spring-boot:run` funciona sem dependências externas. Trade-off assumido: divergências sutis de dialeto podem surgir (funções específicas de Postgres). Para maior fidelidade, há `docker-compose` com PostgreSQL 16 como alternativa local. |
| **Estado 100% server-authoritative (sem optimistic updates no frontend)** | Optimistic UI com rollback | Elimina toda uma classe de bugs de inconsistência de estado entre client e server. O frontend nunca exibe dados que o backend não confirmou — isso é especialmente crítico para fog-of-war, onde qualquer estado local "otimista" poderia vazar inferências sobre o tabuleiro oponente. O custo é latência visível no feedback de ação (~50-100ms em WebSocket), aceitável para um jogo por turnos. |

**ADR adicional:** [Scheduler Single-Thread para Timeouts de Desconexão](backend/docs/adr-scheduler-single-thread.md) — documenta por que um `ScheduledExecutorService` de thread única é suficiente para gerenciar grace periods de reconexão no volume esperado do sistema.

## Documentação Detalhada

- [Backend — README](backend/README.md): endpoints, WebSocket, configurações, testes
- [Frontend — README](frontend/README.md): estrutura, hooks, padrões arquiteturais
- [Modo Tempestade](docs/modo-tempestade-atual.md): regras completas de habilidades e eventos climáticos
