const TurnIndicator = ({ isMyTurn, opponentName }) => (
  isMyTurn ? (
    <div className="flex items-center justify-center gap-2 border border-primary px-4 py-2 bg-primary-container/10 radar-glow animate-pulse">
      <span className="material-symbols-outlined text-primary text-sm">my_location</span>
      <span className="font-label-caps text-label-caps text-primary">SUA VEZ — ATAQUE!</span>
    </div>
  ) : (
    <div className="flex items-center justify-center gap-2 border border-outline-variant px-4 py-2">
      <span className="material-symbols-outlined text-on-surface-variant text-sm animate-spin" style={{ animationDuration: '2s' }}>sync</span>
      <span className="font-label-caps text-label-caps text-on-surface-variant">
        AGUARDANDO {opponentName?.toUpperCase() || 'OPONENTE'}...
      </span>
    </div>
  )
);

export default TurnIndicator;
