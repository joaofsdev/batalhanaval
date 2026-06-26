import { SHIP_FLEET } from '../../constants/ships';

const ShipSelector = ({ selectedType, placedShips, onSelect, onRemove }) => (
  <div className="space-y-2">
    <h3 className="text-slate-300 font-medium text-sm mb-2">Navios</h3>
    {SHIP_FLEET.map((ship) => {
      const isPlaced = placedShips.some((p) => p.type === ship.type);
      const isSelected = selectedType === ship.type;
      return (
        <div
          key={ship.type}
          className={`flex items-center justify-between px-3 py-2 rounded cursor-pointer transition-colors ${
            isSelected
              ? 'bg-blue-600 text-white'
              : isPlaced
                ? 'bg-green-900/40 text-green-400'
                : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
          }`}
          onClick={() => onSelect(ship.type)}
        >
          <span className="text-sm">
            {ship.label} ({ship.size})
          </span>
          {isPlaced && (
            <button
              onClick={(e) => { e.stopPropagation(); onRemove(ship.type); }}
              className="text-red-400 hover:text-red-300 text-xs ml-2"
            >
              ✕
            </button>
          )}
        </div>
      );
    })}
  </div>
);

export default ShipSelector;
