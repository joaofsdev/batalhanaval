Implementation Plan — Frontend Batalha Naval

Problem Statement:

Implementar o frontend web de um jogo de Batalha Naval multiplayer em tempo real, usando React + Vite, comunicando-se com um backend
Spring Boot via REST (Axios) e WebSocket (STOMP/SockJS). O frontend é puramente uma camada de apresentação — toda lógica de negócio é do
backend.

Requirements:

- Autenticação (login/registro) com JWT no localStorage
- Lobby com matchmaking (POST /api/games)
- Tela de partida com 4 sub-estados: WAITING → PLACING → IN_PROGRESS → FINISHED
- Posicionamento interativo de navios com preview/drag
- Combate em tempo real via WebSocket STOMP
- Reconexão WebSocket com resync via GET
- Nenhuma validação de domínio no frontend (apenas UX)
- 4 checkpoints obrigatórios de pausa

Background:

- Projeto já existe em C:\Projetos\batalhanaval\frontend com React 18 + Vite scaffolded
- Precisa instalar: axios, sockjs-client, @stomp/stompjs, react-router-dom, tailwindcss
- Backend roda em localhost:8080 e está pronto

Proposed Solution:

SPA React com 3 rotas, AuthContext global, hook useGame para estado da partida, hook useWebSocket para conexão STOMP. Tailwind para
estilos. Componentes separados por responsabilidade (Board, Placement, GameStatus). Interceptor Axios para JWT e 401 redirect.

────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task Breakdown:

Task 1: Estrutura base do projeto (PAUSE 1)

Objetivo: Configurar o projeto com todas as dependências, estrutura de pastas, Tailwind, axiosClient com interceptor JWT, AuthContext, e
react-router-dom com rotas placeholder.

Implementação:

- Instalar dependências: axios, sockjs-client, @stomp/stompjs, react-router-dom, tailwindcss (+ postcss, autoprefixer)
- Configurar Tailwind (tailwind.config.js, index.css com @tailwind directives)
- Criar estrutura de pastas conforme spec (api/, context/, hooks/, pages/, components/, constants/)
- Criar src/api/axiosClient.js com baseURL de env var, interceptor que injeta Authorization: Bearer do localStorage, e interceptor de
  resposta que redireciona para / em 401
- Criar src/api/authApi.js com register() e login()
- Criar src/api/gameApi.js com createOrJoinGame(), getGame(), placeShips()
- Criar src/context/AuthContext.jsx com Provider que expõe user/token/login/logout, lendo do localStorage na inicialização
- Criar src/components/shared/ProtectedRoute.jsx que redireciona para / se não tem token
- Criar src/components/shared/Toast.jsx — componente simples de notificação
- Configurar router no App.jsx com rotas /, /lobby, /game/:id apontando para páginas placeholder
- Criar src/constants/ships.js com a frota
- Criar .env.example com VITE_API_BASE_URL=http://localhost:8080

Testes: Verificar que npm run dev sobe sem erros, que o interceptor existe, que navegar para /lobby sem token redireciona para /.

Demo: Projeto roda, estrutura de pastas visível, ProtectedRoute funciona, axiosClient configurado.

────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task 2: Tela de Autenticação (AuthPage)

Objetivo: Implementar a tela de login/registro com toggle, chamada à API, persistência do token e redirecionamento.

Implementação:

- Criar src/pages/AuthPage.jsx com formulário que alterna entre Login e Cadastro
- Campos: username + password (login), username + email + password (registro)
- Validação UX: required em todos os campos antes de submit
- On submit: chamar authApi.login() ou authApi.register()
- Em sucesso: chamar authContext.login(data) que salva token + user no localStorage, depois navigate('/lobby')
- Em erro: exibir error.response.data.message como mensagem inline no formulário
- Se já tem token no localStorage ao montar: redirect para /lobby
- Estilo: formulário centralizado, tema escuro/naval com Tailwind

Testes: Login com credenciais válidas salva token e redireciona. Login com credenciais inválidas mostra erro. Registro com username
duplicado mostra erro.

Demo: Formulário funcional, token persistido, redirecionamento para lobby funcionando.

────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task 3: Tela de Lobby (LobbyPage)

Objetivo: Implementar o lobby com exibição do username, botão de matchmaking e logout.

Implementação:

