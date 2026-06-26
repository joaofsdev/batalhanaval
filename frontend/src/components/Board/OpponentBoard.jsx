import BoardCell from './BoardCell';

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
    <div>
      <p className="text-slate-400 text-sm mb-2 text-center">Tabuleiro do Oponente</p>
      <div className="grid grid-cols-10 gap-0">
        {Array.from({ length: 100 }, (_, i) => {
          const row = Math.floor(i / 10);
          const col = i % 10;
          const hasShot = shotMap.has(`${row},${col}`);
          return (
            <BoardCell
              key={i}
              state={getCellState(row, col)}
              onClick={() => handleClick(row, col)}
              onMouseEnter={undefined}
              onMouseLeave={undefined}
              style={{ cursor: isMyTurn && !hasShot ? 'crosshair' : 'default' }}
            />
          );
        })}
      </div>
    </div>
  );
};

export default OpponentBoard;
