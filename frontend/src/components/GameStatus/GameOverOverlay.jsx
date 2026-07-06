import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import * as gameApi from '../../api/gameApi';

const GameOverOverlay = ({ isWinner, isCancelled, stats, gameId, rematchInvite }) => {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [rematchLoading, setRematchLoading] = useState(false);
  const [rematchSent, setRematchSent] = useState(false);

  const displayStats = stats || { shots: '--', hits: '--', accuracy: '--' };

  const handleRematch = async () => {
    setRematchLoading(true);
    try {
      const res = await gameApi.requestRematch(gameId);
      setRematchSent(true);
      // Navigate to the new game
      navigate(`/game/${res.data.id}`);
    } catch (err) {
      setRematchLoading(false);
    }
  };

  const handleAcceptRematch = async () => {
    if (rematchInvite?.gameId) {
      try {
        // Join the rematch game via normal matchmaking (will find the WAITING game)
        const res = await gameApi.createOrJoinGame();
        navigate(`/game/${res.data.id}`);
      } catch (err) {
        // Fallback: navigate directly
        navigate(`/game/${rematchInvite.gameId}`);
      }
    }
  };

  const borderColor = isCancelled ? '#94a3b8' : isWinner ? '#ffb13b' : '#ef4444';
  const borderClass = isCancelled
    ? 'border-outline-variant shadow-[0_0_30px_rgba(148,163,184,0.2)]'
    : isWinner
      ? 'border-tertiary-container shadow-[0_0_30px_rgba(255,177,59,0.3)]'
      : 'border-error shadow-[0_0_30px_rgba(239,68,68,0.2)]';
  const iconClass = isCancelled ? 'text-outline-variant' : isWinner ? 'text-tertiary-container' : 'text-error';
  const icon = isCancelled ? 'block' : isWinner ? 'emoji_events' : 'anchor';
  const title = isCancelled ? '[ PARTIDA CANCELADA ]' : isWinner ? '[ MISSÃO CUMPRIDA ]' : '[ MISSÃO FRACASSADA ]';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-background/80 backdrop-blur-sm">
      <div className={`relative p-8 border-2 bg-surface-container flex flex-col items-center gap-6 max-w-2xl w-full mx-4 ${borderClass}`}>
        {/* Corner accents */}
        <div className="corner-accent tl" style={{ borderColor }} />
        <div className="corner-accent tr" style={{ borderColor }} />
        <div className="corner-accent bl" style={{ borderColor }} />
        <div className="corner-accent br" style={{ borderColor }} />

        {/* Ícone */}
        <span className={`material-symbols-outlined text-6xl ${iconClass}`}>
          {icon}
        </span>

        {/* Título */}
        <h2 className={`font-display-tactical text-display-tactical uppercase tracking-widest text-center whitespace-nowrap ${iconClass}`} style={{ fontSize: 'clamp(1rem, 3vw, 1.5rem)' }}>
          {title}
        </h2>

        {/* Stats */}
        {!isCancelled && (
        <div className="grid grid-cols-3 gap-4 w-full">
          {[
            { label: 'TIROS', value: displayStats.shots },
            { label: 'ACERTOS', value: displayStats.hits },
            { label: 'PRECISÃO', value: displayStats.accuracy !== '--' ? `${displayStats.accuracy}%` : '--' },
          ].map(stat => (
            <div key={stat.label} className="bg-surface-container-high p-4 flex flex-col items-center gap-2 border border-outline-variant">
              <span className="font-label-caps text-label-caps text-on-surface-variant">{stat.label}</span>
              <span className={`font-display-tactical text-headline-lg ${isWinner ? 'text-primary' : 'text-on-surface'}`}>
                {stat.value}
              </span>
            </div>
          ))}
        </div>
        )}

        {/* Cancelled explanation */}
        {isCancelled && (
          <p className="font-mono-data text-mono-data text-on-surface-variant text-center">
            A partida foi encerrada por inatividade durante o posicionamento.
          </p>
        )}

        {/* Rematch invite received */}
        {!isCancelled && rematchInvite && (
          <div className="w-full p-4 border border-tertiary-container bg-tertiary-container/10 flex items-center justify-between">
            <span className="font-mono-data text-mono-data text-tertiary">
              🔄 {rematchInvite.opponentUsername?.toUpperCase()} QUER REVANCHE!
            </span>
            <button
              onClick={handleAcceptRematch}
              className="px-4 py-2 bg-tertiary-container text-on-tertiary-fixed font-label-caps text-label-caps hover:bg-tertiary transition-all"
            >
              ACEITAR
            </button>
          </div>
        )}

        {/* Botões */}
        <div className="flex gap-3 w-full">
          {!isCancelled && (
          <button
            onClick={handleRematch}
            disabled={rematchLoading || rematchSent}
            className="flex-1 py-3 border border-tertiary-container text-tertiary-container font-label-caps text-label-caps hover:bg-tertiary-container/10 transition-all flex items-center justify-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed"
          >
            <span className="material-symbols-outlined text-sm">replay</span>
            {rematchSent ? 'CONVITE ENVIADO' : rematchLoading ? 'ENVIANDO...' : 'REVANCHE'}
          </button>
          )}
          <button
            onClick={() => navigate('/lobby')}
            className="flex-1 py-3 bg-primary-container text-on-primary-fixed font-label-caps text-label-caps hover:bg-primary transition-all flex items-center justify-center gap-2 shadow-[0_0_10px_rgba(34,211,238,0.3)]"
          >
            <span className="material-symbols-outlined text-sm">refresh</span>
            NOVA MISSÃO
          </button>
          <button
            onClick={() => { logout(); navigate('/'); }}
            className="py-3 px-4 border border-error text-error font-label-caps text-label-caps hover:bg-error/10 transition-all flex items-center justify-center"
          >
            <span className="material-symbols-outlined text-sm">logout</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default GameOverOverlay;
