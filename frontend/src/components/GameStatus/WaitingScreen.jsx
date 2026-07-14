const WaitingScreen = ({ gameId, myUsername, onCancel, canCancel = true, opponentFound = false }) => (
  <div className="flex flex-col items-center justify-center h-full gap-8 bg-background p-8">
    <h2 className={`font-headline-lg text-headline-lg glow-text uppercase tracking-widest animate-pulse ${opponentFound ? 'text-tertiary' : 'text-primary'}`}>
      {opponentFound ? 'ADVERSÁRIO ENCONTRADO!' : 'AGUARDANDO OPONENTE...'}
    </h2>

    <div className="relative w-48 h-48 flex items-center justify-center">
      <div className="absolute w-full h-full border border-primary-container/40 rounded-full">
        <div className="absolute w-full h-full border border-primary-container rounded-full sonar-pulse" />
        <div className="absolute inset-[20%] border border-primary-container rounded-full sonar-pulse" style={{ animationDelay: '1s' }} />
        <div className="absolute inset-[40%] border border-primary-container rounded-full sonar-pulse" style={{ animationDelay: '2s' }} />
      </div>
      <div className={`w-4 h-4 rounded-full shadow-[0_0_15px_rgba(34,211,238,1)] animate-pulse ${opponentFound ? 'bg-tertiary' : 'bg-primary-container'}`} />
    </div>

    {gameId && !opponentFound && (
      <div className="flex flex-col items-center gap-2">
        <span className="font-label-caps text-label-caps text-on-surface-variant">
          ID DA MISSÃO
        </span>
        <div className="flex items-center gap-2 border border-outline-variant px-4 py-2">
          <span className="font-mono-data text-mono-data text-primary">
            #{gameId?.slice(0, 8).toUpperCase()}
          </span>
          <button
            onClick={() => navigator.clipboard.writeText(gameId)}
            className="text-on-surface-variant hover:text-primary transition-colors"
          >
            <span className="material-symbols-outlined text-sm">content_copy</span>
          </button>
        </div>
      </div>
    )}

    {!opponentFound && (
      <div className="w-full max-w-sm flex flex-col gap-3">
        <div className="flex items-center gap-4 p-4 border border-outline-variant bg-surface-container">
          <div className="w-10 h-10 rounded-full border border-primary bg-secondary-container flex items-center justify-center font-label-caps text-label-caps text-primary">
            {myUsername?.[0]?.toUpperCase() || '?'}
          </div>
          <div className="flex-1">
            <p className="font-mono-data text-mono-data text-on-surface">
              {myUsername?.toUpperCase() || 'VOCÊ'}
            </p>
            <p className="font-label-caps text-label-caps text-primary text-[10px]">PRONTO</p>
          </div>
          <span className="material-symbols-outlined text-primary">check_circle</span>
        </div>

        <div className="flex items-center gap-4 p-4 border border-outline-variant/40 bg-surface-container opacity-60">
          <div className="w-10 h-10 rounded-full border border-outline-variant flex items-center justify-center">
            <span className="font-display-tactical text-on-surface-variant">?</span>
          </div>
          <div className="flex-1">
            <p className="font-mono-data text-mono-data text-on-surface-variant italic">Oponente</p>
            <p className="font-label-caps text-label-caps text-on-surface-variant text-[10px]">AGUARDANDO...</p>
          </div>
          <span className="material-symbols-outlined text-on-surface-variant animate-spin" style={{ animationDuration: '2s' }}>sync</span>
        </div>
      </div>
    )}

    {!opponentFound && (
      <button
        onClick={onCancel}
        disabled={!canCancel}
        className="mt-4 px-8 py-2 border border-error text-error font-label-caps text-label-caps hover:bg-error/10 transition-all flex items-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed"
      >
        <span className="material-symbols-outlined text-sm">close</span>
        CANCELAR MISSÃO
      </button>
    )}

    {opponentFound && (
      <p className="font-mono-data text-mono-data text-tertiary animate-pulse">
        PREPARANDO CAMPO DE BATALHA...
      </p>
    )}
  </div>
);

export default WaitingScreen;
