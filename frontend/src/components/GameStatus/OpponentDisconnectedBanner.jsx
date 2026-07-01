import { useState, useEffect, useRef } from 'react';

const OpponentDisconnectedBanner = ({ gracePeriodSeconds, onExpired }) => {
  const [secondsLeft, setSecondsLeft] = useState(gracePeriodSeconds);
  const intervalRef = useRef(null);

  useEffect(() => {
    setSecondsLeft(gracePeriodSeconds);
    intervalRef.current = setInterval(() => {
      setSecondsLeft((prev) => {
        if (prev <= 1) {
          clearInterval(intervalRef.current);
          onExpired?.();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(intervalRef.current);
  }, [gracePeriodSeconds]);

  return (
    <div className="fixed top-14 left-1/2 -translate-x-1/2 z-50 flex items-center gap-3 px-6 py-3 bg-surface-container border border-error/50 shadow-[0_0_20px_rgba(239,68,68,0.3)]">
      <span className="material-symbols-outlined text-error text-sm animate-pulse">wifi_off</span>
      <span className="font-mono-data text-mono-data text-error">
        OPONENTE DESCONECTADO — RECONEXÃO EM {secondsLeft}s
      </span>
      <div className="w-16 h-1 bg-surface-container-highest">
        <div
          className="h-full bg-error transition-all duration-1000 ease-linear"
          style={{ width: `${(secondsLeft / gracePeriodSeconds) * 100}%` }}
        />
      </div>
    </div>
  );
};

export default OpponentDisconnectedBanner;
