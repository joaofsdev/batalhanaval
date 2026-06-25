# Batalha Naval — API Backend

Backend do jogo **Batalha Naval multiplayer online** em tempo real, construído com Java 21 e Spring Boot. Dois jogadores se enfrentam em partidas com tabuleiro 10x10, comunicação via WebSocket e servidor autoritativo que garante o Fog of War.

---

## Stack e Justificativas

| Tecnologia | Justificativa |
|-----------|---------------|
| **Java 21** | LTS mais recente, suporte a virtual threads, pattern matching e records |
| **Spring Boot 4** | Ecossistema maduro para REST + WebSocket + Security + JPA num único projeto |
| **Spring WebSocket (STOMP/SockJS)** | Pub/sub nativo com user destinations para comunicação privada; SockJS como fallback |
| **Spring Security + JWT** | Autenticação stateless, adequada para API REST + handshake WebSocket |
| **Spring Data JPA** | Abstração produtiva para persistência |
| **MySQL (produção)** | Banco relacional robusto, gratuito no tier Railway/Render |
| **H2 (desenvolvimento)** | Banco em memória, zero config, ideal para dev e testes |
| **Flyway** | Versionamento de schema; migrações reproduzíveis |
| **Lombok** | Reduz boilerplate em entidades e DTOs |
| **SpringDoc OpenAPI** | Documentação automática da API REST |
| **Railway/Render (back)** | Deploy simples com variáveis de ambiente, suporte a Java e MySQL managed |
| **Vercel (front)** | Deploy otimizado para SPAs com CDN global |

---

## Arquitetura

```
┌────────────────────┐         ┌──────────────────────────────────────┐         ┌─────────┐
│                    │  REST   │            SPRING BOOT               │         │         │
│   Cliente Web      │◄───────►│                                      │         │  MySQL  │
│   (Vercel)         │         │  ┌────────────┐  ┌───────────────┐  │   JPA   │  (prod) │
│                    │  STOMP  │  │ Controllers│  │  Services     │◄─┼────────►│         │
│                    │◄═══════►│  └────────────┘  │ (invariantes) │  │         │  H2     │
│                    │  SockJS │  ┌────────────┐  └───────────────┘  │         │  (dev)  │
│                    │         │  │ WS Handlers│  ┌───────────────┐  │         │         │
│                    │         │  └────────────┘  │ Repositories  │  │         │         │
└────────────────────┘         │  ┌────────────┐  └───────────────┘  │         └─────────┘
                               │  │  Security  │                      │
                               │  │ JWT Filter │                      │
                               │  └────────────┘                      │
                               └──────────────────────────────────────┘
```

### Camadas

| Camada | Responsabilidade |
|--------|-----------------|
| `controller/` | Endpoints REST — auth, criação de partida, posicionamento |
| `websocket/` | Handlers STOMP — processamento de tiros em tempo real |
| `service/` | Lógica de negócio e invariantes de domínio |
| `domain/` | Entidades JPA, Enums |
| `repository/` | Interfaces Spring Data JPA |
| `security/` | JWT filter, interceptors WebSocket, UserDetailsService |
| `dto/` | Objetos de transferência (request/response) |
| `config/` | Security chain, WebSocket broker, CORS |
| `exception/` | Handler global de erros, exceções de domínio |

---

## Fog of War — Como é Garantido

O servidor é **autoritativo**: o cliente nunca recebe o tabuleiro do oponente.

1. **DTOs separados por perspectiva** — O response retorna `myBoard` (completo, com navios) e `opponentBoard` (apenas tiros já feitos e resultados). Nunca inclui `ships` ou `hasShip` do oponente.

2. **User Destinations no WebSocket** — Cada jogador recebe mensagens no seu canal privado (`/user/queue/...`). Atacante recebe resultado detalhado; defensor recebe apenas notificação de ataque.

3. **Broadcast contém apenas estado público** — O canal `/topic/game/{id}/state` transmite somente turno atual, status e vencedor.

