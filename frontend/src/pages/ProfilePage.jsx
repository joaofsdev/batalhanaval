import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import * as userApi from '../api/userApi';

const ProfilePage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    setLoading(true);
    const request = user?.id === id ? userApi.getMyProfile() : userApi.getProfile(id);
    request
      .then(({ data }) => setProfile(data))
      .catch((err) => setError(err.response?.data?.message || 'Erro ao carregar perfil'))
      .finally(() => setLoading(false));
  }, [id, user?.id]);

  const formatDate = (isoDate) => {
    if (!isoDate) return '--';
    return new Date(isoDate).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
  };

  const formatDuration = (seconds) => {
    if (seconds < 60) return `${seconds}s`;
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}m ${secs}s`;
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <p className="font-mono-data text-mono-data text-primary animate-pulse">CARREGANDO PERFIL...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center gap-4">
        <p className="font-mono-data text-mono-data text-error border-l-2 border-error pl-3">{error}</p>
        <button onClick={() => navigate('/lobby')} className="font-label-caps text-label-caps text-primary border-b border-primary/30 pb-1">
          VOLTAR AO LOBBY
        </button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background flex flex-col">
      {/* Header */}
      <header className="flex justify-between items-center px-panel-padding h-12 bg-surface-container-low border-b border-outline-variant z-40">
        <button
          onClick={() => navigate('/lobby')}
          className="flex items-center gap-2 font-label-caps text-label-caps text-primary hover:text-primary-fixed transition-colors"
        >
          <span className="material-symbols-outlined text-sm">arrow_back</span>
          LOBBY
        </button>
        <span className="font-mono-data text-mono-data text-on-surface-variant">
          PERFIL DO OPERADOR
        </span>
      </header>

      {/* Content */}
      <div className="flex-1 flex items-start justify-center p-margin-safe overflow-auto">
        <div className="w-full max-w-2xl flex flex-col gap-6 py-8">
          {/* Profile card */}
          <section className="bg-surface-container border border-outline-variant p-panel-padding flex flex-col items-center gap-4 relative">
            <div className="corner-accent tl" /><div className="corner-accent tr" />
            <div className="corner-accent bl" /><div className="corner-accent br" />

            <div className="w-20 h-20 rounded-full border-2 border-primary bg-secondary-container flex items-center justify-center font-display-tactical text-display-tactical text-primary">
              {profile.username?.[0]?.toUpperCase()}
            </div>
            <h1 className="font-headline-lg text-headline-lg text-primary uppercase tracking-widest">
              {profile.username}
            </h1>
            <p className="font-mono-data text-mono-data text-on-surface-variant">
              MEMBRO DESDE {formatDate(profile.memberSince)}
            </p>

            {/* Stats grid */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 w-full mt-4">
              {[
                { label: 'RANK', value: `#${profile.rank}` },
                { label: 'VITÓRIAS', value: profile.wins },
                { label: 'DERROTAS', value: profile.losses },
                { label: 'WIN RATE', value: `${profile.winRate}%` },
              ].map((stat) => (
                <div key={stat.label} className="bg-surface-container-high p-4 flex flex-col items-center gap-2 border border-outline-variant">
                  <span className="font-label-caps text-label-caps text-on-surface-variant">{stat.label}</span>
                  <span className="font-display-tactical text-headline-md text-primary">{stat.value}</span>
                </div>
              ))}
            </div>

            <div className="bg-surface-container-high p-4 flex items-center justify-center gap-2 border border-outline-variant w-full">
              <span className="font-label-caps text-label-caps text-on-surface-variant">TOTAL DE PARTIDAS:</span>
              <span className="font-mono-data text-mono-data text-primary">{profile.totalGames}</span>
            </div>
          </section>

          {/* Combat stats */}
          <section className="bg-surface-container border border-outline-variant p-panel-padding flex flex-col gap-4">
            <header className="border-b border-outline-variant pb-2 flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-sm">target</span>
              <h2 className="font-headline-md text-headline-md text-primary tracking-widest">
                STATS DE COMBATE
              </h2>
            </header>

            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 w-full">
              {[
                { label: 'TIROS', value: profile.totalShots ?? 0 },
                { label: 'ACERTOS', value: profile.shotsHit ?? 0 },
                { label: 'NAVIOS AFUNDADOS', value: profile.shipsSunk ?? 0 },
                { label: 'PRECISÃO', value: `${profile.accuracy ?? 0}%` },
              ].map((stat) => (
                <div key={stat.label} className="bg-surface-container-high p-4 flex flex-col items-center gap-2 border border-outline-variant">
                  <span className="font-label-caps text-label-caps text-on-surface-variant">{stat.label}</span>
                  <span className="font-display-tactical text-headline-md text-primary">{stat.value}</span>
                </div>
              ))}
            </div>
          </section>

          {/* Recent games */}
          <section className="bg-surface-container border border-outline-variant p-panel-padding flex flex-col gap-4">
            <header className="border-b border-outline-variant pb-2 flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-sm">history</span>
              <h2 className="font-headline-md text-headline-md text-primary tracking-widest">
                PARTIDAS RECENTES
              </h2>
            </header>

            {profile.recentGames?.length === 0 ? (
              <p className="font-mono-data text-mono-data text-on-surface-variant text-center py-4">
                NENHUMA PARTIDA
              </p>
            ) : (
              <div className="flex flex-col gap-2">
                {profile.recentGames?.map((entry) => (
                  <div
                    key={entry.id}
                    className={`flex items-center justify-between p-3 border ${
                      entry.won ? 'border-primary/30 bg-primary-container/5' : 'border-error/30 bg-error/5'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <span className={`material-symbols-outlined text-sm ${entry.won ? 'text-primary' : 'text-error'}`}>
                        {entry.won ? 'emoji_events' : 'close'}
                      </span>
                      <span className="font-mono-data text-mono-data text-on-surface">
                        vs {entry.opponentUsername?.toUpperCase()}
                      </span>
                    </div>
                    <div className="flex items-center gap-4">
                      <span className="font-mono-data text-mono-data text-on-surface-variant">
                        {formatDuration(entry.durationSeconds)}
                      </span>
                      <span className={`font-label-caps text-label-caps px-2 py-0.5 border ${
                        entry.won ? 'text-primary border-primary' : 'text-error border-error'
                      }`}>
                        {entry.won ? 'V' : 'D'}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;
