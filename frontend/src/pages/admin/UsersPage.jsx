import { useState, useEffect, useCallback } from 'react';
import * as adminApi from '../../api/adminApi';
import Toast from '../../components/shared/Toast';

const STATUS_OPTIONS = [
  { value: '', label: 'Todos' },
  { value: 'ACTIVE', label: 'Ativos' },
  { value: 'SUSPENDED', label: 'Suspensos' },
  { value: 'BANNED', label: 'Banidos' },
];

const STATUS_BADGES = {
  ACTIVE: 'bg-primary/20 text-primary',
  SUSPENDED: 'bg-warning/20 text-warning',
  BANNED: 'bg-error/20 text-error',
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

const UsersPage = () => {
  const [users, setUsers] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState(null);

  const [modal, setModal] = useState(null);
  const [suspendDate, setSuspendDate] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    try {
      const res = await adminApi.getUsers(page, 20, statusFilter || undefined);
      setUsers(res.data.content);
      setTotalPages(res.data.totalPages);
    } catch (err) {
      setToast({ message: 'Erro ao carregar usuários', type: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page, statusFilter]);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  const handleFilterChange = (e) => {
    setStatusFilter(e.target.value);
    setPage(0);
  };

  const openModal = (type, user) => {
    setModal({ type, user });
    setSuspendDate('');
  };

  const closeModal = () => {
    setModal(null);
    setSuspendDate('');
  };

  const handleConfirmAction = async () => {
    if (!modal) return;
    setActionLoading(true);

    try {
      const { type, user } = modal;

      if (type === 'ban') {
        await adminApi.banUser(user.id);
        setToast({ message: `${user.username} foi banido`, type: 'success' });
      } else if (type === 'suspend') {
        if (!suspendDate) {
          setToast({ message: 'Informe a data de suspensão', type: 'error' });
          setActionLoading(false);
          return;
        }
        const suspendedUntil = new Date(suspendDate).toISOString();
        await adminApi.suspendUser(user.id, suspendedUntil);
        setToast({ message: `${user.username} foi suspenso`, type: 'success' });
      } else if (type === 'reactivate') {
        await adminApi.reactivateUser(user.id);
        setToast({ message: `${user.username} foi reativado`, type: 'success' });
      }

      closeModal();
      loadUsers();
    } catch (err) {
      const msg = err.response?.data?.message || 'Erro ao executar ação';
      setToast({ message: msg, type: 'error' });
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-bold">Usuários</h2>
        <select
          value={statusFilter}
          onChange={handleFilterChange}
          className="bg-surface-container border border-outline-variant px-3 py-1.5 text-sm text-on-surface rounded focus:border-primary outline-none"
        >
          {STATUS_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>{opt.label}</option>
          ))}
        </select>
      </div>

      <div className="overflow-x-auto border border-outline-variant rounded">
        <table className="w-full text-sm">
          <thead className="bg-surface-container-high text-on-surface-variant text-left">
            <tr>
              <th className="px-3 py-2 font-medium">Username</th>
              <th className="px-3 py-2 font-medium">Email</th>
              <th className="px-3 py-2 font-medium">Role</th>
              <th className="px-3 py-2 font-medium">Status</th>
              <th className="px-3 py-2 font-medium">Suspenso até</th>
              <th className="px-3 py-2 font-medium">Criado em</th>
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
            ) : users.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-3 py-8 text-center text-on-surface-variant">
                  Nenhum usuário encontrado
                </td>
              </tr>
            ) : (
              users.map((user) => (
                <tr key={user.id} className="hover:bg-surface-container-high/50">
                  <td className="px-3 py-2 font-mono-data text-mono-data">{user.username}</td>
                  <td className="px-3 py-2 text-on-surface-variant">{user.email}</td>
                  <td className="px-3 py-2">
                    <span className="text-xs uppercase">{user.role}</span>
                  </td>
                  <td className="px-3 py-2">
                    <span className={`text-xs px-2 py-0.5 rounded uppercase ${STATUS_BADGES[user.status] || ''}`}>
                      {user.status}
                    </span>
                  </td>
                  <td className="px-3 py-2 text-on-surface-variant text-xs">
                    {formatDate(user.suspendedUntil)}
                  </td>
                  <td className="px-3 py-2 text-on-surface-variant text-xs">
                    {formatDate(user.createdAt)}
                  </td>
                  <td className="px-3 py-2">
                    <div className="flex gap-1">
                      {user.status !== 'BANNED' && (
                        <button
                          onClick={() => openModal('ban', user)}
                          className="px-2 py-1 text-xs border border-error text-error rounded hover:bg-error/10 transition-colors"
                        >
                          Banir
                        </button>
                      )}
                      {user.status !== 'SUSPENDED' && user.status !== 'BANNED' && (
                        <button
                          onClick={() => openModal('suspend', user)}
                          className="px-2 py-1 text-xs border border-warning text-warning rounded hover:bg-warning/10 transition-colors"
                        >
                          Suspender
                        </button>
                      )}
                      {user.status !== 'ACTIVE' && (
                        <button
                          onClick={() => openModal('reactivate', user)}
                          className="px-2 py-1 text-xs border border-primary text-primary rounded hover:bg-primary/10 transition-colors"
                        >
                          Reativar
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

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

      {modal && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-surface-container-high p-6 border border-outline-variant flex flex-col gap-4 min-w-[320px] max-w-md rounded">
            <p className="font-label-caps text-label-caps text-on-surface uppercase">
              {modal.type === 'ban' && 'Confirmar Banimento'}
              {modal.type === 'suspend' && 'Suspender Usuário'}
              {modal.type === 'reactivate' && 'Reativar Usuário'}
            </p>

            <div className="text-sm text-on-surface-variant">
              {modal.type === 'ban' && (
                <p>Tem certeza que deseja banir <strong className="text-on-surface">{modal.user.username}</strong>? Essa ação bloqueia o login do usuário.</p>
              )}
              {modal.type === 'suspend' && (
                <>
                  <p className="mb-3">Suspender <strong className="text-on-surface">{modal.user.username}</strong> até:</p>
                  <input
                    type="datetime-local"
                    value={suspendDate}
                    onChange={(e) => setSuspendDate(e.target.value)}
                    className="w-full bg-surface-container-lowest border border-outline-variant px-3 py-2 text-sm text-on-surface rounded focus:border-primary outline-none"
                    min={new Date().toISOString().slice(0, 16)}
                  />
                </>
              )}
              {modal.type === 'reactivate' && (
                <p>Reativar a conta de <strong className="text-on-surface">{modal.user.username}</strong>? O usuário poderá fazer login novamente.</p>
              )}
            </div>

            <div className="flex gap-2 mt-2">
              <button
                onClick={closeModal}
                disabled={actionLoading}
                className="flex-1 py-2 border border-outline-variant text-on-surface-variant text-sm rounded hover:bg-surface-container transition-colors disabled:opacity-50"
              >
                Cancelar
              </button>
              <button
                onClick={handleConfirmAction}
                disabled={actionLoading}
                className={`flex-1 py-2 text-sm rounded transition-colors disabled:opacity-50 ${
                  modal.type === 'reactivate'
                    ? 'bg-primary text-on-primary hover:bg-primary/90'
                    : 'bg-error text-on-primary hover:bg-error/90'
                }`}
              >
                {actionLoading ? 'Processando...' : 'Confirmar'}
              </button>
            </div>
          </div>
        </div>
      )}

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

export default UsersPage;
