import { useMemo } from 'react';
import BoardCell from '../Board/BoardCell';
import { ShipSprite } from '../Board/ShipSprite';

const ROWS = ['A','B','C','D','E','F','G','H','I','J'];
const SHIP_SIZES = { CARRIER: 5, BATTLESHIP: 4, CRUISER: 3, SUBMARINE: 3, DESTROYER: 2 };

const PostGameBoards = ({ myBoard, opponentBoard, onBack }) => {
  const myCellMap = useMemo(() => {
    const map = new Map();
    (myBoard?.cells || []).forEach((c) => map.set(`${c.row},${c.col}`, c));
    return map;
  }, [myBoard]);

  const opponentShotMap = useMemo(() => {
    const map = new Map();
    (opponentBoard?.shotsReceived || []).forEach((s) => map.set(`${s.row},${s.col}`, s.result));
    return map;
  }, [opponentBoard]);

  const getMyCellState = (row, col) => {
    const key = `${row},${col}`;
    const cell = myCellMap.get(key);
    if (!cell) return 'empty';
    if (cell.hit && cell.hasShip) return 'hit';
    if (cell.hit && !cell.hasShip) return 'miss';
    return 'empty';
  };

  const getOpponentCellState = (row, col) => {
    const key = `${row},${col}`;
    const result = opponentShotMap.get(key);
    if (!result) return 'empty';
    if (result === 'SUNK' || result === 'HIT') return 'hit';
    if (result === 'MISS') return 'miss';
    return 'empty';
  };

  const opponentShips = opponentBoard?.opponentShips || [];

  return (
    <div className="fixed inset-0 z-50 flex flex-col items-center bg-background/95 backdrop-blur-sm overflow-y-auto">
      <div className="w-full max-w-5xl px-4 pt-6 pb-4 flex items-center justify-between">
        <button
          onClick={onBack}
          className="flex items-center gap-2 px-4 py-2 border border-outline-variant text-on-surface-variant font-label-caps text-label-caps hover:bg-surface-container-high transition-all"
        >
          <span className="material-symbols-outlined text-sm">arrow_back</span>
          VOLTAR
        </button>
        <h2 className="font-display-tactical text-display-tactical text-primary uppercase tracking-widest"
            style={{ fontSize: 'clamp(0.8rem, 2.5vw, 1.2rem)' }}>
          [ RELATÓRIO DE BATALHA ]
        </h2>
        <div className="w-24" />
      </div>

      <div className="flex flex-col md:flex-row gap-8 items-start justify-center px-4 pb-8">
        <div className="flex flex-col items-center gap-3">
          <h3 className="font-label-caps text-label-caps text-on-surface-variant tracking-wider">
            MEU TABULEIRO
          </h3>
          <div className="bg-surface-container p-4 border border-outline-variant">
            <div className="flex">
              <div className="w-5" />
              {Array.from({ length: 10 }, (_, i) => (
                <div key={i} className="w-8 flex items-center justify-center">
                  <span className="font-mono-data text-[10px] text-on-surface-variant">{i + 1}</span>
                </div>
              ))}
            </div>
            <div className="flex">
              <div className="flex flex-col">
                {ROWS.map((r) => (
                  <div key={r} className="h-8 w-5 flex items-center justify-center">
                    <span className="font-mono-data text-[10px] text-on-surface-variant">{r}</span>
                  </div>
                ))}
              </div>
              <div className="relative">
                <div className="grid grid-cols-10 gap-grid-gap bg-outline-variant/50 border border-outline-variant">
                  {Array.from({ length: 100 }, (_, i) => {
                    const row = Math.floor(i / 10);
                    const col = i % 10;
                    return <BoardCell key={i} state={getMyCellState(row, col)} />;
                  })}
                </div>
                <div className="absolute z-10 pointer-events-none" style={{ top: 1, left: 1 }}>
                  {(myBoard?.ships || []).map((ship) => (
                    <ShipSprite
                      key={ship.shipType}
                      shipType={ship.shipType}
                      originRow={ship.originRow}
                      originCol={ship.originCol}
                      orientation={ship.orientation}
                      size={SHIP_SIZES[ship.shipType] || 3}
                      isSunk={ship.sunk}
                    />
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="flex flex-col items-center gap-3">
          <h3 className="font-label-caps text-label-caps text-on-surface-variant tracking-wider">
            TABULEIRO DO OPONENTE
          </h3>
          <div className="bg-surface-container p-4 border border-outline-variant">
            <div className="flex">
              <div className="w-5" />
              {Array.from({ length: 10 }, (_, i) => (
                <div key={i} className="w-8 flex items-center justify-center">
                  <span className="font-mono-data text-[10px] text-on-surface-variant">{i + 1}</span>
                </div>
              ))}
            </div>
            <div className="flex">
              <div className="flex flex-col">
                {ROWS.map((r) => (
                  <div key={r} className="h-8 w-5 flex items-center justify-center">
                    <span className="font-mono-data text-[10px] text-on-surface-variant">{r}</span>
                  </div>
                ))}
              </div>
              <div className="relative">
                <div className="grid grid-cols-10 gap-grid-gap bg-outline-variant/50 border border-outline-variant">
                  {Array.from({ length: 100 }, (_, i) => {
                    const row = Math.floor(i / 10);
                    const col = i % 10;
                    return <BoardCell key={i} state={getOpponentCellState(row, col)} />;
                  })}
                </div>
                {opponentShips.length > 0 && (
                  <div className="absolute z-10 pointer-events-none" style={{ top: 1, left: 1 }}>
                    {opponentShips.map((ship) => (
                      <ShipSprite
                        key={ship.shipType}
                        shipType={ship.shipType}
                        originRow={ship.originRow}
                        originCol={ship.originCol}
                        orientation={ship.orientation}
                        size={SHIP_SIZES[ship.shipType] || 3}
                        isSunk={ship.sunk}
                      />
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="flex gap-6 pb-8">
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 bg-surface-container border border-outline-variant/30" />
          <span className="font-mono-data text-[11px] text-on-surface-variant">INTACTO</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 grid-cell hit" />
          <span className="font-mono-data text-[11px] text-on-surface-variant">ACERTO</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 grid-cell miss" />
          <span className="font-mono-data text-[11px] text-on-surface-variant">ERRO</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 bg-red-900/50 border border-red-400/30" />
          <span className="font-mono-data text-[11px] text-on-surface-variant">AFUNDADO</span>
        </div>
      </div>
    </div>
  );
};

export default PostGameBoards;
