import { useEffect, useState, useRef } from 'react';

const TURN_TIMEOUT_SECONDS = 60;

const TurnIndicator = ({ isMyTurn, opponentName }) => {
  const [secondsLeft, setSecondsLeft] = useState(TURN_TIMEOUT_SECONDS);
  const intervalRef = useRef(null);

  useEffect(() => {
    // Reset timer whenever this component re-mounts (via key change from parent)
    setSecondsLeft(TURN_TIMEOUT_SECONDS);

    intervalRef.current = setInterval(() => {
      setSecondsLeft((prev) => Math.max(0, prev - 1));
    }, 1000);

    return () => clearInterval(intervalRef.current);
  }, []);

  const timerColor = secondsLeft <= 10 ? 'text-error' : secondsLeft <= 20 ? 'text-tertiary' : 'text-primary';
  const timerText = `${String(Math.floor(secondsLeft / 60)).padStart(2, '0')}:${String(secondsLeft % 60).padStart(2, '0')}`;

  return isMyTurn ? (
    <div className="flex items-center justify-center gap-3 border border-primary px-4 py-2 bg-primary-container/10 radar-glow">
      <span className="material-symbols-outlined text-primary text-sm">my_location</span>
      <span className="font-label-caps text-label-caps text-primary">SUA VEZ — ATAQUE!</span>
      <span className={`font-mono-data text-mono-data ${timerColor} tabular-nums`}>{timerText}</span>
    </div>
  ) : (
    <div className="flex items-center justify-center gap-3 border border-outline-variant px-4 py-2">
      <span className="material-symbols-outlined text-on-surface-variant text-sm animate-spin" style={{ animationDuration: '2s' }}>sync</span>
      <span className="font-label-caps text-label-caps text-on-surface-variant">
        AGUARDANDO {opponentName?.toUpperCase() || 'OPONENTE'}...
      </span>
      <span className={`font-mono-data text-mono-data ${timerColor} tabular-nums`}>{timerText}</span>
    </div>
  );
};

export default TurnIndicator;
