import { useEffect, useRef, useState, useCallback } from 'react';

const useStormWebSocket = ({ gameId, subscribe, connected, setToast }) => {
  const [stormData, setStormData] = useState({
    turnsUntilStorm: null,
    isStormTurn: false,
    stormEvent: null,
  });
  const [abilityResult, setAbilityResult] = useState(null);
  const [abilityRotation, setAbilityRotation] = useState(null);
  const [fogActive, setFogActive] = useState(false);
  const [blockedRow, setBlockedRow] = useState(null);
  const [currentShake, setCurrentShake] = useState(false);
  const subscribedRef = useRef(false);
  const shakeTimerRef = useRef(null);

  useEffect(() => {
    if (!connected || !gameId || subscribedRef.current) return;
    subscribedRef.current = true;

    // Storm events
    subscribe(`/topic/game/${gameId}/storm`, (payload) => {
      // Backend sends StormEventNotification directly: { eventType, affectedAxis, message, shipMoved }
      const event = payload.eventType ? {
        type: payload.eventType,
        affectedAxis: payload.affectedAxis,
        message: payload.message,
        shipMoved: payload.shipMoved,
        affectedRow: payload.affectedAxis ? parseInt(payload.affectedAxis.replace('ROW_', ''), 10) : null,
      } : null;

      setStormData({
        turnsUntilStorm: null,
        isStormTurn: !!event,
        stormEvent: event,
      });

      if (event) {
        switch (event.type) {
          case 'FOG':
            setFogActive(true);
            break;
          case 'TIDE':
            setBlockedRow(event.affectedRow ?? null);
            break;
          case 'CURRENT':
            setCurrentShake(true);
            if (shakeTimerRef.current) clearTimeout(shakeTimerRef.current);
            shakeTimerRef.current = setTimeout(() => setCurrentShake(false), 600);
            setToast({ message: event.message || 'Corrente Marítima — posições alteradas', type: 'info' });
            break;
          case 'CALM':
            setToast({ message: 'Calmaria — tiro bônus concedido ao jogador da vez!', type: 'success' });
            break;
          default:
            break;
        }
      } else {
        // No active event — clear effects
        setFogActive(false);
        setBlockedRow(null);
      }
    });

    // Ability result (user-specific queue)
    subscribe('/user/queue/game/ability-result', (payload) => {
      setAbilityResult(payload);
    });

    // Ability rotation (user-specific queue)
    subscribe('/user/queue/game/ability-rotated', (payload) => {
      setAbilityRotation(payload);

      const toastMessage = payload.previousWasDiscarded
        ? `Habilidade rotacionada! Seu ${payload.previousAbilityName || payload.previousAbility} foi descartado. Nova habilidade: ${payload.newAbilityName}`
        : `Nova habilidade: ${payload.newAbilityName}`;

      setToast({ message: toastMessage, type: 'info' });
    });
  }, [connected, gameId, subscribe, setToast]);

  const clearFog = useCallback(() => setFogActive(false), []);
  const clearBlockedRow = useCallback(() => setBlockedRow(null), []);
  const syncFog = useCallback((active) => setFogActive(active), []);
  const syncStormState = useCallback((payload) => {
    setStormData((prev) => ({
      ...prev,
      isStormTurn: !!payload.isStormTurn,
      turnsUntilStorm: payload.turnsUntilStorm ?? prev.turnsUntilStorm,
    }));
  }, []);

  return {
    stormData,
    abilityResult,
    abilityRotation,
    fogActive,
    blockedRow,
    currentShake,
    clearFog,
    clearBlockedRow,
    syncFog,
    syncStormState,
  };
};

export default useStormWebSocket;
