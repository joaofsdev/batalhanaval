import { useState, useEffect } from 'react';

const STORM_DESCRIPTIONS = {
  FOG: 'Neblina — resultados de tiros ocultos até o próximo turno',
  TIDE: 'Maré — uma linha do tabuleiro está bloqueada',
  CURRENT: 'Corrente — navios foram deslocados',
  CALM: 'Calmaria — turno bônus concedido',
};

const getCurrentDescription = (stormEvent) => {
  if (stormEvent.shipMoved === true) {
    return 'Corrente Marítima! Seus navios podem ter se deslocado.';
  }
  if (stormEvent.shipMoved === false) {
    return 'Corrente Marítima passou sem efeito desta vez.';
  }
  return STORM_DESCRIPTIONS.CURRENT;
};

const StormTracker = ({ turnsUntilStorm, isStormTurn, stormEvent }) => {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    if (isStormTurn && stormEvent) {
      setVisible(true);
    } else {
      setVisible(false);
    }
  }, [isStormTurn, stormEvent]);

  return (
    <div className="flex flex-col gap-2">
      {/* Countdown */}
      {!isStormTurn && turnsUntilStorm != null && turnsUntilStorm > 0 && (
        <div className="flex items-center gap-2 bg-surface-container px-3 py-2 border border-outline-variant">
          <span className="material-symbols-outlined text-warning text-sm">thunderstorm</span>
          <span className="font-mono-data text-mono-data text-on-surface-variant">
            TEMPESTADE EM {turnsUntilStorm} TURNO{turnsUntilStorm > 1 ? 'S' : ''}
          </span>
        </div>
      )}

      {/* Storm banner */}
      <div
        className={`overflow-hidden transition-all duration-500 ease-out ${
          visible ? 'max-h-32 opacity-100' : 'max-h-0 opacity-0'
        }`}
      >
        {stormEvent && (
          <div className="bg-warning/10 border border-warning/40 px-4 py-3 flex items-center gap-3">
            <span className="material-symbols-outlined text-warning text-xl">thunderstorm</span>
            <div>
              <p className="font-label-caps text-label-caps text-warning">
                TEMPESTADE — {stormEvent.type}
              </p>
              <p className="text-xs text-on-surface-variant">
                {stormEvent.type === 'CURRENT'
                  ? getCurrentDescription(stormEvent)
                  : (stormEvent.message || STORM_DESCRIPTIONS[stormEvent.type] || 'Evento de tempestade ativo')}
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default StormTracker;
