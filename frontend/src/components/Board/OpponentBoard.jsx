import { useMemo } from 'react';
import BoardCell from './BoardCell';
import { ShipSprite } from './ShipSprite';

const ROWS = ['A','B','C','D','E','F','G','H','I','J'];

const SHIP_SIZES = { CARRIER: 5, BATTLESHIP: 4, CRUISER: 3, SUBMARINE: 3, DESTROYER: 2 };

const getShipCellKeys = (originRow, originCol, orientation, size) => {
  const keys = [];
  for (let i = 0; i < size; i++) {
    const r = orientation === 'VERTICAL' ? originRow + i : originRow;
    const c = orientation === 'HORIZONTAL' ? originCol + i : originCol;
    keys.push(`${r},${c}`);
  }
  return keys;
};

const OpponentBoard = ({ shotsReceived, isMyTurn, onFire, fogActive = false, blockedRow = null, revealedShips = null }) => {
  const shotMap = new Map(
    (shotsReceived || []).map((s) => [`${s.row},${s.col}`, s.result])
  );

  const sunkShips = useMemo(() => {
    const shots = shotsReceived || [];
    const seen = new Set();
    const ships = [];

    shots.filter((s) => s.result === 'SUNK' && s.sunkShipType && s.sunkShipOriginRow != null && s.sunkShipOrientation).forEach((s) => {
      const shipKey = `${s.sunkShipType}-${s.sunkShipOriginRow}-${s.sunkShipOriginCol}`;
      if (seen.has(shipKey)) return;
      seen.add(shipKey);

      ships.push({
        shipType: s.sunkShipType,
        originRow: s.sunkShipOriginRow,
        originCol: s.sunkShipOriginCol,
        orientation: s.sunkShipOrientation,
        size: SHIP_SIZES[s.sunkShipType] || 1,
      });
    });

    return ships;
  }, [shotsReceived]);

  const sunkCells = useMemo(() => {
    const sunk = new Set();
    sunkShips.forEach((ship) => {
      const keys = getShipCellKeys(ship.originRow, ship.originCol, ship.orientation, ship.size);
      keys.forEach((k) => sunk.add(k));
    });
    return sunk;
  }, [sunkShips]);

  const getCellState = (row, col) => {
    const key = `${row},${col}`;
    const result = shotMap.get(key);
    if (!result) return 'empty';
    if (result === 'HIDDEN') return 'fog-hidden';
    if (fogActive) return 'fog-hidden';
    if (sunkCells.has(key)) return 'sunk';
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
        <div className="flex mb-1 ml-6">
          {Array.from({ length: 10 }, (_, i) => (
            <div key={i} className="w-8 h-6 flex items-center justify-center font-mono-data text-[10px] text-primary-fixed-dim">
              {i + 1}
            </div>
          ))}
        </div>

        <div className="flex">
          <div className="flex flex-col mr-1">
            {ROWS.map(r => (
              <div key={r} className="w-5 h-8 flex items-center justify-center font-mono-data text-[10px] text-primary-fixed-dim">
                {r}
              </div>
            ))}
          </div>

          <div className="relative">
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
                    {fogActive && hasShot && (
                      <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                        <span className="font-mono-data text-sm text-on-surface/70">?</span>
                      </div>
                    )}
                    {blocked && !hasShot && (
                      <div className="absolute inset-0 bg-blue-900/40 pointer-events-none border border-blue-400/30" />
                    )}
                  </div>
                );
              })}
            </div>

            {sunkShips.length > 0 && !fogActive && (
              <div className="absolute z-10 pointer-events-none" style={{ top: 1, left: 1 }}>
                {sunkShips.map((ship) => (
                  <ShipSprite
                    key={`${ship.shipType}-${ship.originRow}-${ship.originCol}`}
                    shipType={ship.shipType}
                    originRow={ship.originRow}
                    originCol={ship.originCol}
                    orientation={ship.orientation}
                    size={ship.size}
                    isSunk
                  />
                ))}
              </div>
            )}

            {revealedShips && revealedShips.length > 0 && (
              <div className="absolute z-20 pointer-events-none opacity-60" style={{ top: 1, left: 1 }}>
                {revealedShips.map((ship) => (
                  <ShipSprite
                    key={`reveal-${ship.shipType}-${ship.originRow}-${ship.originCol}`}
                    shipType={ship.shipType}
                    originRow={ship.originRow}
                    originCol={ship.originCol}
                    orientation={ship.orientation}
                    size={SHIP_SIZES[ship.shipType] || 1}
                  />
                ))}
              </div>
            )}
          </div>
        </div>

        {fogActive && (
          <div className="absolute inset-0 bg-slate-900/30 backdrop-blur-[1px] pointer-events-none rounded-sm" />
        )}
      </div>
      <span className="font-label-caps text-label-caps text-primary">
        {isMyTurn ? 'SELECIONE O ALVO' : 'TABULEIRO INIMIGO'}
      </span>
    </div>
  );
};

export default OpponentBoard;