4. **Entidades nunca serializam direto** — Todas as respostas passam por DTOs que excluem campos sensíveis.

5. **Validação de participante** — Toda requisição valida que o usuário é participante da partida antes de retornar qualquer dado.

---

## Como Rodar Localmente

### Pré-requisitos

- Java 21 (JDK)
- Maven 3.9+ (ou use o wrapper `./mvnw`)

### Passos

```bash
# 1. Clone o repositório
git clone https://github.com/seu-usuario/batalhanaval-api.git
cd batalhanaval-api

# 2. Rode com perfil dev (H2 em memória)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# O servidor inicia em http://localhost:8080
```

### Testando a API

**REST com curl:**

```bash
# Registrar
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"jogador1","email":"j1@email.com","password":"senha1234"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"jogador1","password":"senha1234"}'

# Criar/entrar em partida
curl -X POST http://localhost:8080/api/games \
  -H "Authorization: Bearer <token>"
```

**WebSocket com wscat:**

```bash
wscat -c "ws://localhost:8080/ws?token=<jwt>"
```

**Swagger UI:** `http://localhost:8080/swagger-ui.html`

**Console H2:** `http://localhost:8080/h2-console` (URL: `jdbc:h2:mem:batalhanaval`, user: `sa`, sem senha)

---

## Variáveis de Ambiente

| Variável | Obrigatória | Descrição | Exemplo |
|----------|-------------|-----------|---------|
| `SPRING_PROFILES_ACTIVE` | Sim | Perfil Spring ativo | `prod` |
| `DB_URL` | Sim (prod) | JDBC URL do MySQL | `jdbc:mysql://host:3306/batalhanaval?useSSL=true` |
| `DB_USERNAME` | Sim (prod) | Usuário do banco | `batalhanaval_user` |
| `DB_PASSWORD` | Sim (prod) | Senha do banco | `(secret)` |
| `JWT_SECRET` | Sim (prod) | Chave HMAC-SHA256 (min 32 chars) | `(secret)` |
| `CORS_ALLOWED_ORIGINS` | Sim (prod) | Origens permitidas | `https://batalhanaval.vercel.app` |
| `PORT` | Não | Porta do servidor (default 8080) | `8080` |

---

## Testes

### Executar todos os testes

```bash
./mvnw test
```

### Executar teste específico

```bash
./mvnw test -Dtest=PlacementServiceTest
```

### Cobertura

| Camada | Tipo | Ferramentas |
|--------|------|-------------|
| Domínio/Service | Unitário | JUnit 5, Mockito, AssertJ |
| Service + Repository | Integração | @SpringBootTest, H2 |
| Controllers REST | Controller | @WebMvcTest, MockMvc |
| Segurança | Cross-cutting | MockMvc com tokens válidos/inválidos |

**Regras de domínio cobertas:**
- Posicionamento válido/inválido de navios
- Cálculo de resultado de tiro (MISS, HIT, SUNK)
- Detecção de condição de vitória
- Controle de turno
- Célula já atacada

---

## Deploy

### Railway (recomendado)

1. Crie um projeto no [Railway](https://railway.app)
2. Adicione um serviço **MySQL**
3. Adicione um serviço conectado ao repositório GitHub
4. Configure as variáveis de ambiente (seção acima)
5. Railway detecta o `pom.xml` e builda automaticamente
6. Flyway roda as migrations no primeiro deploy

### Render (alternativa)

1. Crie um **Web Service** no [Render](https://render.com)
2. Conecte ao repositório GitHub
3. Build command: `./mvnw clean package -DskipTests`
4. Start command: `java -jar target/batalhanaval-api-0.0.1-SNAPSHOT.jar`
5. Adicione um banco MySQL (ou use Railway/PlanetScale)
6. Configure as variáveis de ambiente

### Frontend (Vercel)

1. Deploy do frontend no Vercel
2. Configure `CORS_ALLOWED_ORIGINS` no backend com a URL do Vercel
