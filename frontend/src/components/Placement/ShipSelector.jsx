const ShipSelector = ({ fleet, selectedType, placedShips, onSelect, onRemove }) => (
  <div className="flex flex-col gap-3">
    {fleet.map((ship) => {
      const isPlaced = placedShips.some((p) => p.type === ship.type);
      const isSelected = selectedType === ship.type;
      return (
        <div
          key={ship.type}
          onClick={() => !isPlaced && onSelect(ship.type)}
          className={`border p-3 transition-colors ${
            isSelected
              ? 'border-primary bg-primary-container/10 radar-glow cursor-pointer'
              : isPlaced
                ? 'border-outline-variant opacity-50 cursor-not-allowed'
                : 'border-outline-variant hover:border-primary cursor-pointer group'
          }`}
        >
          <div className="flex justify-between items-start mb-2">
            <span className={`font-mono-data text-mono-data font-bold ${
              isPlaced ? 'line-through text-on-surface-variant' : ''
            } ${isSelected ? 'text-primary' : 'text-on-surface group-hover:text-primary'}`}>
              {ship.label.toUpperCase()}
            </span>
            <div className="flex items-center gap-1">
              {isPlaced && (
                <>
                  <span className="font-label-caps text-label-caps text-secondary border border-secondary px-1 text-[10px]">PLACED</span>
                  <button
                    onClick={(e) => { e.stopPropagation(); onRemove(ship.type); }}
                    className="text-error hover:text-on-error transition-colors ml-1"
                  >
                    <span className="material-symbols-outlined text-sm">close</span>
                  </button>
                </>
              )}
              {isSelected && (
                <span className="font-label-caps text-label-caps text-primary border border-primary px-1 text-[10px]">SELECTED</span>
              )}
              {!isPlaced && !isSelected && (
                <span className="font-label-caps text-label-caps text-on-surface-variant border border-outline-variant px-1 text-[10px]">PENDING</span>
              )}
            </div>
          </div>

          <div className="flex gap-1">
            {Array.from({ length: ship.size }).map((_, i) => (
              <div key={i} className={`w-4 h-4 ${
                isPlaced ? 'bg-outline-variant'
                  : isSelected ? 'bg-primary border border-primary'
                  : 'border border-outline-variant group-hover:border-primary'
              }`} />
            ))}
          </div>
        </div>
      );
    })}
  </div>
);

export default ShipSelector;
