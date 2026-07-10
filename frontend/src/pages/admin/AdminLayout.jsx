import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const navItems = [
  { to: '/admin/users', label: 'Usuários', icon: 'group' },
  { to: '/admin/games', label: 'Partidas', icon: 'sports_esports' },
  { to: '/admin/audit', label: 'Auditoria', icon: 'history' },
];

const AdminLayout = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div className="flex h-screen bg-surface text-on-surface">
      <aside className="w-56 bg-surface-container flex flex-col border-r border-outline-variant">
        <div className="p-4 border-b border-outline-variant">
          <h1 className="text-sm font-mono-data uppercase tracking-wider text-primary">
            Admin Panel
          </h1>
          <p className="text-xs text-on-surface-variant mt-1 truncate">
            {user?.username}
          </p>
        </div>

        <nav className="flex-1 p-2 space-y-1">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2 rounded text-sm transition-colors ${
                  isActive
                    ? 'bg-primary/10 text-primary'
                    : 'text-on-surface-variant hover:bg-surface-container-high'
                }`
              }
            >
              <span className="material-symbols-outlined text-lg">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="p-2 border-t border-outline-variant space-y-1">
          <button
            onClick={() => navigate('/lobby')}
            className="flex items-center gap-3 px-3 py-2 rounded text-sm text-on-surface-variant hover:bg-surface-container-high w-full"
          >
            <span className="material-symbols-outlined text-lg">arrow_back</span>
            Voltar ao jogo
          </button>
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 px-3 py-2 rounded text-sm text-error hover:bg-error/10 w-full"
          >
            <span className="material-symbols-outlined text-lg">logout</span>
            Sair
          </button>
        </div>
      </aside>

      <main className="flex-1 overflow-y-auto p-6">
        <Outlet />
      </main>
    </div>
  );
};

export default AdminLayout;
