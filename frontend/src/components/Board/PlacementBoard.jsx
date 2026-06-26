import { useState, useMemo } from 'react';
import BoardCell from './BoardCell';
import ShipSelector from '../Placement/ShipSelector';
import OrientationToggle from '../Placement/OrientationToggle';
import { SHIP_FLEET, ORIENTATIONS } from '../../constants/ships';
import * as gameApi from '../../api/gameApi';

const PlacementBoard = ({ gameId, onConfirmed }) => {
  const [placedShips, setPlacedShips] = useState([]);
  const [selectedType, setSelectedType] = useState(null);
  const [orientation, setOrientation] = useState(ORIENTATIONS.HORIZONTAL);
  const [hoverCells, setHoverCells] = useState([]);
  const [hoverValid, setHoverValid] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const selectedShip = SHIP_FLEET.find((s) => s.type === selectedType);

  const occupiedCells = useMemo(() => {
    const set = new Set();
    placedShips.forEach((ship) => {
      const size = SHIP_FLEET.find((s) => s.type === ship.type).size;
      for (let i = 0; i < size; i++) {
        const r = ship.orientation === ORIENTATIONS.VERTICAL ? ship.originRow + i : ship.originRow;
        const c = ship.orientation === ORIENTATIONS.HORIZONTAL ? ship.originCol + i : ship.originCol;
        set.add(`${r},${c}`);
      }
    });
    return set;
  }, [placedShips]);

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

  return (
    <div className="flex flex-col md:flex-row gap-6 items-start justify-center">
      <div className="w-48">
        <ShipSelector
          selectedType={selectedType}
          placedShips={placedShips}
          onSelect={setSelectedType}
          onRemove={handleRemove}
        />
        <OrientationToggle orientation={orientation} onChange={setOrientation} />
        <button
          onClick={handleConfirm}
          disabled={placedShips.length < 5 || loading}
          className="w-full mt-4 py-2 bg-green-600 hover:bg-green-700 disabled:bg-slate-600 text-white text-sm font-medium rounded transition-colors"
        >
          {loading ? 'Enviando...' : 'Confirmar Frota'}
        </button>
        {error && <p className="text-red-400 text-xs mt-2">{error}</p>}
      </div>

      <div>
        <div className="grid grid-cols-10 gap-0">
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
              />
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default PlacementBoard;
