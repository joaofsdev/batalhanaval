import { useState, useEffect, useCallback } from 'react';
import * as adminApi from '../../api/adminApi';
import Toast from '../../components/shared/Toast';

const STATUS_LABELS = {
  WAITING: 'Aguardando',
  PLACING: 'Posicionando',
  IN_PROGRESS: 'Em andamento',
};

const STATUS_BADGES = {
  WAITING: 'bg-secondary/20 text-secondary',
  PLACING: 'bg-warning/20 text-warning',
  IN_PROGRESS: 'bg-primary/20 text-primary',
};

const formatDate = (isoString) => {
  if (!isoString) return '—';
  return new Date(isoString).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const truncateId = (uuid) => uuid?.slice(0, 8) || '—';

const GamesPage = () => {
  const [games, setGames] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState(null);

  // Modal state
  const [selectedGame, setSelectedGame] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);

  const loadGames = useCallback(async () => {
    setLoading(true);
    try {
      const res = await adminApi.getActiveGames(page, 20);
      setGames(res.data.content);
      setTotalPages(res.data.totalPages);
    } catch (err) {
      setToast({ message: 'Erro ao carregar partidas', type: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    loadGames();
  }, [loadGames]);

  const handleForceEnd = async () => {
    if (!selectedGame) return;
    setActionLoading(true);

    try {
      await adminApi.forceEndGame(selectedGame.id);
      setToast({ message: 'Partida encerrada com sucesso', type: 'success' });
      setSelectedGame(null);
      loadGames();
    } catch (err) {
      const msg = err.response?.data?.message || 'Erro ao encerrar partida';
      setToast({ message: msg, type: 'error' });
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div>
      <h2 className="text-xl font-bold mb-4">Partidas Ativas</h2>

      {/* Table */}
      {!loading && games.length === 0 ? (
        <div className="border border-outline-variant rounded p-8 text-center text-on-surface-variant">
          <span className="material-symbols-outlined text-3xl mb-2 block">sports_esports</span>
          <p>Nenhuma partida em andamento</p>
        </div>
      ) : (
        <div className="overflow-x-auto border border-outline-variant rounded">
          <table className="w-full text-sm">
            <thead className="bg-surface-container-high text-on-surface-variant text-left">
              <tr>
                <th className="px-3 py-2 font-medium">ID</th>
                <th className="px-3 py-2 font-medium">Jogador 1</th>
                <th className="px-3 py-2 font-medium">Jogador 2</th>
                <th className="px-3 py-2 font-medium">Modo</th>
                <th className="px-3 py-2 font-medium">Status</th>
                <th className="px-3 py-2 font-medium">Início</th>
                <th className="px-3 py-2 font-medium">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant">
              {loading ? (
                <tr>
                  <td colSpan={7} className="px-3 py-8 text-center text-on-surface-variant">
                    Carregando...
                  </td>
                </tr>
              ) : (
                games.map((game) => (
                  <tr key={game.id} className="hover:bg-surface-container-high/50">
                    <td className="px-3 py-2 font-mono-data text-mono-data text-xs">
                      {truncateId(game.id)}
                    </td>
                    <td className="px-3 py-2 font-mono-data text-mono-data">
                      {game.player1?.username || '—'}
                    </td>
                    <td className="px-3 py-2 font-mono-data text-mono-data">
                      {game.player2?.username || '(aguardando)'}
                    </td>
                    <td className="px-3 py-2 text-xs uppercase text-on-surface-variant">
                      {game.gameMode}
                    </td>
                    <td className="px-3 py-2">
                      <span className={`text-xs px-2 py-0.5 rounded ${STATUS_BADGES[game.status] || ''}`}>
                        {STATUS_LABELS[game.status] || game.status}
                      </span>
                    </td>
                    <td className="px-3 py-2 text-on-surface-variant text-xs">
                      {formatDate(game.createdAt)}
                    </td>
                    <td className="px-3 py-2">
                      <button
                        onClick={() => setSelectedGame(game)}
                        className="px-2 py-1 text-xs border border-error text-error rounded hover:bg-error/10 transition-colors"
                      >
                        Encerrar
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 mt-4">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="px-3 py-1 text-sm border border-outline-variant rounded disabled:opacity-30 hover:bg-surface-container-high transition-colors"
          >
            Anterior
          </button>
          <span className="text-sm text-on-surface-variant">
            {page + 1} / {totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="px-3 py-1 text-sm border border-outline-variant rounded disabled:opacity-30 hover:bg-surface-container-high transition-colors"
          >
            Próxima
          </button>
        </div>
      )}

      {/* Modal de confirmação */}
      {selectedGame && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-surface-container-high p-6 border border-outline-variant flex flex-col gap-4 min-w-[320px] max-w-md rounded">
            <p className="font-label-caps text-label-caps text-on-surface uppercase">
              Forçar encerramento
            </p>

            <div className="text-sm text-on-surface-variant">
              <p>
                Tem certeza que deseja encerrar essa partida? Ela será anulada (sem vencedor) e ambos os jogadores serão notificados.
              </p>
              <div className="mt-3 p-2 bg-surface-container rounded text-xs space-y-1">
                <p><span className="text-on-surface-variant">ID:</span> <span className="font-mono-data">{truncateId(selectedGame.id)}</span></p>
                <p><span className="text-on-surface-variant">Jogadores:</span> {selectedGame.player1?.username} vs {selectedGame.player2?.username || '(aguardando)'}</p>
                <p><span className="text-on-surface-variant">Modo:</span> {selectedGame.gameMode}</p>
              </div>
            </div>

            <div className="flex gap-2 mt-2">
              <button
                onClick={() => setSelectedGame(null)}
                disabled={actionLoading}
                className="flex-1 py-2 border border-outline-variant text-on-surface-variant text-sm rounded hover:bg-surface-container transition-colors disabled:opacity-50"
              >
                Cancelar
              </button>
              <button
                onClick={handleForceEnd}
                disabled={actionLoading}
                className="flex-1 py-2 bg-error text-on-primary text-sm rounded hover:bg-error/90 transition-colors disabled:opacity-50"
              >
                {actionLoading ? 'Processando...' : 'Encerrar partida'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Toast */}
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}
    </div>
  );
};

export default GamesPage;
