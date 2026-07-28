import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ============================================================================
// Teste de Carga — Batalha Naval API (Produção)
//
// Uso (no WSL):
//   k6 run load-test.js
//
// Instalar k6 no WSL:
//   sudo gpg -k
//   sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
//   echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
//   sudo apt-get update && sudo apt-get install k6
// ============================================================================

const BASE_URL = 'https://batalhanaval-te2c.onrender.com';

// Métricas customizadas
const loginDuration = new Trend('login_duration');
const rankingDuration = new Trend('ranking_duration');
const profileDuration = new Trend('profile_duration');
const fleetConfigDuration = new Trend('fleet_config_duration');
const errorRate = new Rate('errors');

// Configuração de carga
export const options = {
    stages: [
        { duration: '30s', target: 5 },   // Ramp-up: 0→5 usuários em 30s
        { duration: '1m', target: 10 },    // Sustentado: 10 usuários por 1 min
        { duration: '30s', target: 20 },   // Pico: 20 usuários por 30s
        { duration: '30s', target: 0 },    // Ramp-down
    ],
    thresholds: {
        http_req_duration: ['p(95)<2000'],  // 95% das requests < 2s
        errors: ['rate<0.1'],               // Menos de 10% de erros
    },
};

// Dados de teste — usuários pré-registrados
const TEST_USERS = [
    { username: 'loadtest1', email: 'load1@test.com', password: 'LoadTest123!' },
    { username: 'loadtest2', email: 'load2@test.com', password: 'LoadTest123!' },
    { username: 'loadtest3', email: 'load3@test.com', password: 'LoadTest123!' },
    { username: 'loadtest4', email: 'load4@test.com', password: 'LoadTest123!' },
    { username: 'loadtest5', email: 'load5@test.com', password: 'LoadTest123!' },
    { username: 'loadtest6', email: 'load6@test.com', password: 'LoadTest123!' },
    { username: 'loadtest7', email: 'load7@test.com', password: 'LoadTest123!' },
    { username: 'loadtest8', email: 'load8@test.com', password: 'LoadTest123!' },
    { username: 'loadtest9', email: 'load9@test.com', password: 'LoadTest123!' },
    { username: 'loadtest10', email: 'load10@test.com', password: 'LoadTest123!' },
];

// Setup: registrar usuários de teste (roda uma vez antes do teste)
export function setup() {
    const tokens = [];

    for (const user of TEST_USERS) {
        // Tenta registrar (ignora se já existe)
        http.post(`${BASE_URL}/api/auth/register`, JSON.stringify(user), {
            headers: { 'Content-Type': 'application/json' },
        });

        // Login para obter token
        const loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
            username: user.username,
            password: user.password,
        }), {
            headers: { 'Content-Type': 'application/json' },
        });

        if (loginRes.status === 200) {
            const body = JSON.parse(loginRes.body);
            tokens.push(body.token);
        }
    }

    return { tokens };
}

// Cenário principal: simula um jogador navegando no lobby
export default function (data) {
    const token = data.tokens[__VU % data.tokens.length];
    if (!token) return;

    const authHeaders = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
        },
    };

    group('Fluxo Lobby', () => {
        // 1. Buscar configuração de frota (público, leve)
        const fleetRes = http.get(`${BASE_URL}/api/games/fleet-config`);
        fleetConfigDuration.add(fleetRes.timings.duration);
        check(fleetRes, { 'fleet-config OK': (r) => r.status === 200 });
        errorRate.add(fleetRes.status !== 200);

        sleep(1);

        // 2. Verificar partida ativa
        const activeRes = http.get(`${BASE_URL}/api/games/active`, authHeaders);
        check(activeRes, { 'active OK': (r) => r.status === 200 || r.status === 204 });
        errorRate.add(activeRes.status !== 200 && activeRes.status !== 204);

        sleep(1);

        // 3. Carregar ranking
        const rankingRes = http.get(`${BASE_URL}/api/ranking?page=0&size=20&period=all`, authHeaders);
        rankingDuration.add(rankingRes.timings.duration);
        check(rankingRes, { 'ranking OK': (r) => r.status === 200 });
        errorRate.add(rankingRes.status !== 200);

        sleep(1);

        // 4. Carregar perfil
        const profileRes = http.get(`${BASE_URL}/api/users/me/profile`, authHeaders);
        profileDuration.add(profileRes.timings.duration);
        check(profileRes, { 'profile OK': (r) => r.status === 200 });
        errorRate.add(profileRes.status !== 200);

        sleep(1);

        // 5. Carregar ranking novamente (deve vir do cache)
        const rankingRes2 = http.get(`${BASE_URL}/api/ranking?page=0&size=20&period=all`, authHeaders);
        rankingDuration.add(rankingRes2.timings.duration);
        check(rankingRes2, { 'ranking cached OK': (r) => r.status === 200 });

        sleep(1);

        // 6. Carregar perfil novamente (deve vir do cache)
        const profileRes2 = http.get(`${BASE_URL}/api/users/me/profile`, authHeaders);
        profileDuration.add(profileRes2.timings.duration);
        check(profileRes2, { 'profile cached OK': (r) => r.status === 200 });

        sleep(2);
    });
}
