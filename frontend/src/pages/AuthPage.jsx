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
    <div className="min-h-screen bg-slate-900 flex items-center justify-center px-4">
      <div className="w-full max-w-sm bg-slate-800 rounded-lg shadow-lg p-8">
        <h1 className="text-2xl font-bold text-center text-blue-400 mb-6">
          ⚓ Batalha Naval
        </h1>

        <div className="flex mb-6">
          <button
            type="button"
            onClick={() => { setMode('login'); setError(''); }}
            className={`flex-1 py-2 text-sm font-medium border-b-2 ${mode === 'login' ? 'border-blue-500 text-blue-400' : 'border-slate-600 text-slate-400'}`}
          >
            Login
          </button>
          <button
            type="button"
            onClick={() => { setMode('register'); setError(''); }}
            className={`flex-1 py-2 text-sm font-medium border-b-2 ${mode === 'register' ? 'border-blue-500 text-blue-400' : 'border-slate-600 text-slate-400'}`}
          >
            Cadastro
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <input
            name="username"
            placeholder="Usuário"
            required
            value={form.username}
            onChange={handleChange}
            className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-white placeholder-slate-400 focus:outline-none focus:border-blue-500"
          />
          {mode === 'register' && (
            <input
              name="email"
              type="email"
              placeholder="Email"
              required
              value={form.email}
              onChange={handleChange}
              className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-white placeholder-slate-400 focus:outline-none focus:border-blue-500"
            />
          )}
          <input
            name="password"
            type="password"
            placeholder="Senha"
            required
            value={form.password}
            onChange={handleChange}
            className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-white placeholder-slate-400 focus:outline-none focus:border-blue-500"
          />

          {error && (
            <p className="text-red-400 text-sm text-center">{error}</p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-slate-600 text-white font-medium rounded transition-colors"
          >
            {loading ? 'Carregando...' : mode === 'login' ? 'Entrar' : 'Cadastrar'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default AuthPage;
