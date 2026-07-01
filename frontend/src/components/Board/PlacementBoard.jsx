import { useState, useMemo, useEffect } from 'react';
import BoardCell from './BoardCell';
import ShipSelector from '../Placement/ShipSelector';
import OrientationToggle from '../Placement/OrientationToggle';
import { ORIENTATIONS } from '../../constants/ships';
import * as gameApi from '../../api/gameApi';

const PlacementBoard = ({ gameId, onConfirmed }) => {
  const [fleet, setFleet] = useState([]);
  const [placedShips, setPlacedShips] = useState([]);
  const [selectedType, setSelectedType] = useState(null);
  const [orientation, setOrientation] = useState(ORIENTATIONS.HORIZONTAL);
  const [hoverCells, setHoverCells] = useState([]);
  const [hoverValid, setHoverValid] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    gameApi.getFleetConfig()
      .then(({ data }) => {
        setFleet(data.map((s) => ({ type: s.type, size: s.size, label: s.name })));
      })
      .catch(() => {
        // Fallback to avoid being stuck if endpoint fails
        setFleet([
          { type: 'CARRIER', size: 5, label: 'Porta-aviões' },
          { type: 'BATTLESHIP', size: 4, label: 'Encouraçado' },
          { type: 'CRUISER', size: 3, label: 'Cruzador' },
          { type: 'SUBMARINE', size: 3, label: 'Submarino' },
          { type: 'DESTROYER', size: 2, label: 'Contratorpedeiro' },
        ]);
      });
  }, []);

  const selectedShip = fleet.find((s) => s.type === selectedType);

  const occupiedCells = useMemo(() => {
    const set = new Set();
    placedShips.forEach((ship) => {
      const size = fleet.find((s) => s.type === ship.type)?.size || 0;
      for (let i = 0; i < size; i++) {
        const r = ship.orientation === ORIENTATIONS.VERTICAL ? ship.originRow + i : ship.originRow;
        const c = ship.orientation === ORIENTATIONS.HORIZONTAL ? ship.originCol + i : ship.originCol;
        set.add(`${r},${c}`);
      }
    });
    return set;
  }, [placedShips, fleet]);

  const getCellsForPlacement = (row, col, ship, orient) => {
    const cells = [];
    for (let i = 0; i < ship.size; i++) {
      const r = orient === ORIENTATIONS.VERTICAL ? row + i : row;
      const c = orient === ORIENTATIONS.HORIZONTAL ? col + i : col;
      cells.push({ row: r, col: c });
    }
    return cells;
  };

  const handleMouseEnter = (row, col) => {
    if (!selectedShip) return;
    const cells = getCellsForPlacement(row, col, selectedShip, orientation);
    const valid = cells.every((c) => c.row < 10 && c.col < 10);
    setHoverCells(cells);
    setHoverValid(valid);
  };

  const handleMouseLeave = () => setHoverCells([]);

  const handleClick = (row, col) => {
    if (!selectedShip) return;
    const cells = getCellsForPlacement(row, col, selectedShip, orientation);
    if (!cells.every((c) => c.row < 10 && c.col < 10)) return;

    const newShip = { type: selectedShip.type, originRow: row, originCol: col, orientation };
    setPlacedShips((prev) => [...prev.filter((s) => s.type !== selectedShip.type), newShip]);
    setSelectedType(null);
    setHoverCells([]);
  };

  const handleRemove = (type) => {
    setPlacedShips((prev) => prev.filter((s) => s.type !== type));
  };

  const handleConfirm = async () => {
    setError('');
    setLoading(true);
    try {
      await gameApi.placeShips(gameId, placedShips.map(({ type, originRow, originCol, orientation: o }) => ({
        shipType: type, originRow, originCol, orientation: o,
      })));
      onConfirmed();
    } catch (err) {
      setError(err.response?.data?.message || 'Erro ao confirmar frota');
    } finally {
      setLoading(false);
    }
  };

  const getCellState = (row, col) => {
    const key = `${row},${col}`;
    const isHover = hoverCells.some((c) => c.row === row && c.col === col);
    if (isHover) return hoverValid ? 'preview' : 'preview-invalid';
    if (occupiedCells.has(key)) return 'ship';
    return 'empty';
  };

  const ROWS = ['A','B','C','D','E','F','G','H','I','J'];
  const fleetSize = fleet.length;

  if (fleet.length === 0) {
    return (
      <div className="flex items-center justify-center py-16">
        <p className="font-mono-data text-mono-data text-primary animate-pulse">CARREGANDO CONFIGURAÇÃO DE FROTA...</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col md:flex-row gap-6 items-start justify-center">
      {/* Painel de controle */}
      <section className="w-full md:w-80 flex flex-col bg-surface-container border border-outline-variant p-panel-padding gap-4">
        <header className="border-b border-outline-variant pb-2">
          <h2 className="font-headline-md text-headline-md text-primary tracking-widest whitespace-nowrap" style={{ fontSize: 'clamp(0.85rem, 2vw, 1.125rem)' }}>
            [ POSICIONAR FROTA ]
          </h2>
        </header>

        <OrientationToggle orientation={orientation} onChange={setOrientation} />

        <div className="flex-1 overflow-y-auto">
          <ShipSelector
            fleet={fleet}
            selectedType={selectedType}
            placedShips={placedShips}
            onSelect={setSelectedType}
            onRemove={handleRemove}
          />
        </div>

        {/* Footer: status + botão */}
        <div className="mt-auto pt-4 border-t border-outline-variant flex flex-col gap-2">
          <div className="flex justify-between font-mono-data text-mono-data text-primary">
            <span>STATUS:</span>
            <span>{placedShips.length}/{fleetSize} NAVIOS POSICIONADOS</span>
          </div>
          <div className="w-full h-1 bg-surface-container-highest">
            <div className="h-full bg-primary transition-all duration-300" style={{ width: `${(placedShips.length / fleetSize) * 100}%` }} />
          </div>
          <button
            onClick={handleConfirm}
            disabled={placedShips.length < fleetSize || loading}
            className={`w-full py-3 mt-2 font-label-caps text-label-caps uppercase transition-all ${
              placedShips.length === fleetSize && !loading
                ? 'border border-primary text-primary hover:bg-primary-container/20 radar-glow'
                : 'border border-outline-variant text-on-surface-variant cursor-not-allowed'
            }`}
          >
            {loading ? 'CONFIRMANDO...' : 'CONFIRMAR FROTA'}
          </button>
          {error && <p className="font-mono-data text-mono-data text-error border-l-2 border-error pl-3 mt-2">{error}</p>}
        </div>
      </section>

      {/* Área do tabuleiro */}
      <section className="flex-1 flex flex-col items-center justify-center relative">
        {/* Fundo decorativo */}
        <div className="absolute inset-0 pointer-events-none z-0 opacity-20">
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 rounded-full border border-primary/20" />
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-64 h-64 rounded-full border border-primary/30" />
          <div className="absolute top-1/2 left-1/2 w-full h-[1px] bg-primary/20 -translate-y-1/2 -translate-x-1/2" />
          <div className="absolute top-1/2 left-1/2 h-full w-[1px] bg-primary/20 -translate-x-1/2 -translate-y-1/2" />
        </div>

        <div className="relative z-10 bg-surface-container border-2 border-outline-variant p-4 radar-glow-intense">
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

            {/* Grid 10x10 */}
            <div className="grid grid-cols-10 gap-grid-gap bg-outline-variant/50 border border-outline-variant">
              {Array.from({ length: 100 }, (_, i) => {
                const row = Math.floor(i / 10);
                const col = i % 10;
                return (
                  <BoardCell
                    key={i}
                    state={getCellState(row, col)}
                    onClick={() => handleClick(row, col)}
                    onMouseEnter={() => handleMouseEnter(row, col)}
                    onMouseLeave={handleMouseLeave}
                    style={{ cursor: selectedShip ? 'crosshair' : 'default' }}
                  />
                );
              })}
            </div>
          </div>
        </div>

        <div className="mt-4 font-mono-data text-mono-data text-primary-fixed-dim animate-pulse">
          &gt; AGUARDANDO COORDENADAS DE GRID...
        </div>
      </section>
    </div>
  );
};

export default PlacementBoard;
