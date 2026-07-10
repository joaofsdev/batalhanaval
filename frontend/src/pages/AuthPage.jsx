import { useState } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import * as authApi from '../api/authApi';

const AuthPage = () => {
  const { token, login } = useAuth();
  const navigate = useNavigate();
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ username: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  if (token) return <Navigate to="/lobby" replace />;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (mode === 'register') {
      const emailRegex = /^[\w.+-]+@[\w-]+\.[a-zA-Z]{2,}$/;
      if (!emailRegex.test(form.email)) {
        setError('Email inválido. Use um email real (ex: usuario@dominio.com)');
        return;
      }
      if (form.password.length < 6) {
        setError('A senha deve ter no mínimo 6 caracteres');
        return;
      }
      if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]/.test(form.password)) {
        setError('A senha deve conter pelo menos 1 símbolo (ex: @, !, #, $)');
        return;
      }
    }

    setLoading(true);

    try {
      const payload =
        mode === 'login'
          ? { username: form.username, password: form.password }
          : form;
      const res =
        mode === 'login'
          ? await authApi.login(payload)
          : await authApi.register(payload);
      login(res.data);
      navigate('/lobby');
    } catch (err) {
      setError(err.response?.data?.message || 'Erro ao conectar com o servidor');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  return (
    <div className="min-h-screen flex flex-row">
      <div className="hidden md:flex md:w-3/5 h-screen relative radar-grid items-center justify-center bg-surface-container-lowest overflow-hidden">
        <div
          className="absolute inset-0 z-0"
          style={{
            backgroundImage: "url('/src/assets/background-image.png')",
            backgroundSize: 'cover',
            backgroundPosition: 'center',
            opacity: 0.5,
          }}
        />

        <div
          className="absolute inset-0 z-[1]"
          style={{
            background: 'linear-gradient(135deg, rgba(8, 28, 36, 0.75) 0%, rgba(0, 54, 62, 0.6) 100%)',
            mixBlendMode: 'multiply',
          }}
        />

        <div className="absolute inset-0 z-10 w-full h-full pointer-events-none opacity-20 bg-[conic-gradient(from_90deg_at_50%_50%,transparent_0%,rgba(34,211,238,1)_100%)] animate-[spin_4s_linear_infinite] rounded-full scale-150" />

        <div className="absolute top-4 left-4 z-40 font-mono-data text-mono-data text-primary-container/70">
          LAT: 45.923 N<br/>LON: 14.882 W
        </div>

        <div className="absolute w-96 h-96 border border-primary-container/40 rounded-full flex items-center justify-center z-20">
          <div className="absolute w-full h-full border border-primary-container rounded-full sonar-pulse" />
          <div className="absolute w-2/3 h-2/3 border border-primary-container rounded-full sonar-pulse" style={{ animationDelay: '1s' }} />
          <div className="absolute w-1/3 h-1/3 border border-primary-container rounded-full sonar-pulse" style={{ animationDelay: '2s' }} />
          <div className="w-4 h-4 bg-primary-container rounded-full animate-pulse shadow-[0_0_15px_rgba(34,211,238,1)]" />
        </div>

        <div className="absolute inset-0 border border-primary-container/20 m-8 z-30 pointer-events-none" />
      </div>

      <div className="w-full md:w-2/5 min-h-screen bg-surface-container-low flex flex-col justify-center px-8 md:px-16 lg:px-24 border-l border-outline-variant relative z-40">
        <div className="corner-accent tl" /><div className="corner-accent tr" />
        <div className="corner-accent bl" /><div className="corner-accent br" />

        <div className="max-w-md w-full mx-auto">
          <div className="mb-12 text-center">
            <h1 className="font-display-tactical text-display-tactical text-primary mb-2 glow-text uppercase tracking-widest">
              BATALHA NAVAL
            </h1>
            <p className="font-mono-data text-mono-data text-primary-fixed-dim uppercase tracking-widest">
              SISTEMA DE COMBATE NAVAL v1.0
            </p>
          </div>

          <div className="flex mb-8 border-b border-outline-variant">
            <button
              type="button"
              onClick={() => { setMode('login'); setError(''); }}
              className={`flex-1 py-3 font-label-caps text-label-caps transition-colors ${mode === 'login' ? 'text-primary border-t-2 border-primary bg-primary-container/10' : 'text-on-surface-variant hover:text-primary'}`}
            >LOGIN</button>
            <button
              type="button"
              onClick={() => { setMode('register'); setError(''); }}
              className={`flex-1 py-3 font-label-caps text-label-caps transition-colors ${mode === 'register' ? 'text-primary border-t-2 border-primary bg-primary-container/10' : 'text-on-surface-variant hover:text-primary'}`}
            >REGISTRO</button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="block font-label-caps text-label-caps text-primary-fixed-dim mb-2">
                IDENTIFICAÇÃO (USUÁRIO)
              </label>
              <div className="relative">
                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-primary-fixed-dim">person</span>
                <input
                  name="username"
                  required
                  value={form.username}
                  onChange={handleChange}
                  placeholder="CMD_JOHN_DOE"
                  className="w-full bg-[#1e293b] border-0 border-b border-primary-fixed-dim/50 text-on-surface font-mono-data text-mono-data pl-10 py-3 focus:ring-0 focus:border-primary-container focus:bg-surface-container-highest transition-all duration-200 outline-none placeholder-primary-fixed-dim/60 focus:shadow-[0_0_8px_rgba(34,211,238,0.3)]"
                />
              </div>
            </div>

            {mode === 'register' && (
              <div>
                <label className="block font-label-caps text-label-caps text-primary-fixed-dim mb-2">
                  EMAIL
                </label>
                <div className="relative">
                  <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-primary-fixed-dim">mail</span>
                  <input
                    name="email"
                    type="email"
                    required
                    value={form.email}
                    onChange={handleChange}
                    placeholder="operador@aegis.mil"
                    className="w-full bg-[#1e293b] border-0 border-b border-primary-fixed-dim/50 text-on-surface font-mono-data text-mono-data pl-10 py-3 focus:ring-0 focus:border-primary-container focus:bg-surface-container-highest transition-all duration-200 outline-none placeholder-primary-fixed-dim/60 focus:shadow-[0_0_8px_rgba(34,211,238,0.3)]"
                  />
                </div>
              </div>
            )}

            <div>
              <label className="block font-label-caps text-label-caps text-primary-fixed-dim mb-2">
                SENHA
              </label>
              <div className="relative">
                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-primary-fixed-dim">lock</span>
                <input
                  name="password"
                  type="password"
                  required
                  value={form.password}
                  onChange={handleChange}
                  placeholder="••••••••"
                  className="w-full bg-[#1e293b] border-0 border-b border-primary-fixed-dim/50 text-on-surface font-mono-data text-mono-data pl-10 py-3 focus:ring-0 focus:border-primary-container focus:bg-surface-container-highest transition-all duration-200 outline-none placeholder-primary-fixed-dim/60 focus:shadow-[0_0_8px_rgba(34,211,238,0.3)]"
                />
              </div>
            </div>

            {error && (
              <p className="font-mono-data text-mono-data text-error border-l-2 border-error pl-3">
                {error}
              </p>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-primary-container text-on-primary-fixed font-label-caps text-label-caps py-4 rounded hover:bg-primary transition-all duration-200 shadow-[0_0_10px_rgba(34,211,238,0.2)] hover:shadow-[0_0_20px_rgba(34,211,238,0.6)] flex items-center justify-center gap-2 mt-8 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              <span className="material-symbols-outlined text-[18px]">login</span>
              {loading ? 'AUTENTICANDO...' : 'AUTENTICAR'}
            </button>
          </form>

          <div className="mt-8 text-center">
            <span className="font-mono-data text-mono-data text-on-surface-variant">
              {mode === 'login' ? 'Não tem conta? ' : 'Já tem conta? '}
              <button
                type="button"
                onClick={() => setMode(mode === 'login' ? 'register' : 'login')}
                className="text-primary border-b border-primary/30 pb-1"
              >
                {mode === 'login' ? 'REGISTRE-SE' : 'FAÇA LOGIN'}
              </button>
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AuthPage;
