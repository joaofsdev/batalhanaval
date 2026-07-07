import { useMemo } from 'react';
import BoardCell from './BoardCell';
import { ShipSprite } from './ShipSprite';

const ROWS = ['A','B','C','D','E','F','G','H','I','J'];

const SHIP_SIZES = { CARRIER: 5, BATTLESHIP: 4, CRUISER: 3, SUBMARINE: 3, DESTROYER: 2 };

const OpponentBoard = ({ shotsReceived, isMyTurn, onFire, fogActive = false, blockedRow = null }) => {
  const shotMap = new Map(
    (shotsReceived || []).map((s) => [`${s.row},${s.col}`, s.result])
  );

  // Build set of all cells that belong to a sunk ship via linear flood-fill from SUNK cells
  const sunkCells = useMemo(() => {
    const shots = shotsReceived || [];
    const resultMap = new Map(shots.map((s) => [`${s.row},${s.col}`, s.result]));
    const sunk = new Set();
    const directions = [[0, 1], [0, -1], [1, 0], [-1, 0]];

    shots.filter((s) => s.result === 'SUNK').forEach((s) => {
      sunk.add(`${s.row},${s.col}`);
      for (const [dr, dc] of directions) {
        let r = s.row + dr;
        let c = s.col + dc;
        while (r >= 0 && r < 10 && c >= 0 && c < 10) {
          const key = `${r},${c}`;
          const res = resultMap.get(key);
          if (res === 'HIT' || res === 'SUNK') {
            sunk.add(key);
            r += dr;
            c += dc;
          } else {
            break;
          }
        }
      }
    });

    return sunk;
  }, [shotsReceived]);

  // Derive sunk ship sprites from SUNK shots + flood-fill
  const sunkShips = useMemo(() => {
    const shots = shotsReceived || [];
    const resultMap = new Map(shots.map((s) => [`${s.row},${s.col}`, s.result]));
    const directions = [[0, 1], [0, -1], [1, 0], [-1, 0]];
    const visited = new Set();
    const ships = [];

    shots.filter((s) => s.result === 'SUNK' && s.sunkShipType).forEach((s) => {
      const key = `${s.row},${s.col}`;
      if (visited.has(key)) return;

      // Flood-fill to find all cells of this ship
      const shipCells = [{ row: s.row, col: s.col }];
      visited.add(key);

      for (const [dr, dc] of directions) {
        let r = s.row + dr;
        let c = s.col + dc;
        while (r >= 0 && r < 10 && c >= 0 && c < 10) {
          const cellKey = `${r},${c}`;
          const res = resultMap.get(cellKey);
          if ((res === 'HIT' || res === 'SUNK') && !visited.has(cellKey)) {
            shipCells.push({ row: r, col: c });
            visited.add(cellKey);
            r += dr;
            c += dc;
          } else {
            break;
          }
        }
      }

      // Determine origin and orientation from cells
      const rows = shipCells.map((c) => c.row);
      const cols = shipCells.map((c) => c.col);
      const minRow = Math.min(...rows);
      const minCol = Math.min(...cols);
      const isVertical = new Set(cols).size === 1;

      ships.push({
        shipType: s.sunkShipType,
        originRow: minRow,
        originCol: minCol,
        orientation: isVertical ? 'VERTICAL' : 'HORIZONTAL',
        size: SHIP_SIZES[s.sunkShipType] || shipCells.length,
      });
    });

    return ships;
  }, [shotsReceived]);

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

            {/* Sunk ship sprites overlay */}
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
