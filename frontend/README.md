# Frontend — Batalha Naval

SPA (Single Page Application) que serve como camada de apresentação do jogo de Batalha Naval multiplayer. Toda a lógica de negócio reside no backend; o frontend é responsável pela interface, renderização de estado recebido via WebSocket e interações do usuário.

## Stack

| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| React | 18.3.1 | Biblioteca de UI |
| React Router DOM | 7.18.0 | Roteamento SPA |
| Vite | 5.4.1 | Build tool e dev server (HMR) |
| Tailwind CSS | 3.4.19 | Estilização utilitária |
| Axios | 1.18.1 | Cliente HTTP (REST) |
| @stomp/stompjs | 7.3.0 | Cliente STOMP sobre WebSocket |
| sockjs-client | 1.6.1 | Fallback de transporte WebSocket |
| ESLint | 9.9.0 | Linting |

## Estrutura de Pastas

```
src/
├── api/              → Clientes HTTP (axiosClient, authApi, gameApi, userApi, adminApi)
├── assets/           → Recursos estáticos (imagens, sprites)
├── components/       → Componentes React organizados por domínio
│   ├── Board/        → Tabuleiro de jogo
│   ├── Placement/    → Interface de posicionamento de frota
│   ├── GameStatus/   → Indicadores de estado da partida
│   ├── Storm/        → Componentes do modo Tempestade
│   └── shared/       → Componentes reutilizáveis
├── constants/        → Constantes (tipos de navio, sprites)
├── context/          → React Context (AuthContext — JWT e estado do usuário)
├── hooks/            → Custom hooks
│   ├── useGame.js          → Reducer de estado da partida
│   ├── useWebSocket.js     → Conexão STOMP principal
│   ├── useStormWebSocket.js → Subscrição em eventos de tempestade
│   └── useSoundEffects.js  → Efeitos sonoros
├── pages/            → Páginas/telas
│   ├── AuthPage.jsx        → Login e registro
│   ├── LobbyPage.jsx       → Lobby (matchmaking e salas)
│   ├── GamePage.jsx        → Tela principal do jogo
│   ├── RoomPage.jsx        → Sala privada
│   ├── ProfilePage.jsx     → Perfil do jogador
│   └── admin/              → Painel administrativo
├── App.jsx           → Router principal e definição de rotas
├── main.jsx          → Entry point (React DOM)
└── index.css         → Diretivas Tailwind
```

## Instalação e Execução

### Desenvolvimento

```bash
npm install
cp .env.example .env
npm run dev
```

O dev server inicia em `http://localhost:5173` com Hot Module Replacement.

### Build de Produção

```bash
npm run build
npm run preview
```

O build gera os arquivos otimizados no diretório `dist/`.

## Variáveis de Ambiente

| Variável | Obrigatória | Descrição |
|----------|-------------|-----------|
| `VITE_API_BASE_URL` | Sim | URL base da API backend (ex: `http://localhost:8080`) |

Arquivo `.env.example` disponível na raiz do frontend como referência.

## Scripts Disponíveis

| Comando | Descrição |
|---------|-----------|
| `npm run dev` | Inicia dev server com HMR (Vite) |
| `npm run build` | Gera build de produção em `dist/` |
| `npm run preview` | Serve o build localmente para validação |
| `npm run lint` | Executa ESLint no código-fonte |

## Padrões Arquiteturais

### Estado Server-Authoritative

O frontend **não** implementa optimistic updates. Todo estado exibido é recebido diretamente do servidor via WebSocket. O fluxo é:

1. Usuário realiza ação (ex: dispara tiro)
2. Mensagem STOMP é enviada ao backend
3. Backend processa, valida e emite o novo estado
4. Frontend atualiza a UI com o estado recebido

Essa decisão elimina inconsistências de estado e simplifica a lógica do cliente.

### Comunicação com Backend

- **REST (Axios)**: autenticação, criação/entrada em partida, posicionamento de frota, consultas de perfil e ranking
- **WebSocket (STOMP/SockJS)**: disparos, resultados de tiro, notificações de turno, eventos de tempestade, reconexão, rematch

### Gerenciamento de Estado

- `AuthContext` — armazena token JWT no localStorage; interceptor Axios injeta o header `Authorization` automaticamente
- `useGame` — hook com reducer local que gerencia estado da partida (tabuleiros, turno, status)
- `useWebSocket` — gerencia conexão STOMP com autenticação via query param e reconexão automática
- `useStormWebSocket` — subscrição dedicada aos tópicos de eventos climáticos do modo Tempestade

### Fluxo de Telas

```
AuthPage (/) → LobbyPage (/lobby) → GamePage (/game/:id)
                    ↓
              RoomPage (/room/:id)
                    ↓
              ProfilePage (/profile)
```

Rotas protegidas redirecionam para `AuthPage` se não houver token válido.

## Deploy

Deploy configurado para Vercel com rewrite SPA — todas as rotas redirecionam para `index.html` (configurado em `vercel.json`).
