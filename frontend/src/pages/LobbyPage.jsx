import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import * as gameApi from '../api/gameApi';

const LobbyPage = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handlePlay = async () => {
    setError('');
    setLoading(true);
    try {
      const res = await gameApi.createOrJoinGame();
      navigate(`/game/${res.data.id}`);
    } catch (err) {
      const code = err.response?.data?.code;
      if (code === 'PLAYER_ALREADY_IN_GAME') {
        setError('Você já está em uma partida ativa.');
        // TODO: obter gameId do backend quando expor no erro 409, e oferecer botão para retomar
      } else {
        setError(err.response?.data?.message || 'Erro ao buscar partida');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div className="min-h-screen bg-slate-900 flex flex-col items-center justify-center px-4">
      <div className="bg-slate-800 rounded-lg shadow-lg p-8 w-full max-w-sm text-center">
        <h1 className="text-xl font-bold text-blue-400 mb-2">⚓ Batalha Naval</h1>
        <p className="text-slate-300 mb-6">
          Bem-vindo, <span className="text-white font-medium">{user?.username}</span>
        </p>

        {error && <p className="text-red-400 text-sm mb-4">{error}</p>}

        <button
          onClick={handlePlay}
          disabled={loading}
          className="w-full py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-slate-600 text-white font-medium rounded transition-colors mb-3"
        >
          {loading ? 'Buscando partida...' : 'Entrar em Partida'}
        </button>

        <button
          onClick={handleLogout}
          className="w-full py-2 text-slate-400 hover:text-white text-sm transition-colors"
        >
          Sair
        </button>
      </div>
    </div>
  );
};

export default LobbyPage;
