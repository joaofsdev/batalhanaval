import { useEffect, useState, useRef, useCallback } from 'react';

const TURN_TIMEOUT_SECONDS = 60;

const TurnIndicator = ({ isMyTurn, opponentName, muted, onTimeout }) => {
  const [secondsLeft, setSecondsLeft] = useState(TURN_TIMEOUT_SECONDS);
  const intervalRef = useRef(null);
  const audioCtxRef = useRef(null);
  const urgencyPlayedRef = useRef(false);

  useEffect(() => {
    setSecondsLeft(TURN_TIMEOUT_SECONDS);
    urgencyPlayedRef.current = false;

    intervalRef.current = setInterval(() => {
      setSecondsLeft((prev) => {
        const next = Math.max(0, prev - 1);
        if (next === 0) {
          clearInterval(intervalRef.current);
        }
        return next;
      });
    }, 1000);

    return () => clearInterval(intervalRef.current);
  }, []);

  useEffect(() => {
    if (secondsLeft === 0 && isMyTurn) {
      onTimeout?.();
    }
  }, [secondsLeft, isMyTurn, onTimeout]);

  const playUrgencyBeep = useCallback(() => {
    if (muted || urgencyPlayedRef.current) return;
    urgencyPlayedRef.current = true;

    try {
      const ctx = audioCtxRef.current || new (window.AudioContext || window.webkitAudioContext)();
      audioCtxRef.current = ctx;
      if (ctx.state === 'suspended') ctx.resume();

      const playBeep = (startTime) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.type = 'square';
        osc.frequency.setValueAtTime(660, startTime);
        gain.gain.setValueAtTime(0.25, startTime);
        gain.gain.exponentialRampToValueAtTime(0.01, startTime + 0.12);
        osc.start(startTime);
        osc.stop(startTime + 0.12);
      };

      playBeep(ctx.currentTime);
      playBeep(ctx.currentTime + 0.18);
    } catch {
    }
  }, [muted]);

  useEffect(() => {
    if (secondsLeft === 10 && isMyTurn) {
      playUrgencyBeep();
    }
  }, [secondsLeft, isMyTurn, playUrgencyBeep]);

  const isUrgent = isMyTurn && secondsLeft <= 20;
  const isCritical = isMyTurn && secondsLeft <= 10;
  const isDanger = isMyTurn && secondsLeft <= 5;

  const timerColor = isCritical ? 'text-error' : isUrgent ? 'text-tertiary' : 'text-primary';
  const timerText = `${String(Math.floor(secondsLeft / 60)).padStart(2, '0')}:${String(secondsLeft % 60).padStart(2, '0')}`;

  const getBorderClass = () => {
    if (!isMyTurn) return 'border-outline-variant';
    if (isCritical) return 'border-error';
    if (isUrgent) return 'border-tertiary';
    return 'border-primary';
  };

  const getBgClass = () => {
    if (!isMyTurn) return '';
    if (isCritical) return 'bg-error/10';
    if (isUrgent) return 'bg-tertiary/5';
    return 'bg-primary-container/10 radar-glow';
  };

  return isMyTurn ? (
    <div className={`flex flex-col items-center gap-1 border px-4 py-2 transition-colors duration-300 ${getBorderClass()} ${getBgClass()} ${isCritical ? 'animate-shake' : ''}`}>
      <div className="flex items-center justify-center gap-3">
        <span className={`material-symbols-outlined text-sm ${isCritical ? 'text-error' : 'text-primary'}`}>my_location</span>
        <span className={`font-label-caps text-label-caps ${isCritical ? 'text-error' : isUrgent ? 'text-tertiary' : 'text-primary'} ${isUrgent ? 'animate-pulse' : ''}`}>
          SUA VEZ — ATAQUE!
        </span>
        <span className={`font-mono-data text-mono-data ${timerColor} tabular-nums`}>{timerText}</span>
      </div>
      {isDanger && (
        <span className="font-mono-data text-[11px] text-error animate-pulse">
          ⚠️ TURNO SERÁ PULADO!
        </span>
      )}
    </div>
  ) : (
    <div className={`flex items-center justify-center gap-3 border px-4 py-2 ${getBorderClass()}`}>
      <span className="material-symbols-outlined text-on-surface-variant text-sm animate-spin" style={{ animationDuration: '2s' }}>sync</span>
      <span className="font-label-caps text-label-caps text-on-surface-variant">
        AGUARDANDO {opponentName?.toUpperCase() || 'OPONENTE'}...
      </span>
      <span className={`font-mono-data text-mono-data text-on-surface-variant tabular-nums`}>{timerText}</span>
    </div>
  );
};

export default TurnIndicator;
