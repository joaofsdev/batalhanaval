import { useMemo } from 'react';
import BoardCell from './BoardCell';

const ROWS = ['A','B','C','D','E','F','G','H','I','J'];

const MyBoard = ({ cells, ships }) => {
  const cellMap = useMemo(() => {
    const map = new Map();
    (cells || []).forEach((c) => map.set(`${c.row},${c.col}`, c));
    return map;
  }, [cells]);

  const sunkCells = useMemo(() => {
    const set = new Set();
    (ships || []).filter((s) => s.sunk).forEach((s) => {
      const size = getShipSize(s.shipType);
      for (let i = 0; i < size; i++) {
        const r = s.orientation === 'VERTICAL' ? s.originRow + i : s.originRow;
        const c = s.orientation === 'HORIZONTAL' ? s.originCol + i : s.originCol;
        set.add(`${r},${c}`);
      }
    });
    return set;
  }, [ships]);

  const getCellState = (row, col) => {
    const key = `${row},${col}`;
    const cell = cellMap.get(key);
    if (!cell) return 'empty';
    if (cell.hit && cell.hasShip && sunkCells.has(key)) return 'sunk';
    if (cell.hit && cell.hasShip) return 'hit';
    if (cell.hit && !cell.hasShip) return 'miss';
    if (cell.hasShip) return 'ship';
    return 'empty';
  };

  return (
    <div className="flex flex-col items-center gap-2">
      <div className="bg-surface-container border border-outline-variant p-3">
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
              return <BoardCell key={i} state={getCellState(row, col)} />;
            })}
          </div>
        </div>
      </div>
      <span className="font-label-caps text-label-caps text-on-surface-variant">MEU TABULEIRO</span>
    </div>
  );
};

const getShipSize = (type) => {
  const sizes = { CARRIER: 5, BATTLESHIP: 4, CRUISER: 3, SUBMARINE: 3, DESTROYER: 2 };
  return sizes[type] || 0;
};

export default MyBoard;
