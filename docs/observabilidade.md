# Observabilidade — Batalha Naval API

Solução completa de observabilidade para o backend Spring Boot, cobrindo métricas, traces distribuídos, identificação de requisições/queries lentas e dashboards.

## Arquitetura

```
┌──────────────────────┐         metrics (scrape)        ┌──────────────────┐
│   Batalha Naval API  │ ◄──────────────────────────────►│    Prometheus    │
│   (Spring Boot)      │                                 │    :9090         │
│                      │         traces (OTLP/HTTP)      ├──────────────────┤
│   /actuator/prometheus│ ─────────────────────────────► │      Tempo       │
│                      │                                 │    :4318         │
└──────────────────────┘                                 └───────┬──────────┘
                                                                 │
                                                                 ▼
                                                         ┌──────────────────┐
                                                         │     Grafana      │
                                                         │     :3001        │
                                                         │  (dashboards)    │
                                                         └──────────────────┘
```

## Stack

| Componente | Tecnologia | Função |
|-----------|-----------|--------|
| Métricas | Micrometer + Prometheus | Coleta e armazenamento de métricas (JVM, HTTP, DB, custom) |
| Traces | Micrometer Tracing + OpenTelemetry | Rastreamento distribuído de requisições end-to-end |
| Traces Backend | Grafana Tempo | Armazenamento e consulta de traces |
| Dashboards | Grafana | Visualização de métricas e traces |
| Slow Requests | SlowRequestFilter (custom) | Detecção e logging de requisições lentas |
| Slow Queries | SlowQueryInterceptor (AOP) | Detecção e logging de queries lentas |

## Como Rodar

### 1. Subir a stack de observabilidade

```bash
docker compose -f docker-compose-observability.yml up -d
```

Isso sobe:
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001 (login: `admin` / `admin`)
- **Tempo**: http://localhost:3200 (acesso interno via Grafana)

### 2. Rodar o backend

```bash
cd backend
./mvnw spring-boot:run
```

O backend já está configurado para:
- Expor métricas em `/actuator/prometheus`
- Enviar traces via OTLP para `localhost:4318`
- Logar slow requests e slow queries

### 3. Acessar os dashboards

Abra http://localhost:3001 e navegue para a pasta **Batalha Naval**:
- **Application Overview**: visão geral (request rate, latência, erros, JVM, DB connections)
- **Performance & Traces**: foco em performance (slow requests, slow queries, endpoints mais lentos, SLOs, traces)

## Métricas Coletadas

### HTTP
| Métrica | Descrição |
|---------|-----------|
| `http_server_requests_seconds` | Histograma de latência por endpoint (com percentis) |
| `http_server_requests_slow_total` | Contador de requisições que excedem o threshold |

### Database
| Métrica | Descrição |
|---------|-----------|
| `db_query_duration_seconds` | Histograma de duração de queries JPA |
| `db_query_slow_total` | Contador de queries que excedem o threshold |
| `hikaricp_connections_*` | Pool de conexões (active, idle, max, acquire time) |

### JVM
| Métrica | Descrição |
|---------|-----------|
| `jvm_memory_used_bytes` | Uso de memória heap/non-heap |
| `jvm_threads_live_threads` | Threads ativas |
| `jvm_gc_pause_seconds` | Duração de pausas do GC |
| `system_cpu_usage` / `process_cpu_usage` | Uso de CPU |

## Configuração de Thresholds

Os thresholds são configuráveis via `application-*.yaml`:

```yaml
observability:
  slow-request:
    threshold-ms: 500    # Requisições > 500ms são logadas como lentas
  slow-query:
    threshold-ms: 200    # Queries > 200ms são logadas como lentas
```

### Ambiente Dev (padrão)
- Slow request: **500ms**
- Slow query: **200ms**
- Trace sampling: **100%** (todas as requisições)

### Ambiente Prod
- Slow request: **1000ms**
- Slow query: **500ms**
- Trace sampling: **10%** (amostragem para reduzir overhead)

## Logs de Observabilidade

Slow requests e queries aparecem nos logs da aplicação:

```
WARN  [SLOW REQUEST] 1523ms | GET /api/games/123 | status=200 | remoteAddr=127.0.0.1
WARN  [SLOW QUERY] 345ms | method=GameRepository.findById(..) | args=[123]
```

## Traces Distribuídos

Cada requisição HTTP recebe um trace ID propagado automaticamente pelo Micrometer Tracing. O trace inclui:
- Span da requisição HTTP (método, URI, status, duração)
- Spans internos de cada operação de repositório JPA
- Propagação automática entre serviços via headers W3C Trace Context

Para consultar traces no Grafana:
1. Acesse o dashboard "Performance & Traces"
2. Use o painel "Recent Traces" para buscar por service name
3. Ou acesse Grafana → Explore → Tempo para buscas avançadas (por trace ID, duração, tags)

## Endpoints de Monitoramento

| Endpoint | Descrição |
|----------|-----------|
| `GET /actuator/health` | Health check (inclui detalhes de DB, disk) |
| `GET /actuator/info` | Informações da aplicação |
| `GET /actuator/prometheus` | Métricas em formato Prometheus |
| `GET /actuator/metrics` | Lista de métricas disponíveis (JSON) |
| `GET /actuator/metrics/{name}` | Detalhe de uma métrica específica |

## Estrutura de Arquivos

```
batalhanaval/
├── docker-compose-observability.yml    # Stack completa (Prometheus + Tempo + Grafana)
├── observability/
│   ├── prometheus/
│   │   └── prometheus.yml              # Config do Prometheus (scrape targets)
│   ├── tempo/
│   │   └── tempo.yml                   # Config do Tempo (receivers OTLP)
│   └── grafana/
│       ├── provisioning/
│       │   ├── datasources/
│       │   │   └── datasources.yml     # Prometheus + Tempo como datasources
│       │   └── dashboards/
│       │       └── dashboards.yml      # Provisioning de dashboards
│       └── dashboards/
│           ├── application-overview.json
│           └── performance-traces.json
└── backend/
    └── src/main/java/.../config/observability/
        ├── ObservabilityConfig.java     # @Timed annotation support
        ├── SlowQueryInterceptor.java    # AOP interceptor para queries lentas
        └── SlowRequestFilter.java      # Servlet filter para requests lentos
```

## Personalização

### Adicionar métricas customizadas a um service

Use a annotation `@Timed`:

```java
@Timed(value = "game.fire.duration", description = "Time to process a fire action")
public GameState fire(Long gameId, int x, int y) {
    // ...
}
```

### Alterar target do Prometheus em produção

Edite `observability/prometheus/prometheus.yml` e ajuste o `targets` para apontar para o host/IP correto da aplicação em produção.

### Alterar trace sampling em runtime

A propriedade `management.tracing.sampling.probability` pode ser alterada via variável de ambiente:
```bash
MANAGEMENT_TRACING_SAMPLING_PROBABILITY=0.5
```
