import BoardCell from './BoardCell';

const ROWS = ['A','B','C','D','E','F','G','H','I','J'];

const OpponentBoard = ({ shotsReceived, isMyTurn, onFire }) => {
  const shotMap = new Map(
    (shotsReceived || []).map((s) => [`${s.row},${s.col}`, s.result])
  );

  const getCellState = (row, col) => {
    const result = shotMap.get(`${row},${col}`);
    if (result === 'SUNK') return 'sunk';
    if (result === 'HIT') return 'hit';
    if (result === 'MISS') return 'miss';
    return 'empty';
  };

  const handleClick = (row, col) => {
    if (!isMyTurn) return;
    if (shotMap.has(`${row},${col}`)) return;
    onFire(row, col);
  };

  return (
    <div className="flex flex-col items-center gap-2">
      <div className={`bg-surface-container-lowest p-3 border-2 ${isMyTurn ? 'border-primary radar-glow-intense' : 'border-outline-variant'}`}>
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
              return (
                <BoardCell
                  key={i}
                  state={getCellState(row, col)}
                  onClick={() => handleClick(row, col)}
                  style={{ cursor: isMyTurn && !hasShot ? 'crosshair' : 'default' }}
                />
              );
            })}
          </div>
        </div>
      </div>
      <span className="font-label-caps text-label-caps text-primary">
        {isMyTurn ? 'SELECIONE O ALVO' : 'FOG OF WAR'}
      </span>
    </div>
  );
};

export default OpponentBoard;
