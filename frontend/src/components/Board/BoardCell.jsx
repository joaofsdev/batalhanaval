const CELL_STYLES = {
  empty: '',
  ship: 'placed',
  preview: 'preview',
  'preview-invalid': 'preview-invalid',
  hit: 'hit',
  miss: 'miss',
  sunk: 'sunk',
};

const BoardCell = ({ state = 'empty', onClick, onMouseEnter, onMouseLeave, style }) => (
  <div
    className={`w-8 h-8 bg-surface-container border border-outline-variant/30 grid-cell ${CELL_STYLES[state] || ''}`}
    onClick={onClick}
    onMouseEnter={onMouseEnter}
    onMouseLeave={onMouseLeave}
    style={style}
  />
);

export default BoardCell;
