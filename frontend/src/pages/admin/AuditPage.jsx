import { useState, useEffect, useCallback } from 'react';
import * as adminApi from '../../api/adminApi';
import Toast from '../../components/shared/Toast';

const ACTION_LABELS = {
  USER_BANNED: 'Usuário banido',
  USER_SUSPENDED: 'Usuário suspenso',
  USER_REACTIVATED: 'Usuário reativado',
  GAME_FORCE_ENDED: 'Partida encerrada',
  SUSPENSION_EXPIRED: 'Suspensão expirada',
};

const ACTION_COLORS = {
  USER_BANNED: 'text-error',
  USER_SUSPENDED: 'text-warning',
  USER_REACTIVATED: 'text-primary',
  GAME_FORCE_ENDED: 'text-error',
  SUSPENSION_EXPIRED: 'text-on-surface-variant',
};

const TARGET_LABELS = {
  USER: 'Usuário',
  GAME: 'Partida',
};

const formatDate = (isoString) => {
  if (!isoString) return '—';
  return new Date(isoString).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
};

const truncateId = (uuid) => uuid?.slice(0, 8) || '—';

const AuditPage = () => {
  const [logs, setLogs] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState(null);

  const loadLogs = useCallback(async () => {
    setLoading(true);
    try {
      const res = await adminApi.getAuditLog(page, 20);
      setLogs(res.data.content);
      setTotalPages(res.data.totalPages);
    } catch (err) {
      setToast({ message: 'Erro ao carregar logs de auditoria', type: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    loadLogs();
  }, [loadLogs]);

  return (
    <div>
      <h2 className="text-xl font-bold mb-4">Auditoria</h2>

      {/* Table */}
      {!loading && logs.length === 0 ? (
        <div className="border border-outline-variant rounded p-8 text-center text-on-surface-variant">
          <span className="material-symbols-outlined text-3xl mb-2 block">history</span>
          <p>Nenhum registro de auditoria</p>
        </div>
      ) : (
        <div className="overflow-x-auto border border-outline-variant rounded">
          <table className="w-full text-sm">
            <thead className="bg-surface-container-high text-on-surface-variant text-left">
              <tr>
                <th className="px-3 py-2 font-medium">Admin</th>
                <th className="px-3 py-2 font-medium">Ação</th>
                <th className="px-3 py-2 font-medium">Alvo</th>
                <th className="px-3 py-2 font-medium">Detalhes</th>
                <th className="px-3 py-2 font-medium">Data/Hora</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant">
              {loading ? (
                <tr>
                  <td colSpan={5} className="px-3 py-8 text-center text-on-surface-variant">
                    Carregando...
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log.id} className="hover:bg-surface-container-high/50">
                    <td className="px-3 py-2 font-mono-data text-mono-data">
                      {log.adminUsername === 'SYSTEM' ? (
                        <span className="text-on-surface-variant italic">SYSTEM</span>
                      ) : (
                        log.adminUsername
                      )}
                    </td>
                    <td className="px-3 py-2">
                      <span className={`text-xs font-medium ${ACTION_COLORS[log.action] || 'text-on-surface'}`}>
                        {ACTION_LABELS[log.action] || log.action}
                      </span>
                    </td>
                    <td className="px-3 py-2 text-xs">
                      <span className="text-on-surface-variant">{TARGET_LABELS[log.targetType] || log.targetType}</span>
                      {' '}
                      <span className="font-mono-data">{truncateId(log.targetId)}</span>
                    </td>
                    <td className="px-3 py-2 text-xs text-on-surface-variant max-w-[200px] truncate">
                      {log.details || '—'}
                    </td>
                    <td className="px-3 py-2 text-on-surface-variant text-xs whitespace-nowrap">
                      {formatDate(log.createdAt)}
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

export default AuditPage;
