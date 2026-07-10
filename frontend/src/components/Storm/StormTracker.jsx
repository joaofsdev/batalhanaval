const ROWS = ['A','B','C','D','E','F','G','H','I','J'];

const ACTIVE_EVENT_CONFIG = {
  FOG: {
    icon: '🌫️',
    label: 'NEBLINA ATIVA',
    description: 'Resultados de tiro ocultos até o próximo turno',
    borderColor: 'border-secondary/40',
    bgColor: 'bg-secondary/10',
    textColor: 'text-secondary',
  },
  TIDE: {
    icon: '🌊',
    label: 'MARÉ ALTA',
    borderColor: 'border-blue-400/40',
    bgColor: 'bg-blue-900/10',
    textColor: 'text-blue-400',
  },
};

const ActiveEventBanner = ({ fogActive, blockedRow }) => {
  if (fogActive) {
    const cfg = ACTIVE_EVENT_CONFIG.FOG;
    return (
      <div className={`flex items-center gap-2 px-3 py-2 border ${cfg.borderColor} ${cfg.bgColor}`}>
        <span className="text-base leading-none">{cfg.icon}</span>
        <div className="flex items-center gap-2 flex-wrap">
          <span className={`font-label-caps text-label-caps ${cfg.textColor}`}>{cfg.label}</span>
          <span className="text-xs text-on-surface-variant">— {cfg.description}</span>
        </div>
      </div>
    );
  }

  if (blockedRow != null) {
    const cfg = ACTIVE_EVENT_CONFIG.TIDE;
    const rowLetter = ROWS[blockedRow] || blockedRow;
    return (
      <div className={`flex items-center gap-2 px-3 py-2 border ${cfg.borderColor} ${cfg.bgColor}`}>
        <span className="text-base leading-none">{cfg.icon}</span>
        <div className="flex items-center gap-2 flex-wrap">
          <span className={`font-label-caps text-label-caps ${cfg.textColor}`}>{cfg.label}</span>
          <span className="text-xs text-on-surface-variant">— linha {rowLetter} bloqueada para disparos</span>
        </div>
      </div>
    );
  }

  return null;
};

const StormTracker = ({ turnsUntilStorm, isStormTurn, fogActive, blockedRow }) => {
  const hasActiveEffect = fogActive || blockedRow != null;
  const isStormPending = isStormTurn && !hasActiveEffect;

  return (
    <div className="flex flex-col gap-2">
      {isStormPending && (
        <div className="flex items-center gap-2 px-3 py-2 border border-warning/40 bg-warning/10 animate-pulse">
          <span className="material-symbols-outlined text-warning text-sm">thunderstorm</span>
          <div className="flex items-center gap-2 flex-wrap">
            <span className="font-label-caps text-label-caps text-warning">TEMPESTADE IMINENTE</span>
            <span className="text-xs text-on-surface-variant">— evento será resolvido com seu próximo disparo</span>
          </div>
        </div>
      )}

      {hasActiveEffect && (
        <ActiveEventBanner fogActive={fogActive} blockedRow={blockedRow} />
      )}

      {!isStormTurn && !hasActiveEffect && turnsUntilStorm != null && turnsUntilStorm > 0 && (
        <div className="flex items-center gap-2 bg-surface-container px-3 py-2 border border-outline-variant">
          <span className="material-symbols-outlined text-warning text-sm">thunderstorm</span>
          <span className="font-mono-data text-mono-data text-on-surface-variant">
            TEMPESTADE EM {turnsUntilStorm} TURNO{turnsUntilStorm > 1 ? 'S' : ''}
          </span>
        </div>
      )}
    </div>
  );
};

export default StormTracker;
