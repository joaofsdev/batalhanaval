import BoardCell from './BoardCell';

const ROWS = ['A','B','C','D','E','F','G','H','I','J'];

const OpponentBoard = ({ shotsReceived, isMyTurn, onFire, fogActive = false, blockedRow = null }) => {
  const shotMap = new Map(
    (shotsReceived || []).map((s) => [`${s.row},${s.col}`, s.result])
  );

  const getCellState = (row, col) => {
    const result = shotMap.get(`${row},${col}`);
    if (!result) return 'empty';
    // HIDDEN result from backend (shot during fog) — always show as hidden
    if (result === 'HIDDEN') return 'fog-hidden';
    // Under active fog, mask all results as unknown
    if (fogActive) return 'fog-hidden';
    if (result === 'SUNK') return 'sunk';
    if (result === 'HIT') return 'hit';
    if (result === 'MISS') return 'miss';
    return 'empty';
  };

  const isRowBlocked = (row) => blockedRow != null && row === blockedRow;

  const handleClick = (row, col) => {
    if (!isMyTurn) return;
    if (shotMap.has(`${row},${col}`)) return;
    if (isRowBlocked(row)) return;
    onFire(row, col);
  };

  return (
    <div className="flex flex-col items-center gap-2">
      <div className={`relative bg-surface-container-lowest p-3 border-2 ${isMyTurn ? 'border-primary radar-glow-intense' : 'border-outline-variant'}`}>
        {/* Cabeçalho de colunas */}
        <div className="flex mb-1 ml-6">
          {Array.from({ length: 10 }, (_, i) => (
            <div key={i} className="w-8 h-6 flex items-center justify-center font-mono-data text-[10px] text-primary-fixed-dim">
              {i + 1}
            </div>
          ))}
        </div>

        <div className="flex">
          {/* Cabeçalho de linhas */}
          <div className="flex flex-col mr-1">
            {ROWS.map(r => (
              <div key={r} className="w-5 h-8 flex items-center justify-center font-mono-data text-[10px] text-primary-fixed-dim">
                {r}
              </div>
            ))}
          </div>

          {/* Grid */}
          <div className="grid grid-cols-10 gap-grid-gap bg-outline-variant/50 border border-outline-variant">
            {Array.from({ length: 100 }, (_, i) => {
              const row = Math.floor(i / 10);
              const col = i % 10;
              const hasShot = shotMap.has(`${row},${col}`);
              const cellState = getCellState(row, col);
              const blocked = isRowBlocked(row);

              return (
                <div key={i} className="relative">
                  <BoardCell
                    state={cellState === 'fog-hidden' ? 'empty' : cellState}
                    onClick={() => handleClick(row, col)}
                    style={{
                      cursor: isMyTurn && !hasShot && !blocked ? 'crosshair' : 'default',
                    }}
                  />
                  {/* Fog: show "?" over cells that have shots */}
                  {fogActive && hasShot && (
                    <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                      <span className="font-mono-data text-sm text-on-surface/70">?</span>
                    </div>
                  )}
                  {/* Tide: blocked row visual indicator */}
                  {blocked && !hasShot && (
                    <div className="absolute inset-0 bg-blue-900/40 pointer-events-none border border-blue-400/30" />
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* Fog overlay */}
        {fogActive && (
          <div className="absolute inset-0 bg-slate-900/30 backdrop-blur-[1px] pointer-events-none rounded-sm" />
        )}
      </div>
      <span className="font-label-caps text-label-caps text-primary">
        {fogActive ? 'NEBLINA ATIVA' : blockedRow != null ? `MARÉ — LINHA ${ROWS[blockedRow]} BLOQUEADA` : isMyTurn ? 'SELECIONE O ALVO' : 'FOG OF WAR'}
      </span>
    </div>
  );
};

export default OpponentBoard;
