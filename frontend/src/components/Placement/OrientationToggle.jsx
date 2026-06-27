import { ORIENTATIONS } from '../../constants/ships';

const OrientationToggle = ({ orientation, onChange }) => (
  <div className="flex justify-between items-center bg-surface-container-high p-2 border border-outline-variant mt-3">
    <span className="font-label-caps text-label-caps text-on-surface-variant">ORIENTATION</span>
    <div className="flex gap-2">
      <button
        onClick={() => onChange(ORIENTATIONS.HORIZONTAL)}
        className={`p-1 border flex items-center justify-center w-8 h-8 transition-all ${orientation === ORIENTATIONS.HORIZONTAL ? 'border-primary bg-primary-container/20 text-primary radar-glow' : 'border-outline-variant text-on-surface-variant hover:text-primary'}`}
      >
        <span className="material-symbols-outlined text-sm">swap_horiz</span>
      </button>
      <button
        onClick={() => onChange(ORIENTATIONS.VERTICAL)}
        className={`p-1 border flex items-center justify-center w-8 h-8 transition-all ${orientation === ORIENTATIONS.VERTICAL ? 'border-primary bg-primary-container/20 text-primary radar-glow' : 'border-outline-variant text-on-surface-variant hover:text-primary'}`}
      >
        <span className="material-symbols-outlined text-sm">swap_vert</span>
      </button>
    </div>
  </div>
);

export default OrientationToggle;