- Criar src/pages/LobbyPage.jsx protegida pelo ProtectedRoute
- Exibir "Bem-vindo, {username}" e botão "Entrar em Partida"
- Ao clicar: chamar gameApi.createOrJoinGame()
- Em sucesso: navigate(/game/${response.data.id})
- Em erro 409 PLAYER_ALREADY_IN_GAME: exibir mensagem com opção de retomar (// TODO: obter gameId ativo do backend)
- Botão Logout: chamar authContext.logout(), navegar para /

Testes: Clicar em "Entrar em Partida" cria/entra jogo e redireciona. Logout limpa localStorage.

Demo: Fluxo completo auth → lobby → redirecionamento para game funcionando. (PAUSE 2)

────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task 4: Tela de Partida — Estado WAITING + Conexão WebSocket

Objetivo: Implementar GamePage com carregamento inicial, conexão WebSocket, e sub-estado WAITING.

Implementação:

- Criar src/hooks/useWebSocket.js — gerencia conexão STOMP/SockJS, subscribe, publish, reconexão com backoff exponencial
- Criar src/pages/GamePage.jsx que ao montar faz GET /api/games/{id}, conecta WebSocket, e renderiza sub-estado baseado em game.status
- Criar src/components/GameStatus/WaitingScreen.jsx — "Aguardando oponente..." com spinner
- Inscrever em /topic/game/{id}/player-joined — ao receber, refetch game state
- Inscrever em /user/queue/errors — exibir toast

Testes: Ao entrar numa partida WAITING, mostra spinner. Ao 2º jogador entrar (evento player-joined), transiciona para PLACING.

Demo: Tela de espera funcional, WebSocket conectado, transição automática ao oponente entrar.

────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task 5: Fase de Posicionamento (PlacementBoard)

Objetivo: Implementar a interface completa de posicionamento de navios com preview, seleção, remoção e envio ao backend.

Implementação:

- Criar src/components/Board/PlacementBoard.jsx — grid 10x10 interativo
- Criar src/components/Board/BoardCell.jsx — célula reutilizável com estados visuais
- Criar src/components/Placement/ShipSelector.jsx — lista de navios com indicação de posicionado/pendente
- Criar src/components/Placement/OrientationToggle.jsx — toggle H/V
- Lógica local (useState): navios posicionados, navio selecionado, orientação atual
- Hover sobre célula: preview visual das células que serão ocupadas (verificação superficial se cabe no grid)
- Click: posiciona navio nas células
- Botão remover navio já posicionado
- Botão "Confirmar Frota": habilitado quando 5 navios posicionados; ao clicar, envia POST /api/games/{id}/ships
- Em erro (SHIPS_OVERLAP, etc): exibir toast com mensagem do backend, permitir reposicionar
- Após confirmar com sucesso (boardReady=true, status ainda PLACING): exibir "Aguardando oponente posicionar frota..."
- Inscrever em /user/queue/game/started — ao receber, transicionar para IN_PROGRESS

Testes: Posicionar 5 navios, confirmar, ver mensagem de espera. Erro de overlap exibido. Transição ao receber started.

Demo: Posicionamento completo funcional com preview, validação do backend, e aguardo do oponente. (PAUSE 3)

────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task 6: Fase de Combate (IN_PROGRESS) — Tabuleiros + WebSocket

Objetivo: Implementar os dois tabuleiros lado a lado, envio de tiro via STOMP, e processamento de todos os eventos de combate.

Implementação:

- Criar src/components/Board/MyBoard.jsx — grid 10x10 read-only mostrando navios próprios + hits/misses recebidos
- Criar src/components/Board/OpponentBoard.jsx — grid 10x10 clicável, mostrando apenas shots realizados (MISS/HIT/SUNK), fog of war no
  resto
- Criar src/components/GameStatus/TurnIndicator.jsx — banner "Sua vez" / "Vez do oponente"
- Criar src/hooks/useGame.js — reducer que gerencia game state, processa eventos WS (shot-result, opponent-shot, state update)
- Ao clicar no OpponentBoard: publish para /app/game/{gameId}/fire com { row, col }
- Desabilitar clique (UX) se não é minha vez (comparar currentTurnPlayerId === myUserId)
- Processar /user/queue/game/shot-result: adicionar shot ao opponentBoard local, mostrar feedback (toast se SUNK com tipo do navio)
- Processar /user/queue/game/opponent-shot: marcar hit/miss no myBoard local
- Processar /topic/game/{id}/state: atualizar currentTurnPlayerId e status
- Processar /user/queue/errors: toast com mensagem

Testes: Tiro enviado, resultado recebido e exibido. Tiro do oponente aparece no meu tabuleiro. Turno alterna. Erro de "não é sua vez"
exibido.

Demo: Combate funcional entre 2 browsers, tiros refletidos em tempo real, indicador de turno funciona. (PAUSE 4)

────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task 7: Fim de Jogo + Reconexão + Polimento

Objetivo: Implementar tela de vitória/derrota, lógica de reconexão WebSocket, e polimento visual.

Implementação:

- Criar src/components/GameStatus/GameOverOverlay.jsx — overlay com resultado (vitória/derrota baseado em winnerId === myUserId)
- Botão "Jogar Novamente" → navigate('/lobby')
- Botão "Sair" → logout + navigate('/')
- No useWebSocket: implementar reconexão com backoff (a lib @stomp/stompjs suporta reconnectDelay); após reconectar, refetch GET
  /api/games/{id} para resync
- Polimento visual: animações de transição entre estados, feedback visual ao clicar (loading state no botão de confirmar frota, célula
  piscando ao atirar), responsividade básica
- Criar .env.example final documentado
- Limpar código: remover arquivos padrão do Vite (App.css, assets/react.svg, etc.)

Testes: Ao status=FINISHED, overlay aparece com resultado correto. Desconectar e reconectar WS mantém estado. Layout funciona em telas
menores.

Demo: Jogo completo end-to-end, do login até vitória/derrota, com reconexão funcional.
