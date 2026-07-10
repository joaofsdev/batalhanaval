import { useState, useEffect } from 'react';

const PlacementTimer = ({ deadline }) => {
  const [remaining, setRemaining] = useState(null);
  const [expired, setExpired] = useState(false);

  useEffect(() => {
    if (!deadline) {
      setRemaining(null);
      setExpired(false);
      return;
    }

    const update = () => {
      const diff = Math.max(0, Math.floor((new Date(deadline).getTime() - Date.now()) / 1000));
      setRemaining(diff);
      if (diff <= 0) {
        setExpired(true);
      }
    };

    update();
    const interval = setInterval(update, 1000);
    return () => clearInterval(interval);
  }, [deadline]);

  if (remaining === null) return null;

  const minutes = Math.floor(remaining / 60);
  const seconds = remaining % 60;
  const formatted = `${minutes}:${seconds.toString().padStart(2, '0')}`;
  const isAlert = remaining <= 30 && remaining > 0;

  if (expired) {
    return (
      <div className="flex items-center gap-2 px-3 py-2 border border-error bg-error/10">
        <span className="material-symbols-outlined text-sm text-error">hourglass_disabled</span>
        <span className="font-mono-data text-mono-data text-error animate-pulse">
          VERIFICANDO STATUS...
        </span>
      </div>
    );
  }

  return (
    <div className={`flex items-center gap-2 px-3 py-2 border ${
      isAlert
        ? 'border-error bg-error/10'
        : 'border-outline-variant bg-surface-container-high'
    }`}>
      <span className={`material-symbols-outlined text-sm ${
        isAlert ? 'text-error' : 'text-on-surface-variant'
      }`}>
        timer
      </span>
      <span className={`font-mono-data text-mono-data ${
        isAlert ? 'text-error animate-pulse' : 'text-on-surface-variant'
      }`}>
        TEMPO: {formatted}
      </span>
    </div>
  );
};

export default PlacementTimer;
