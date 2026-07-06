import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import GameHistory from "../components/GameStatus/GameHistory";
import * as gameApi from "../api/gameApi";

const LobbyPage = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [selectedMode, setSelectedMode] = useState("CLASSIC");
  const [ranking, setRanking] = useState([]);
  const [myPosition, setMyPosition] = useState(null);
  const [rankingPage, setRankingPage] = useState(0);
  const [rankingTotalPages, setRankingTotalPages] = useState(0);
  const [rankingPeriod, setRankingPeriod] = useState("all");
  const [rankingLoading, setRankingLoading] = useState(true);
  const [rankingError, setRankingError] = useState(false);
  const [rankingRetry, setRankingRetry] = useState(0);

  useEffect(() => {
    gameApi.getActiveGame()
      .then(({ status, data }) => {
        if (status === 200 && data?.id) {
          navigate(`/game/${data.id}`);
        }
      })
      .catch(() => {});
  }, [navigate]);

  useEffect(() => {
    setRankingLoading(true);
    setRankingError(false);
    gameApi.getRanking(rankingPage, 20, rankingPeriod)
      .then(({ data }) => {
        setRanking(data.ranking || []);
        setMyPosition(data.myPosition || null);
        setRankingTotalPages(data.totalPages || 0);
      })
      .catch(() => {
        setRankingError(true);
        setRanking([]);
      })
      .finally(() => setRankingLoading(false));
  }, [rankingPage, rankingPeriod, rankingRetry]);

  const handlePlay = async () => {
    setError("");
    setLoading(true);
    try {
      const res = await gameApi.createOrJoinGame(selectedMode);
      navigate(`/game/${res.data.id}`);
    } catch (err) {
      const code = err.response?.data?.code;
      if (code === "PLAYER_ALREADY_IN_GAME") {
        setError("Você já está em uma partida ativa.");
      } else {
        setError(err.response?.data?.message || "Erro ao buscar partida");
      }
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  return (
    <div className="h-screen flex flex-col bg-background overflow-hidden">
      {/* Header */}
      <header className="flex justify-between items-center w-full px-panel-padding h-16 z-40 bg-surface-container-low border-b border-outline-variant">
        <nav className="hidden md:flex gap-6">
          <span className="text-primary border-b-2 border-primary pb-1 font-label-caps text-label-caps">
            Lobby
          </span>
        </nav>

        <div
          onClick={() => navigate(`/profile/${user?.id}`)}
          className="flex items-center gap-3 cursor-pointer hover:opacity-80 transition-opacity"
        >
          <div className="w-8 h-8 rounded-full border border-primary bg-secondary-container flex items-center justify-center font-label-caps text-label-caps text-primary">
            {user?.username?.[0]?.toUpperCase()}
          </div>
          <span className="font-mono-data text-mono-data text-on-surface hidden md:block">
            {user?.username?.toUpperCase()}
          </span>
        </div>
      </header>

      <div className="flex flex-1 overflow-hidden">
        {/* Sidebar */}
        <aside className="hidden md:flex flex-col h-full w-64 border-r border-outline-variant bg-surface-container-low pt-4 z-40">
          {/* Perfil */}
          <div className="px-4 mb-8 flex flex-col items-center">
            <div className="w-16 h-16 rounded-full border-2 border-primary mb-2 bg-secondary-container flex items-center justify-center font-display-tactical text-display-tactical text-primary">
              {user?.username?.[0]?.toUpperCase()}
            </div>
            <h2 className="font-headline-md text-headline-md text-primary uppercase">
              {user?.username}
            </h2>
          </div>

          {/* Nav items */}
          <nav className="flex-1 flex flex-col gap-2">
            <div className="flex items-center gap-3 px-4 py-3 bg-secondary-container text-on-secondary-container border-l-4 border-primary font-label-caps text-label-caps">
              <span className="material-symbols-outlined">radar</span>
              VISÃO TATICA
            </div>
            <div
              onClick={() => navigate(`/profile/${user?.id}`)}
              className="flex items-center gap-3 px-4 py-3 text-on-surface-variant hover:bg-secondary-container hover:text-on-secondary-container cursor-pointer transition-colors font-label-caps text-label-caps"
            >
              <span className="material-symbols-outlined">person</span>
              MEU PERFIL
            </div>
            {user?.role === 'ADMIN' && (
              <div
                onClick={() => navigate('/admin')}
                className="flex items-center gap-3 px-4 py-3 text-on-surface-variant hover:bg-secondary-container hover:text-on-secondary-container cursor-pointer transition-colors font-label-caps text-label-caps"
              >
                <span className="material-symbols-outlined">admin_panel_settings</span>
                PAINEL ADMIN
              </div>
            )}
          </nav>

          {/* Logout */}
          <button
            onClick={handleLogout}
            className="p-4 flex items-center justify-center gap-2 text-on-surface-variant hover:text-error transition-colors"
          >
            <span className="material-symbols-outlined">
              power_settings_new
            </span>
            <span className="font-label-caps text-label-caps">LOGOUT</span>
          </button>
        </aside>

        {/* Conteúdo principal */}
        <main className="flex-1 flex flex-col md:flex-row p-margin-safe gap-margin-safe overflow-auto">
          {/* Coluna esquerda — Ranking + Histórico */}
          <div className="w-full md:w-1/2 flex flex-col gap-margin-safe">
          <section className="bg-surface-container border border-outline-variant p-panel-padding flex flex-col gap-4">
            <header className="border-b border-outline-variant pb-2 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-sm">
                  leaderboard
                </span>
                <h2 className="font-headline-md text-headline-md text-primary tracking-widest">
                  RANKING
                </h2>
              </div>
              <div className="flex gap-1">
                {[
                  { label: "7D", value: "week" },
                  { label: "30D", value: "month" },
                  { label: "ALL", value: "all" },
                ].map((opt) => (
                  <button
                    key={opt.value}
                    onClick={() => { setRankingPeriod(opt.value); setRankingPage(0); }}
                    className={`px-2 py-1 font-label-caps text-[10px] border transition-colors ${
                      rankingPeriod === opt.value
                        ? "border-primary text-primary bg-primary-container/10"
                        : "border-outline-variant text-on-surface-variant hover:text-primary"
                    }`}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            </header>

            <div className="grid grid-cols-6 gap-2 px-2 py-1 border-b border-outline-variant">
              {["#", "OPERADOR", "ELO", "V", "P", "WIN%"].map((col) => (
                <span
                  key={col}
                  className="font-label-caps text-label-caps text-on-surface-variant"
                >
                  {col}
                </span>
              ))}
            </div>

            {/* Dados do ranking */}
            {rankingLoading ? (
              // Skeleton rows
              Array.from({ length: 5 }, (_, i) => (
                <div key={i} className="grid grid-cols-6 gap-2 px-2 py-2 border-b border-outline-variant/30">
                  <div className="h-4 w-6 bg-surface-container-high animate-pulse" />
                  <div className="h-4 w-20 bg-surface-container-high animate-pulse" />
                  <div className="h-4 w-8 bg-surface-container-high animate-pulse" />
                  <div className="h-4 w-6 bg-surface-container-high animate-pulse" />
                  <div className="h-4 w-6 bg-surface-container-high animate-pulse" />
                  <div className="h-4 w-10 bg-surface-container-high animate-pulse" />
                </div>
              ))
            ) : rankingError ? (
              <div className="flex flex-col items-center gap-3 py-6">
                <span className="material-symbols-outlined text-error text-2xl">signal_wifi_off</span>
                <p className="font-mono-data text-mono-data text-error">
                  FALHA AO CARREGAR RANKING
                </p>
                <button
                  onClick={() => setRankingRetry((r) => r + 1)}
                  className="px-4 py-2 border border-primary text-primary font-label-caps text-label-caps hover:bg-primary-container/10 transition-colors"
                >
                  TENTAR NOVAMENTE
                </button>
              </div>
            ) : (
            ranking.map((row) => (
              <div
                key={row.userId}
                className="grid grid-cols-6 gap-2 px-2 py-2 border-b border-outline-variant/30 hover:bg-surface-container-high transition-colors"
              >
                <span className="font-mono-data text-mono-data text-primary-container">
                  {String(row.position).padStart(2, '0')}
                </span>
                <span
                  className="font-mono-data text-mono-data text-on-surface truncate cursor-pointer hover:text-primary transition-colors"
                  onClick={() => navigate(`/profile/${row.userId}`)}
                >
                  {row.username?.toUpperCase()}
                </span>
                <span className="font-mono-data text-mono-data text-tertiary">
                  {row.eloRating}
                </span>
                <span className="font-mono-data text-mono-data text-primary">
                  {row.wins}
                </span>
                <span className="font-mono-data text-mono-data text-on-surface-variant">
                  {row.totalGames}
                </span>
                <span className="font-mono-data text-mono-data text-tertiary">
                  {row.winRate}%
                </span>
              </div>
            )))}

            {/* Linha do usuário logado */}
            {!rankingLoading && !rankingError && myPosition && (
              <div className="grid grid-cols-6 gap-2 px-2 py-2 bg-secondary-container/30 border border-primary/30">
                <span className="font-mono-data text-mono-data text-primary-container">
                  {String(myPosition.position).padStart(2, '0')}
                </span>
                <span className="font-mono-data text-mono-data text-primary truncate">
                  {myPosition.username?.toUpperCase()} (VOCÊ)
                </span>
                <span className="font-mono-data text-mono-data text-tertiary">
                  {myPosition.eloRating}
                </span>
                <span className="font-mono-data text-mono-data text-primary">
                  {myPosition.wins}
                </span>
                <span className="font-mono-data text-mono-data text-on-surface-variant">
                  {myPosition.totalGames}
                </span>
                <span className="font-mono-data text-mono-data text-tertiary">
                  {myPosition.winRate}%
                </span>
              </div>
            )}

            {/* Paginação */}
            {rankingTotalPages > 1 && (
              <div className="flex items-center justify-center gap-4 pt-2">
                <button
                  onClick={() => setRankingPage((p) => Math.max(0, p - 1))}
                  disabled={rankingPage === 0}
                  className="font-label-caps text-label-caps text-primary disabled:text-on-surface-variant disabled:cursor-not-allowed"
                >
                  ← ANT.
                </button>
                <span className="font-mono-data text-mono-data text-on-surface-variant">
                  {rankingPage + 1} / {rankingTotalPages}
                </span>
                <button
                  onClick={() => setRankingPage((p) => Math.min(rankingTotalPages - 1, p + 1))}
                  disabled={rankingPage >= rankingTotalPages - 1}
                  className="font-label-caps text-label-caps text-primary disabled:text-on-surface-variant disabled:cursor-not-allowed"
                >
                  PRÓX. →
                </button>
              </div>
            )}
          </section>

          <GameHistory />
          </div>

          {/* Painel direito — Selecionar Missão */}
          <section className="w-full md:w-1/2 bg-surface-container border border-outline-variant p-panel-padding flex flex-col gap-4">
            <header className="border-b border-outline-variant pb-2 flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-sm">
                grid_view
              </span>
              <h2 className="font-headline-md text-headline-md text-primary tracking-widest">
                SELECIONAR MISSÃO
              </h2>
            </header>

            {/* Card Clássico */}
            <div
              onClick={() => setSelectedMode("CLASSIC")}
              className={`flex-1 border p-panel-padding flex flex-col gap-4 cursor-pointer transition-colors hover:bg-primary-container/5 ${
                selectedMode === "CLASSIC"
                  ? "border-primary bg-primary-container/5"
                  : "border-outline-variant hover:border-primary"
              }`}
            >
              <div className="flex items-start gap-4">
                <div className="w-16 h-16 border border-outline-variant flex items-center justify-center text-primary bg-surface-container-high">
                  <span className="material-symbols-outlined text-3xl">
                    sailing
                  </span>
                </div>
                <div className="flex-1">
                  <h3 className="font-headline-md text-headline-md text-on-surface mb-1">
                    CLÁSSICO
                  </h3>
                  <p className="font-body-md text-body-md text-on-surface-variant">
                    Combate naval tático padrão em malha de radar 10x10.
                    Posicione sua frota e elimine alvos inimigos.
                  </p>
                </div>
              </div>
              {selectedMode === "CLASSIC" && (
                <div className="flex flex-col gap-2">
                  <button
                    onClick={handlePlay}
                    disabled={loading}
                    className="w-full py-3 border border-primary text-primary font-label-caps text-label-caps transition-all duration-200 hover:bg-primary hover:text-on-primary hover:scale-[1.02] disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:scale-100 disabled:hover:bg-transparent disabled:hover:text-primary"
                  >
                    {loading ? "CONECTANDO..." : "INICIAR MISSÃO"}
                  </button>
                  <button
                    onClick={() => navigate("/room?mode=CLASSIC")}
                    className="w-full py-3 border border-primary text-primary font-label-caps text-label-caps transition-all duration-200 hover:bg-primary hover:text-on-primary hover:scale-[1.02]"
                  >
                    SALA DE GUERRA
                  </button>
                </div>
              )}
            </div>

            {/* Card Especial — TEMPESTADE */}
            <div
              onClick={() => setSelectedMode("STORM")}
              className={`flex-1 border p-panel-padding flex flex-col gap-4 cursor-pointer transition-colors hover:bg-primary-container/5 ${
                selectedMode === "STORM"
                  ? "border-primary bg-primary-container/5"
                  : "border-outline-variant hover:border-primary"
              }`}
            >
              <div className="flex items-start gap-4">
                <div className="w-16 h-16 border border-outline-variant flex items-center justify-center text-primary bg-surface-container-high">
                  <span className="material-symbols-outlined text-3xl">
                    auto_awesome
                  </span>
                </div>
                <div className="flex-1">
                  <h3 className="font-headline-md text-headline-md text-on-surface mb-1">
                    TEMPESTADE
                  </h3>
                  <p className="font-body-md text-body-md text-on-surface-variant">
                    Habilidades táticas únicas. Eventos climáticos aleatórios.
                    Para operadores avançados.
                  </p>
                </div>
              </div>
              {selectedMode === "STORM" && (
                <div className="flex flex-col gap-2">
                  <button
                    onClick={handlePlay}
                    disabled={loading}
                    className="w-full py-3 border border-primary text-primary font-label-caps text-label-caps transition-all duration-200 hover:bg-primary hover:text-on-primary hover:scale-[1.02] disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:scale-100 disabled:hover:bg-transparent disabled:hover:text-primary"
                  >
                    {loading ? "CONECTANDO..." : "INICIAR MISSÃO"}
                  </button>
                  <button
                    onClick={() => navigate("/room?mode=STORM")}
                    className="w-full py-3 border border-primary text-primary font-label-caps text-label-caps transition-all duration-200 hover:bg-primary hover:text-on-primary hover:scale-[1.02]"
                  >
                    SALA DE GUERRA
                  </button>
                </div>
              )}
            </div>

            {error && (
              <p className="font-mono-data text-mono-data text-error border-l-2 border-error pl-3 text-sm">
                {error}
              </p>
            )}
          </section>
        </main>
      </div>

      {/* Mobile: botões fixos no rodapé */}
      <div className="md:hidden fixed bottom-0 left-0 right-0 bg-surface-container-low border-t border-outline-variant p-4 flex gap-3 z-40">
        <button
          onClick={handlePlay}
          disabled={loading}
          className="flex-1 py-3 border border-primary text-primary font-label-caps text-label-caps radar-glow disabled:opacity-40 disabled:cursor-not-allowed"
        >
          {loading ? "CONECTANDO..." : "DEPLOY FLEET"}
        </button>
        <button
          onClick={handleLogout}
          className="py-3 px-4 text-on-surface-variant hover:text-error transition-colors"
        >
          <span className="material-symbols-outlined">power_settings_new</span>
        </button>
      </div>
    </div>
  );
};

export default LobbyPage;
