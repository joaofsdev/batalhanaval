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

## Documentação Detalhada

- [Backend — README](backend/README.md): endpoints, WebSocket, configurações, testes
- [Frontend — README](frontend/README.md): estrutura, hooks, padrões arquiteturais
- [Modo Tempestade](docs/modo-tempestade-atual.md): regras completas de habilidades e eventos climáticos
