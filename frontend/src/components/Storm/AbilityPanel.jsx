import { useEffect, useState } from 'react';
import * as gameApi from '../../api/gameApi';

const ABILITY_ICONS = {
  RADAR: 'radar',
  SHIELD: 'shield',
  DOUBLE_TORPEDO: 'rocket_launch',
  LINE_BOMBARDMENT: 'destruction',
};

const ABILITY_ACTION_LABELS = {
  RADAR: 'USAR RADAR',
  SHIELD: 'ATIVAR ESCUDO',
  DOUBLE_TORPEDO: 'DISPARAR TORPEDO DUPLO',
  LINE_BOMBARDMENT: 'BOMBARDEAR LINHA',
};

const NEEDS_TARGET = ['RADAR', 'DOUBLE_TORPEDO', 'LINE_BOMBARDMENT'];

/**
 * Converts user input (e.g. "E5") to { row, col } for RADAR.
 * Convention: letter = row (A-J → 0-9), number = col (1-10 → 0-9).
 * Matches OpponentBoard: rows are letters (vertical), cols are numbers (horizontal).
 */
const parseRadarTarget = (input) => {
  const match = input.match(/^([A-J])(\d{1,2})$/i);
  if (!match) return null;
  const row = match[1].toUpperCase().charCodeAt(0) - 'A'.charCodeAt(0);
  const col = parseInt(match[2], 10) - 1;
  if (row < 0 || row > 9 || col < 0 || col > 9) return null;
  return { row, col };
};

/**
 * Converts user input for LINE_BOMBARDMENT.
 * - Letter (A-J) → { axis: "ROW", index: 0-9 } (letters are rows in the board)
 * - Number (1-10) → { axis: "COL", index: 0-9 } (numbers are columns in the board)
 */
const parseLineBombardmentTarget = (input) => {
  const letterMatch = input.match(/^([A-J])$/i);
  if (letterMatch) {
    const index = letterMatch[1].toUpperCase().charCodeAt(0) - 'A'.charCodeAt(0);
    return { axis: 'ROW', index };
  }
  const numberMatch = input.match(/^(\d{1,2})$/);
  if (numberMatch) {
    const index = parseInt(numberMatch[1], 10) - 1;
    if (index < 0 || index > 9) return null;
    return { axis: 'COL', index };
  }
  return null;
};

/**
 * Builds the payload matching UseAbilityRequest backend contract.
 */
const buildPayload = (abilityType, targetInput) => {
  if (abilityType === 'RADAR' || abilityType === 'DOUBLE_TORPEDO') {
    const coords = parseRadarTarget(targetInput);
    if (!coords) return null;
    return { abilityType, row: coords.row, col: coords.col };
  }
  if (abilityType === 'LINE_BOMBARDMENT') {
    const target = parseLineBombardmentTarget(targetInput);
    if (!target) return null;
    return { abilityType, axis: target.axis, index: target.index };
  }
  // SHIELD — no target needed
  return { abilityType };
};

