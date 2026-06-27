import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import * as gameApi from "../api/gameApi";

const LobbyPage = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [ranking, setRanking] = useState([]);
  const [myPosition, setMyPosition] = useState(null);

  useEffect(() => {
    gameApi.getRanking()
      .then(({ data }) => {
        setRanking(data.ranking || []);
        setMyPosition(data.myPosition || null);
      })
      .catch(() => {});
  }, []);

  const handlePlay = async () => {
    setError("");
    setLoading(true);
    try {
      const res = await gameApi.createOrJoinGame();
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

        <div className="flex items-center gap-3">
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
          {/* Painel esquerdo — Ranking Global */}
          <section className="w-full md:w-1/2 bg-surface-container border border-outline-variant p-panel-padding flex flex-col gap-4">
            <header className="border-b border-outline-variant pb-2 flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-sm">
                leaderboard
              </span>
              <h2 className="font-headline-md text-headline-md text-primary tracking-widest">
                RANKING GLOBAL
              </h2>
            </header>

            <div className="grid grid-cols-4 gap-2 px-2 py-1 border-b border-outline-variant">
              {["#", "OPERADOR", "VITÓRIAS", "PARTIDAS"].map((col) => (
                <span
                  key={col}
                  className="font-label-caps text-label-caps text-on-surface-variant"
                >
                  {col}
                </span>
              ))}
            </div>

            {/* Dados do ranking */}
            {ranking.map((row) => (
              <div
                key={row.userId}
                className="grid grid-cols-4 gap-2 px-2 py-2 border-b border-outline-variant/30 hover:bg-surface-container-high transition-colors"
              >
                <span className="font-mono-data text-mono-data text-primary-container">
                  {String(row.position).padStart(2, '0')}
                </span>
                <span className="font-mono-data text-mono-data text-on-surface">
                  {row.username?.toUpperCase()}
                </span>
                <span className="font-mono-data text-mono-data text-primary">
                  {row.wins}
                </span>
                <span className="font-mono-data text-mono-data text-on-surface-variant">
                  {row.totalGames}
                </span>
              </div>
            ))}

            {/* Linha do usuário logado */}
            {myPosition && (
              <div className="grid grid-cols-4 gap-2 px-2 py-2 bg-secondary-container/30 border border-primary/30">
                <span className="font-mono-data text-mono-data text-primary-container">
                  {String(myPosition.position).padStart(2, '0')}
                </span>
                <span className="font-mono-data text-mono-data text-primary">
                  {myPosition.username?.toUpperCase()} (VOCÊ)
                </span>
                <span className="font-mono-data text-mono-data text-primary">
                  {myPosition.wins}
                </span>
                <span className="font-mono-data text-mono-data text-on-surface-variant">
                  {myPosition.totalGames}
                </span>
              </div>
            )}
          </section>

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
            <div className="flex-1 border border-outline-variant p-panel-padding flex flex-col gap-4 hover:border-primary transition-colors hover:bg-primary-container/5">
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
              <button
                onClick={handlePlay}
                disabled={loading}
                className="w-full py-3 bg-surface-container-high border border-primary text-primary font-label-caps text-label-caps hover:bg-primary-container hover:text-on-primary-fixed transition-all radar-glow disabled:opacity-40 disabled:cursor-not-allowed"
              >
                {loading ? "CONECTANDO..." : "INICIAR MISSÃO"}
              </button>
            </div>

            {/* Card Randômico — DESABILITADO */}
            <div className="flex-1 border border-outline-variant/40 p-panel-padding flex flex-col gap-4 opacity-50 cursor-not-allowed">
              <div className="flex items-start gap-4">
                <div className="w-16 h-16 border border-outline-variant/40 flex items-center justify-center text-on-surface-variant bg-surface-container-high">
                  <span className="material-symbols-outlined text-3xl">
                    shuffle
                  </span>
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <h3 className="font-headline-md text-headline-md text-on-surface-variant">
                      RANDÔMICO
                    </h3>
                    <span className="font-label-caps text-label-caps text-tertiary-container border border-tertiary-container px-2 py-0.5 text-[10px]">
                      EM BREVE
                    </span>
                  </div>
                  <p className="font-body-md text-body-md text-on-surface-variant">
                    Setores desconhecidos. Anomalias no radar. Condições
                    climáticas extremas. Para operadores avançados.
                  </p>
                </div>
              </div>
              <button
                disabled
                className="w-full py-3 bg-surface-container-low border border-outline-variant/40 text-on-surface-variant font-label-caps text-label-caps cursor-not-allowed"
              >
                EM DESENVOLVIMENTO
              </button>
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
