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

const SUSPEND_PRESETS = [
  { label: '1 hora', hours: 1 },
  { label: '6 horas', hours: 6 },
  { label: '24 horas', hours: 24 },
  { label: '3 dias', hours: 72 },
  { label: '7 dias', hours: 168 },
  { label: '30 dias', hours: 720 },
];

const UsersPage = () => {
  const [users, setUsers] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState(null);

  const [modal, setModal] = useState(null);
  const [suspendDate, setSuspendDate] = useState('');
  const [suspendPreset, setSuspendPreset] = useState(null);
  const [useCustomDate, setUseCustomDate] = useState(false);
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
    setSuspendPreset(null);
    setUseCustomDate(false);
  };

  const closeModal = () => {
    setModal(null);
    setSuspendDate('');
    setSuspendPreset(null);
    setUseCustomDate(false);
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
        let suspendedUntil;

        if (useCustomDate) {
          if (!suspendDate) {
            setToast({ message: 'Informe a data de suspensão', type: 'error' });
            setActionLoading(false);
            return;
          }
          suspendedUntil = new Date(suspendDate).toISOString();
        } else if (suspendPreset !== null) {
          const date = new Date();
          date.setHours(date.getHours() + suspendPreset);
          suspendedUntil = date.toISOString();
        } else {
          setToast({ message: 'Selecione a duração da suspensão', type: 'error' });
          setActionLoading(false);
          return;
        }

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
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50" onClick={closeModal}>
          <div
            className="bg-surface-container-high p-6 border border-outline-variant flex flex-col gap-5 w-full max-w-md rounded-xl shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Header */}
            <div className="flex items-center gap-3">
              <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${
                modal.type === 'ban' ? 'bg-error/15' :
                modal.type === 'suspend' ? 'bg-tertiary/15' :
                'bg-primary/15'
              }`}>
                <span className={`material-symbols-outlined text-xl ${
                  modal.type === 'ban' ? 'text-error' :
                  modal.type === 'suspend' ? 'text-tertiary' :
                  'text-primary'
                }`}>
                  {modal.type === 'ban' && 'block'}
                  {modal.type === 'suspend' && 'pause_circle'}
                  {modal.type === 'reactivate' && 'check_circle'}
                </span>
              </div>
              <div>
                <h3 className="text-base font-semibold text-on-surface">
                  {modal.type === 'ban' && 'Confirmar Banimento'}
                  {modal.type === 'suspend' && 'Suspender Usuário'}
                  {modal.type === 'reactivate' && 'Reativar Usuário'}
                </h3>
                <p className="text-xs text-on-surface-variant mt-0.5">
                  {modal.user.username} • {modal.user.email}
                </p>
              </div>
            </div>

            {/* Body */}
            <div className="text-sm text-on-surface-variant">
              {modal.type === 'ban' && (
                <div className="bg-error/5 border border-error/20 rounded-lg p-3">
                  <p>Tem certeza que deseja banir <strong className="text-on-surface">{modal.user.username}</strong>?</p>
                  <p className="text-xs mt-1 text-error">Esta ação bloqueia permanentemente o login do usuário.</p>
                </div>
              )}

              {modal.type === 'suspend' && (
                <div className="flex flex-col gap-4">
                  <p>Selecione a duração da suspensão:</p>

                  {/* Quick presets grid */}
                  <div className="grid grid-cols-3 gap-2">
                    {SUSPEND_PRESETS.map((preset) => (
                      <button
                        key={preset.hours}
                        onClick={() => {
                          setSuspendPreset(preset.hours);
                          setUseCustomDate(false);
                        }}
                        className={`px-3 py-2.5 text-xs font-medium rounded-lg border transition-all ${
                          suspendPreset === preset.hours && !useCustomDate
                            ? 'border-primary bg-primary/15 text-primary shadow-sm shadow-primary/20'
                            : 'border-outline-variant text-on-surface-variant hover:border-on-surface-variant hover:bg-surface-container'
                        }`}
                      >
                        {preset.label}
                      </button>
                    ))}
                  </div>

                  {/* Divider */}
                  <div className="flex items-center gap-3">
                    <div className="flex-1 h-px bg-outline-variant"></div>
                    <span className="text-xs text-on-surface-variant">ou</span>
                    <div className="flex-1 h-px bg-outline-variant"></div>
                  </div>

                  {/* Custom date toggle */}
                  <div>
                    <button
                      onClick={() => {
                        setUseCustomDate(!useCustomDate);
                        setSuspendPreset(null);
                      }}
                      className={`w-full flex items-center justify-between px-3 py-2.5 text-xs font-medium rounded-lg border transition-all ${
                        useCustomDate
                          ? 'border-primary bg-primary/15 text-primary'
                          : 'border-outline-variant text-on-surface-variant hover:border-on-surface-variant hover:bg-surface-container'
                      }`}
                    >
                      <span className="flex items-center gap-2">
                        <span className="material-symbols-outlined text-base">calendar_month</span>
                        Data e hora personalizada
                      </span>
                      <span className="material-symbols-outlined text-base">
                        {useCustomDate ? 'expand_less' : 'expand_more'}
                      </span>
                    </button>

                    {useCustomDate && (
                      <div className="mt-2">
                        <input
                          type="datetime-local"
                          value={suspendDate}
                          onChange={(e) => setSuspendDate(e.target.value)}
                          className="w-full bg-surface-container-lowest border border-outline-variant px-3 py-2.5 text-sm text-on-surface rounded-lg focus:border-primary focus:ring-1 focus:ring-primary/30 outline-none transition-all [color-scheme:dark]"
                          min={new Date().toISOString().slice(0, 16)}
                        />
                        <p className="text-xs text-on-surface-variant mt-1.5 pl-1">
                          Clique no campo acima para selecionar data e hora
                        </p>
                      </div>
                    )}
                  </div>

                  {/* Preview of selected duration */}
                  {(suspendPreset || (useCustomDate && suspendDate)) && (
                    <div className="bg-surface-container rounded-lg px-3 py-2 flex items-center gap-2">
                      <span className="material-symbols-outlined text-base text-on-surface-variant">schedule</span>
                      <span className="text-xs text-on-surface-variant">
                        Suspenso até:{' '}
                        <strong className="text-on-surface">
                          {suspendPreset && !useCustomDate
                            ? new Date(Date.now() + suspendPreset * 3600000).toLocaleString('pt-BR', {
                                day: '2-digit',
                                month: '2-digit',
                                year: 'numeric',
                                hour: '2-digit',
                                minute: '2-digit',
                              })
                            : suspendDate
                              ? new Date(suspendDate).toLocaleString('pt-BR', {
                                  day: '2-digit',
                                  month: '2-digit',
                                  year: 'numeric',
                                  hour: '2-digit',
                                  minute: '2-digit',
                                })
                              : ''}
                        </strong>
                      </span>
                    </div>
                  )}
                </div>
              )}

              {modal.type === 'reactivate' && (
                <div className="bg-primary/5 border border-primary/20 rounded-lg p-3">
                  <p>Reativar a conta de <strong className="text-on-surface">{modal.user.username}</strong>?</p>
                  <p className="text-xs mt-1 text-primary">O usuário poderá fazer login novamente.</p>
                </div>
              )}
            </div>

            {/* Actions */}
            <div className="flex gap-3 mt-1">
              <button
                onClick={closeModal}
                disabled={actionLoading}
                className="flex-1 py-2.5 border border-outline-variant text-on-surface-variant text-sm rounded-lg hover:bg-surface-container transition-colors disabled:opacity-50"
              >
                Cancelar
              </button>
              <button
                onClick={handleConfirmAction}
                disabled={actionLoading || (modal.type === 'suspend' && !suspendPreset && !(useCustomDate && suspendDate))}
                className={`flex-1 py-2.5 text-sm font-medium rounded-lg transition-all disabled:opacity-40 disabled:cursor-not-allowed ${
                  modal.type === 'reactivate'
                    ? 'bg-primary text-on-primary hover:bg-primary/90'
                    : 'bg-error text-on-error hover:bg-error/90'
                }`}
              >
                {actionLoading ? (
                  <span className="flex items-center justify-center gap-2">
                    <span className="w-3.5 h-3.5 border-2 border-current border-t-transparent rounded-full animate-spin"></span>
                    Processando...
                  </span>
                ) : (
                  <>
                    {modal.type === 'ban' && 'Banir Usuário'}
                    {modal.type === 'suspend' && 'Confirmar Suspensão'}
                    {modal.type === 'reactivate' && 'Reativar Conta'}
                  </>
                )}
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