const AbilityResultDisplay = ({ result }) => {
  if (!result) return null;

  // Radar: show 3x3 grid
  if (result.abilityType === 'RADAR' && result.radarGrid) {
    const centerRow = result.centerRow;
    const centerCol = result.centerCol;
    return (
      <div className="bg-primary/10 border border-primary/30 p-3 flex flex-col gap-2">
        <p className="text-xs text-primary font-mono-data">RADAR — Área {String.fromCharCode(65 + centerRow)}{centerCol + 1}:</p>
        <div className="grid grid-cols-3 gap-1 w-fit mx-auto">
          {result.radarGrid.map((row, ri) =>
            row.map((hasShip, ci) => (
              <div
                key={`${ri}-${ci}`}
                className={`w-6 h-6 border flex items-center justify-center text-xs font-mono-data ${
                  hasShip
                    ? 'bg-error/80 border-error text-on-primary'
                    : 'bg-surface-container-high border-outline-variant text-on-surface-variant'
                }`}
              >
                {hasShip ? '█' : '·'}
              </div>
            ))
          )}
        </div>
        <p className="text-[10px] text-on-surface-variant text-center">█ = navio detectado</p>
      </div>
    );
  }

  // Double torpedo / Line bombardment: show shot results
  if (result.shotResults && result.shotResults.length > 0) {
    return (
      <div className="bg-primary/10 border border-primary/30 p-3 flex flex-col gap-1">
        <p className="text-xs text-primary font-mono-data">
          {result.abilityType === 'DOUBLE_TORPEDO' ? 'TORPEDO DUPLO' : 'BOMBARDEIO'} — Resultados:
        </p>
        {result.shotResults.map((shot, i) => (
          <div key={i} className="flex items-center gap-2 text-xs font-mono-data">
            <span className="text-on-surface-variant">
              {String.fromCharCode(65 + shot.row)}{shot.col + 1}:
            </span>
            <span className={
              shot.result === 'SUNK' ? 'text-error' :
              shot.result === 'HIT' ? 'text-warning' :
              'text-on-surface-variant'
            }>
              {shot.result === 'SUNK' ? 'AFUNDOU!' : shot.result === 'HIT' ? 'ACERTOU' : 'ÁGUA'}
            </span>
            {shot.sunkShipType && <span className="text-error">({shot.sunkShipType})</span>}
          </div>
        ))}
      </div>
    );
  }

  // Shield / generic message
  return (
    <div className="bg-primary/10 border border-primary/30 p-2 text-xs text-primary font-mono-data">
      {result.message || 'Habilidade usada com sucesso!'}
    </div>
  );
};

