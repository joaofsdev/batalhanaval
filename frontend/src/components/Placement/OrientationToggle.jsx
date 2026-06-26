import { ORIENTATIONS } from '../../constants/ships';

const OrientationToggle = ({ orientation, onChange }) => (
  <div className="flex gap-1 mt-3">
    <button
      onClick={() => onChange(ORIENTATIONS.HORIZONTAL)}
      className={`flex-1 py-1 text-sm rounded ${orientation === ORIENTATIONS.HORIZONTAL ? 'bg-blue-600 text-white' : 'bg-slate-700 text-slate-400'}`}
    >
      H
    </button>
    <button
      onClick={() => onChange(ORIENTATIONS.VERTICAL)}
      className={`flex-1 py-1 text-sm rounded ${orientation === ORIENTATIONS.VERTICAL ? 'bg-blue-600 text-white' : 'bg-slate-700 text-slate-400'}`}
    >
      V
    </button>
  </div>
);

export default OrientationToggle;
