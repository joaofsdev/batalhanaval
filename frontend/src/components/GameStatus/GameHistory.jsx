import { useState, useEffect } from 'react';
import * as gameApi from '../../api/gameApi';

const GameHistory = () => {
  const [history, setHistory] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    gameApi.getGameHistory(page, 5)
      .then(({ data }) => {
        setHistory(data.content || []);
        setTotalPages(data.totalPages || 0);
      })
      .catch(() => setHistory([]))
      .finally(() => setLoading(false));
  }, [page]);

  const formatDuration = (seconds) => {
    if (seconds < 60) return `${seconds}s`;
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}m ${secs}s`;
  };

  const formatDate = (isoDate) => {
    const d = new Date(isoDate);
    return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: '2-digit' }) +
      ' ' + d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <section className="w-full bg-surface-container border border-outline-variant p-panel-padding flex flex-col gap-4">
      <header className="border-b border-outline-variant pb-2 flex items-center gap-2">
        <span className="material-symbols-outlined text-primary text-sm">history</span>
        <h2 className="font-headline-md text-headline-md text-primary tracking-widest">
          HISTÓRICO
        </h2>
      </header>

      {loading ? (
        <p className="font-mono-data text-mono-data text-primary animate-pulse text-center py-4">
          CARREGANDO...
        </p>
      ) : history.length === 0 ? (
        <p className="font-mono-data text-mono-data text-on-surface-variant text-center py-4">
          NENHUMA PARTIDA CONCLUÍDA
        </p>
      ) : (
        <>
          <div className="flex flex-col gap-2">
            {history.map((entry) => (
              <div
                key={entry.id}
                className={`flex items-center justify-between p-3 border transition-colors ${
                  entry.won
                    ? 'border-primary/30 bg-primary-container/5'
                    : 'border-error/30 bg-error/5'
                }`}
              >
                <div className="flex items-center gap-3">
                  <span className={`material-symbols-outlined text-sm ${entry.won ? 'text-primary' : 'text-error'}`}>
                    {entry.won ? 'emoji_events' : 'close'}
                  </span>
                  <div>
                    <p className="font-mono-data text-mono-data text-on-surface">
                      vs {entry.opponentUsername?.toUpperCase()}
                    </p>
                    <p className="font-label-caps text-[10px] text-on-surface-variant">
                      {formatDate(entry.playedAt)}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  <span className="font-mono-data text-mono-data text-on-surface-variant">
                    {formatDuration(entry.durationSeconds)}
                  </span>
                  <span className={`font-label-caps text-label-caps px-2 py-0.5 border ${
                    entry.won
                      ? 'text-primary border-primary'
                      : 'text-error border-error'
                  }`}>
                    {entry.won ? 'VITÓRIA' : 'DERROTA'}
                  </span>
                </div>
              </div>
            ))}
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-4 pt-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="font-label-caps text-label-caps text-primary disabled:text-on-surface-variant disabled:cursor-not-allowed"
              >
                ← ANT.
              </button>
              <span className="font-mono-data text-mono-data text-on-surface-variant">
                {page + 1} / {totalPages}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="font-label-caps text-label-caps text-primary disabled:text-on-surface-variant disabled:cursor-not-allowed"
              >
                PRÓX. →
              </button>
            </div>
          )}
        </>
      )}
    </section>
  );
};

export default GameHistory;