const AbilityPanel = ({ gameId, isMyTurn, isStormTurn, abilityResult, onUseAbility }) => {
  const [ability, setAbility] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [used, setUsed] = useState(false);
  const [showTargetModal, setShowTargetModal] = useState(false);
  const [targetInput, setTargetInput] = useState('');
  const [targetError, setTargetError] = useState('');
  const [localResult, setLocalResult] = useState(null);
  const fetchAbility = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await gameApi.getAbility(gameId);
      setAbility(res.data);
      setUsed(res.data.used || false);
    } catch (err) {
      const status = err?.response?.status;
      const message = err?.response?.data?.message || err?.message || 'Erro desconhecido';
      setAbility(null);
      setError(`Falha ao carregar habilidade (${status || 'rede'}): ${message}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAbility();
  }, [gameId]);

  useEffect(() => {
    if (abilityResult) {
      setUsed(true);
    }
  }, [abilityResult]);

  if (loading) {
    return (
      <div className="bg-surface-container p-4 border border-outline-variant flex items-center justify-center gap-2">
        <span className="material-symbols-outlined text-on-surface-variant animate-spin text-sm">progress_activity</span>
        <span className="text-xs text-on-surface-variant font-mono-data">CARREGANDO HABILIDADE...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-surface-container p-4 border border-error/50 flex flex-col gap-3">
        <div className="flex items-center gap-2">
          <span className="material-symbols-outlined text-error text-lg">error</span>
          <p className="text-xs text-error font-mono-data">{error}</p>
        </div>
        <button
          onClick={fetchAbility}
          className="w-full py-2 border border-error text-error font-label-caps text-label-caps hover:bg-error/10 transition-colors"
        >
          TENTAR NOVAMENTE
        </button>
      </div>
    );
  }

  if (!ability) return null;

  const canUse = !used && isMyTurn && !isStormTurn;

  const handleUseAbility = async (payload) => {
    try {
      const res = await onUseAbility(payload);
      setUsed(true);
      setLocalResult(res.data);
    } catch (err) {
      const message = err?.response?.data?.message || 'Erro ao usar habilidade';
      setTargetError(message);
    }
  };

  const handleClick = () => {
    if (!canUse) return;
    if (NEEDS_TARGET.includes(ability.abilityType)) {
      setShowTargetModal(true);
      setTargetError('');
    } else {
      const payload = buildPayload(ability.abilityType, '');
      handleUseAbility(payload);
    }
  };

  const handleConfirmTarget = () => {
    const value = targetInput.trim().toUpperCase();
    if (!value) return;

    const payload = buildPayload(ability.abilityType, value);
    if (!payload) {
      setTargetError(
        ability.abilityType === 'LINE_BOMBARDMENT'
          ? 'Formato inválido. Use letra (A-J) para linha ou número (1-10) para coluna'
          : 'Formato inválido. Use letra + número (ex: E5)'
      );
      return;
    }

    handleUseAbility(payload);
    setShowTargetModal(false);
    setTargetInput('');
    setTargetError('');
  };

  return (
    <div className="bg-surface-container p-4 border border-outline-variant flex flex-col gap-3">
      {/* Ability info */}
      <div className="flex items-center gap-3">
        <span className="material-symbols-outlined text-primary text-2xl">
          {ABILITY_ICONS[ability.abilityType] || 'auto_awesome'}
        </span>
        <div>
          <p className="font-label-caps text-label-caps text-on-surface">
            {ability.name}
          </p>
          <p className="text-xs text-on-surface-variant">{ability.description}</p>
        </div>
      </div>

      {/* Result display */}
      {(localResult || abilityResult) && (
        <AbilityResultDisplay result={localResult || abilityResult} />
      )}

      {/* Use button */}
      <button
        onClick={handleClick}
        disabled={!canUse}
        className={`w-full py-2 font-label-caps text-label-caps transition-all ${
          used
            ? 'bg-surface-container-high text-on-surface-variant/40 cursor-not-allowed opacity-60 line-through'
            : canUse
              ? 'bg-primary text-on-primary hover:bg-primary/90'
              : 'bg-surface-container-high text-on-surface-variant/50 cursor-not-allowed'
        }`}
      >
        {used && (
          <span className="inline-flex items-center gap-1">
            <span className="material-symbols-outlined text-sm">check_circle</span>
            HABILIDADE USADA
          </span>
        )}
        {!used && (ABILITY_ACTION_LABELS[ability.abilityType] || 'USAR HABILIDADE')}
      </button>
      {/* Disabled reason hint */}
      {!used && !canUse && (
        <p className="text-xs text-on-surface-variant/70 text-center">
          {isStormTurn ? 'Bloqueado por tempestade' : 'Aguarde seu turno'}
        </p>
      )}

      {/* Target selection modal */}
      {showTargetModal && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-surface-container-high p-6 border border-outline-variant flex flex-col gap-4 min-w-[280px]">
            <p className="font-label-caps text-label-caps text-on-surface">
              SELECIONE O ALVO
            </p>
            <p className="text-xs text-on-surface-variant">
              {ability.abilityType === 'LINE_BOMBARDMENT'
                ? 'Informe letra (A-J) para linha ou número (1-10) para coluna'
                : 'Informe coordenada do alvo (ex: E5)'}
            </p>
            <input
              type="text"
              value={targetInput}
              onChange={(e) => {
                setTargetInput(e.target.value);
                setTargetError('');
              }}
              placeholder={ability.abilityType === 'LINE_BOMBARDMENT' ? 'A ou 3' : 'E5'}
              className="bg-surface-container-lowest border border-outline-variant px-3 py-2 font-mono-data text-mono-data text-on-surface focus:border-primary outline-none"
              autoFocus
            />
            {targetError && (
              <p className="text-xs text-error">{targetError}</p>
            )}
            <div className="flex gap-2">
              <button
                onClick={() => {
                  setShowTargetModal(false);
                  setTargetError('');
                }}
                className="flex-1 py-2 border border-outline-variant text-on-surface-variant font-label-caps text-label-caps hover:bg-surface-container transition-colors"
              >
                CANCELAR
              </button>
              <button
                onClick={handleConfirmTarget}
                className="flex-1 py-2 bg-primary text-on-primary font-label-caps text-label-caps hover:bg-primary/90 transition-colors"
              >
                CONFIRMAR
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AbilityPanel;
