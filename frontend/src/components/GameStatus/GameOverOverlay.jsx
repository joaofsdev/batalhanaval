import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const GameOverOverlay = ({ isWinner, stats }) => {
  const navigate = useNavigate();
  const { logout } = useAuth();

  const displayStats = stats || { shots: '--', hits: '--', accuracy: '--' };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-background/80 backdrop-blur-sm">
      <div className={`relative p-8 border-2 bg-surface-container flex flex-col items-center gap-6 max-w-2xl w-full mx-4 ${
        isWinner
          ? 'border-tertiary-container shadow-[0_0_30px_rgba(255,177,59,0.3)]'
          : 'border-error shadow-[0_0_30px_rgba(239,68,68,0.2)]'
      }`}>
        {/* Corner accents */}
        <div className="corner-accent tl" style={{ borderColor: isWinner ? '#ffb13b' : '#ef4444' }} />
        <div className="corner-accent tr" style={{ borderColor: isWinner ? '#ffb13b' : '#ef4444' }} />
        <div className="corner-accent bl" style={{ borderColor: isWinner ? '#ffb13b' : '#ef4444' }} />
        <div className="corner-accent br" style={{ borderColor: isWinner ? '#ffb13b' : '#ef4444' }} />

        {/* Ícone */}
        <span className={`material-symbols-outlined text-6xl ${isWinner ? 'text-tertiary-container' : 'text-error'}`}>
          {isWinner ? 'emoji_events' : 'anchor'}
        </span>

        {/* Título */}
        <h2 className={`font-display-tactical text-display-tactical uppercase tracking-widest text-center ${
          isWinner ? 'text-tertiary-container' : 'text-error'
        }`}>
          {isWinner ? '[ MISSÃO CUMPRIDA ]' : '[ MISSÃO FRACASSADA ]'}
        </h2>

        {/* Stats */}
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

        {/* Botões */}
        <div className="flex gap-4 w-full">
          <button
            onClick={() => navigate('/lobby')}
            className="flex-1 py-3 bg-primary-container text-on-primary-fixed font-label-caps text-label-caps hover:bg-primary transition-all flex items-center justify-center gap-2 shadow-[0_0_10px_rgba(34,211,238,0.3)]"
          >
            <span className="material-symbols-outlined text-sm">refresh</span>
            NOVA MISSÃO
          </button>
          <button
            onClick={() => { logout(); navigate('/'); }}
            className="flex-1 py-3 border border-error text-error font-label-caps text-label-caps hover:bg-error/10 transition-all flex items-center justify-center gap-2"
          >
            <span className="material-symbols-outlined text-sm">logout</span>
            SAIR
          </button>
        </div>
      </div>
    </div>
  );
};

export default GameOverOverlay;
