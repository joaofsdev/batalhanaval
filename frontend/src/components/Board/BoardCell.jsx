const CELL_STYLES = {
  empty: 'bg-slate-700 hover:bg-slate-600',
  ship: 'bg-blue-500',
  preview: 'bg-blue-400/50',
  'preview-invalid': 'bg-red-400/50',
  hit: 'bg-orange-500',
  miss: 'bg-slate-500',
  sunk: 'bg-red-700',
};

const BoardCell = ({ state = 'empty', onClick, onMouseEnter, onMouseLeave, style }) => (
  <div
    className={`w-8 h-8 border border-slate-600 transition-colors ${CELL_STYLES[state] || CELL_STYLES.empty}`}
    onClick={onClick}
    onMouseEnter={onMouseEnter}
    onMouseLeave={onMouseLeave}
    style={style}
  />
);

export default BoardCell;
