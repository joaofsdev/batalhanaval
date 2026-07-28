#!/bin/bash
# =============================================================================
# Setup Script - Observabilidade Batalha Naval (WSL)
#
# Este script:
#   1. Verifica/instala Docker no WSL
#   2. Sobe a stack de observabilidade (Prometheus + Grafana)
#   3. Monitora o backend em produção no Render
#
# Uso: bash setup-observability.sh
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ---------------------------------------------------------------------------
# 1. Verificar se estamos no WSL
# ---------------------------------------------------------------------------
if ! grep -qEi "(Microsoft|WSL)" /proc/version 2>/dev/null; then
    log_warn "Este script foi feito para WSL, mas pode funcionar em Linux nativo também."
fi

# ---------------------------------------------------------------------------
# 2. Verificar/Instalar Docker
# ---------------------------------------------------------------------------
install_docker() {
    log_info "Instalando Docker..."

    # Atualizar pacotes
    sudo apt-get update -qq

    # Instalar dependências
    sudo apt-get install -y -qq \
        apt-transport-https \
        ca-certificates \
        curl \
        gnupg \
        lsb-release

    # Adicionar chave GPG oficial do Docker
    sudo install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    sudo chmod a+r /etc/apt/keyrings/docker.gpg

    # Adicionar repositório
    echo \
        "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
        $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
        sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

    # Instalar Docker Engine
    sudo apt-get update -qq
    sudo apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

    # Adicionar usuário ao grupo docker (evita sudo)
    sudo usermod -aG docker "$USER"

    log_info "Docker instalado com sucesso!"
    log_warn "Se é a primeira vez, pode precisar reiniciar o terminal (newgrp docker) ou o WSL."
}

if command -v docker &> /dev/null; then
    log_info "Docker já está instalado: $(docker --version)"
else
    log_warn "Docker não encontrado."
    read -p "Deseja instalar o Docker? (s/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Ss]$ ]]; then
        install_docker
    else
        log_error "Docker é necessário. Abortando."
        exit 1
    fi
fi

# ---------------------------------------------------------------------------
# 3. Verificar se Docker daemon está rodando
# ---------------------------------------------------------------------------
if ! docker info &> /dev/null 2>&1; then
    log_warn "Docker daemon não está rodando. Iniciando..."
    sudo service docker start
    sleep 2

    if ! docker info &> /dev/null 2>&1; then
        log_error "Não foi possível iniciar o Docker daemon."
        log_info "Tente: sudo service docker start"
        exit 1
    fi
fi

log_info "Docker daemon está rodando ✓"

# ---------------------------------------------------------------------------
# 4. Verificar docker compose
# ---------------------------------------------------------------------------
if ! docker compose version &> /dev/null 2>&1; then
    log_error "docker compose plugin não encontrado."
    log_info "Tente: sudo apt-get install docker-compose-plugin"
    exit 1
fi

log_info "Docker Compose disponível: $(docker compose version --short)"

# ---------------------------------------------------------------------------
# 5. Navegar até o diretório do projeto
# ---------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

log_info "Diretório do projeto: $SCRIPT_DIR"

# ---------------------------------------------------------------------------
# 6. Verificar se o backend está acessível
# ---------------------------------------------------------------------------
log_info "Verificando acesso ao backend no Render..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" https://batalhanaval-te2c.onrender.com/actuator/health 2>/dev/null || echo "000")

if [ "$HTTP_CODE" = "200" ]; then
    log_info "Backend acessível ✓ (HTTP $HTTP_CODE)"
elif [ "$HTTP_CODE" = "000" ]; then
    log_warn "Não foi possível conectar ao backend. Pode estar em cold start (Render free tier)."
    log_info "O Prometheus vai tentar novamente automaticamente a cada 10s."
else
    log_warn "Backend respondeu com HTTP $HTTP_CODE. O endpoint /actuator/prometheus pode não estar habilitado ainda."
    log_info "Certifique-se de fazer deploy do backend com as configurações de observabilidade."
fi

# ---------------------------------------------------------------------------
# 7. Subir a stack
# ---------------------------------------------------------------------------
log_info "Subindo stack de observabilidade..."
docker compose -f docker-compose-observability.yml up -d

echo ""
log_info "======================================"
log_info "  Stack de Observabilidade ONLINE! "
log_info "======================================"
echo ""
log_info "Serviços:"
echo "  📊 Prometheus:  http://localhost:9090"
echo "  📈 Grafana:     http://localhost:3001"
echo "     Login:       admin / admin"
echo ""
log_info "O Prometheus está coletando métricas de:"
echo "  🎯 https://batalhanaval-te2c.onrender.com/actuator/prometheus"
echo ""
log_info "Comandos úteis:"
echo "  Parar:     docker compose -f docker-compose-observability.yml down"
echo "  Logs:      docker compose -f docker-compose-observability.yml logs -f"
echo "  Reiniciar: docker compose -f docker-compose-observability.yml restart"
echo ""
