import BoardCell from './BoardCell';

const MyBoard = ({ cells, ships }) => {
  const sunkShipIds = new Set(
    (ships || []).filter((s) => s.sunk).flatMap((s) => {
      const coords = [];
      for (let i = 0; i < getShipSize(s.shipType); i++) {
        const r = s.orientation === 'VERTICAL' ? s.originRow + i : s.originRow;
        const c = s.orientation === 'HORIZONTAL' ? s.originCol + i : s.originCol;
        coords.push(`${r},${c}`);
      }
      return coords;
    })
  );

  const getCellState = (row, col) => {
    const cell = cells?.find((c) => c.row === row && c.col === col);
    if (!cell) return 'empty';
    const key = `${row},${col}`;
    if (cell.hit && cell.hasShip && sunkShipIds.has(key)) return 'sunk';
    if (cell.hit && cell.hasShip) return 'hit';
    if (cell.hit && !cell.hasShip) return 'miss';
    if (cell.hasShip) return 'ship';
    return 'empty';
  };

  return (
    <div>
      <p className="text-slate-400 text-sm mb-2 text-center">Meu Tabuleiro</p>
      <div className="grid grid-cols-10 gap-0">
        {Array.from({ length: 100 }, (_, i) => {
          const row = Math.floor(i / 10);
          const col = i % 10;
          return <BoardCell key={i} state={getCellState(row, col)} />;
        })}
      </div>
    </div>
  );
};

const getShipSize = (type) => {
  const sizes = { CARRIER: 5, BATTLESHIP: 4, CRUISER: 3, SUBMARINE: 3, DESTROYER: 2 };
  return sizes[type] || 0;
};

export default MyBoard;
