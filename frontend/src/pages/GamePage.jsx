import { useEffect, useState, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import useGame from "../hooks/useGame";
import useWebSocket from "../hooks/useWebSocket";
import useSoundEffects from "../hooks/useSoundEffects";
import WaitingScreen from "../components/GameStatus/WaitingScreen";
import OpponentInfo from "../components/GameStatus/OpponentInfo";
import TurnIndicator from "../components/GameStatus/TurnIndicator";
import OpponentDisconnectedBanner from "../components/GameStatus/OpponentDisconnectedBanner";
import PlacementBoard from "../components/Board/PlacementBoard";
import MyBoard from "../components/Board/MyBoard";
import OpponentBoard from "../components/Board/OpponentBoard";
import Toast from "../components/shared/Toast";
import GameOverOverlay from "../components/GameStatus/GameOverOverlay";
import AbilityPanel from "../components/Storm/AbilityPanel";
import StormTracker from "../components/Storm/StormTracker";
import StormTutorial, { STORAGE_KEY as STORM_TUTORIAL_KEY } from "../components/Storm/StormTutorial";
import useStormWebSocket from "../hooks/useStormWebSocket";
import { GAME_STATUS } from "../constants/ships";
import * as gameApi from "../api/gameApi";
import * as adminApi from "../api/adminApi";

const GamePage = () => {
  const { id: gameId } = useParams();
  const { user, token } = useAuth();
  const navigate = useNavigate();
  const [toast, setToast] = useState(null);
  const [boardConfirmed, setBoardConfirmed] = useState(false);
  const [cancelDisabled, setCancelDisabled] = useState(false);
  const [rematchInvite, setRematchInvite] = useState(null);
  const [opponentDisconnected, setOpponentDisconnected] = useState(null);
  const [opponentFoundDelayActive, setOpponentFoundDelayActive] = useState(false);
  const subscribedRef = useRef(false);
  const delayShownRef = useRef(false);

  const [adminBoardsRevealed, setAdminBoardsRevealed] = useState(false);
  const [adminRevealData, setAdminRevealData] = useState(null);
  const [adminRevealLoading, setAdminRevealLoading] = useState(false);

  const {
    game,
    loading,
    error,
    fetchGame,
    handleShotResult,
    handleOpponentShot,
    handleStateUpdate,
  } = useGame(gameId);

  const { subscribe, publish, connected } = useWebSocket({
    token,
    onReconnect: fetchGame,
  });

  const { playHit, playMiss, playSunk, muted, toggleMute } = useSoundEffects();

  const isStormMode = game?.gameMode === 'STORM';

  const { stormData, abilityResult, abilityRotation, fogActive, blockedRow, currentShake, syncFog, syncStormState } = useStormWebSocket({
    gameId: isStormMode ? gameId : null,
    subscribe,
    connected,
    setToast,
  });

  const [showTutorial, setShowTutorial] = useState(false);

  useEffect(() => {
    if (
      isStormMode &&
      game?.status === GAME_STATUS.IN_PROGRESS &&
      !localStorage.getItem(STORM_TUTORIAL_KEY)
    ) {
      setShowTutorial(true);
    }
  }, [isStormMode, game?.status]);

  useEffect(() => {
    setBoardConfirmed(false);
    setCancelDisabled(false);
    setRematchInvite(null);
    setOpponentDisconnected(null);
    setOpponentFoundDelayActive(false);
    setAdminBoardsRevealed(false);
    setAdminRevealData(null);
    subscribedRef.current = false;
    delayShownRef.current = false;
  }, [gameId]);

  useEffect(() => {
    fetchGame();
  }, [fetchGame]);

  useEffect(() => {
    if (!game) return;
    if (game.status === GAME_STATUS.PLACING && !delayShownRef.current) {
      delayShownRef.current = true;
      setOpponentFoundDelayActive(true);
      setTimeout(() => setOpponentFoundDelayActive(false), 3000);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [game?.status]);

  useEffect(() => {
    if (!game) return;
    const shouldPoll =
      game.status === GAME_STATUS.WAITING ||
      (game.status === GAME_STATUS.PLACING &&
        (boardConfirmed || game.myBoard?.ready));
    if (!shouldPoll) return;
    const interval = setInterval(fetchGame, 3000);
    return () => clearInterval(interval);
  }, [game?.status, boardConfirmed, game?.myBoard?.ready, fetchGame]);

  useEffect(() => {
    if (!connected || !gameId || subscribedRef.current) return;
    subscribedRef.current = true;

    subscribe(`/topic/game/${gameId}/player-joined`, () => {
      setCancelDisabled(true);
      fetchGame();
    });
    subscribe(`/topic/game/${gameId}/state`, (payload) => {
      handleStateUpdate(payload);
      if (payload.fogActive !== undefined) {
        syncFog(!!payload.fogActive);
      }
      if (payload.isStormTurn !== undefined) {
        syncStormState(payload);
      }
      fetchGame();
    });
    subscribe("/user/queue/game/started", () => fetchGame());
    subscribe("/user/queue/game/shot-result", (payload) => {
      handleShotResult(payload);
      if (payload.result === 'SUNK') playSunk();
      else if (payload.result === 'HIT') playHit();
      else if (payload.result === 'MISS') playMiss();
      if (payload.sunkShipType)
        setToast({
          message: `Você afundou o ${payload.sunkShipType} do oponente! 💥`,
          type: "success",
        });
    });
    subscribe("/user/queue/game/opponent-shot", (payload) => {
      handleOpponentShot(payload);
      if (payload.result === 'SUNK') playSunk();
      else if (payload.result === 'HIT') playHit();
      else if (payload.result === 'MISS') playMiss();
      if (payload.sunkShipType)
        setToast({
          message: `Seu ${payload.sunkShipType} foi afundado! 🚢`,
          type: "error",
        });
    });
    subscribe("/user/queue/errors", (payload) => {
      setToast({ message: payload.message, type: "error" });
    });
    subscribe("/user/queue/game/rematch-invite", (payload) => {
      setRematchInvite(payload);
    });
    subscribe(`/topic/game/${gameId}/rematch`, (payload) => {
      if (payload.status === 'MATCHED' && payload.gameId) {
        navigate(`/game/${payload.gameId}`);
      }
    });
    subscribe(`/topic/game/${gameId}/opponent-disconnected`, (payload) => {
      if (payload.type === 'DISCONNECTED') {
        setOpponentDisconnected(payload.gracePeriodSeconds);
      } else if (payload.type === 'RECONNECTED') {
        setOpponentDisconnected(null);
      }
    });
  }, [connected, gameId]);

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="relative w-16 h-16 flex items-center justify-center">
          <div className="absolute w-full h-full border border-primary-container rounded-full sonar-pulse" />
          <div className="w-3 h-3 bg-primary-container rounded-full shadow-[0_0_15px_rgba(34,211,238,1)]" />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center gap-4">
        <p className="font-mono-data text-mono-data text-error border-l-2 border-error pl-3">
          {error}
        </p>
        <button
          onClick={() => navigate("/lobby")}
          className="font-label-caps text-label-caps text-primary border-b border-primary/30 pb-1 hover:text-primary-fixed transition-colors"
        >
          VOLTAR AO LOBBY
        </button>
      </div>
    );
  }

  const handleCancelSearch = async () => {
    try {
      await gameApi.cancelGame(gameId);
    } catch (e) {
    } finally {
      navigate("/lobby");
    }
  };

  const handleAdminReveal = async () => {
    if (adminBoardsRevealed) {
      setAdminBoardsRevealed(false);
      return;
    }
    if (adminRevealData) {
      setAdminBoardsRevealed(true);
      return;
    }
    setAdminRevealLoading(true);
    try {
      const res = await adminApi.revealBoards(gameId);
      setAdminRevealData(res.data);
      setAdminBoardsRevealed(true);
    } catch {
      setToast({ message: "Erro ao revelar boards", type: "error" });
    } finally {
      setAdminRevealLoading(false);
    }
  };

  const handleSurrender = async () => {
    if (!window.confirm("Tem certeza que deseja desistir? Isso contará como derrota.")) return;
    try {
      await gameApi.surrender(gameId);
    } catch (e) {
      setToast({ message: "Erro ao desistir", type: "error" });
    }
  };

  const renderContent = () => {
    if (opponentFoundDelayActive) {
      return (
        <WaitingScreen
          gameId={gameId}
          myUsername={user?.username}
          onCancel={handleCancelSearch}
          canCancel={false}
          opponentFound
        />
      );
    }

    switch (game.status) {
      case GAME_STATUS.WAITING:
        return (
          <WaitingScreen
            gameId={gameId}
            myUsername={user?.username}
            onCancel={handleCancelSearch}
            canCancel={!cancelDisabled}
          />
        );

      case GAME_STATUS.PLACING: {
        const placingOpponent = game.player1?.id === user.id ? game.player2 : game.player1;
        if (boardConfirmed || game.myBoard?.ready) {
          return (
            <div className="flex flex-col items-center gap-6 py-16">
              <div className="relative w-16 h-16 flex items-center justify-center">
                <div className="absolute w-full h-full border border-primary-container rounded-full sonar-pulse" />
                <div className="w-3 h-3 bg-primary-container rounded-full shadow-[0_0_15px_rgba(34,211,238,1)]" />
              </div>
              <p className="font-mono-data text-mono-data text-primary animate-pulse">
                FROTA CONFIRMADA — AGUARDANDO OPONENTE POSICIONAR...
              </p>
            </div>
          );
        }
        return (
          <div className="flex flex-col gap-4">
            <OpponentInfo username={placingOpponent?.username} />
            <PlacementBoard
              gameId={gameId}
              onConfirmed={() => setBoardConfirmed(true)}
              deadline={game.placementDeadline}
            />
          </div>
        );
      }

      case GAME_STATUS.IN_PROGRESS: {
        const isMyTurn = game.currentTurnPlayerId === user.id;
        const opponent =
          game.player1?.id === user.id ? game.player2 : game.player1;
        return (
          <div className="flex flex-col gap-4">
            <OpponentInfo username={opponent?.username} />
            <TurnIndicator
              key={game.currentTurnPlayerId}
              isMyTurn={isMyTurn}
              opponentName={opponent?.username}
              muted={muted}
              onTimeout={() => setToast({ message: 'Turno pulado! Mais 2 e você perde por AFK.', type: 'error' })}
            />
            {isStormMode && (
              <StormTracker
                turnsUntilStorm={stormData.turnsUntilStorm}
                isStormTurn={stormData.isStormTurn}
                fogActive={fogActive}
                blockedRow={blockedRow}
              />
            )}
            <div className="flex flex-col md:flex-row gap-6 justify-center items-start">
              <MyBoard
                cells={game.myBoard?.cells}
                ships={game.myBoard?.ships}
                currentShake={currentShake}
              />
              <OpponentBoard
                shotsReceived={game.opponentBoard?.shotsReceived}
                isMyTurn={isMyTurn}
                onFire={(row, col) =>
                  publish(`/app/game/${gameId}/fire`, { row, col })
                }
                fogActive={fogActive}
                blockedRow={blockedRow}
                revealedShips={adminBoardsRevealed && adminRevealData ? (() => {
                  const opponentData = adminRevealData.player1?.playerId === user.id
                    ? adminRevealData.player2
                    : adminRevealData.player1;
                  return opponentData?.board?.ships || null;
                })() : null}
              />
            </div>
            {isStormMode && (
              <div className="flex justify-center">
                <AbilityPanel
                  gameId={gameId}
                  isMyTurn={isMyTurn}
                  isStormTurn={stormData.isStormTurn}
                  abilityResult={abilityResult}
                  abilityRotation={abilityRotation}
                  onUseAbility={(payload) =>
                    gameApi.useAbility(gameId, payload)
                  }
                />
              </div>
            )}
            <div className="flex justify-center mt-4">
              <button
                onClick={handleSurrender}
                className="px-6 py-2 border border-error text-error font-label-caps text-label-caps hover:bg-error/10 transition-all flex items-center gap-2"
              >
                <span className="material-symbols-outlined text-sm">flag</span>
                DESISTIR
              </button>
            </div>
          </div>
        );
      }

      case GAME_STATUS.FINISHED: {
        const myShots = game.opponentBoard?.shotsReceived || [];
        const totalShots = myShots.length;
        const totalHits = myShots.filter((s) => s.result === 'HIT' || s.result === 'SUNK').length;
        const accuracy = totalShots > 0 ? Math.round((totalHits / totalShots) * 100) : 0;
        const isCancelled = !game.winnerId;
        return (
          <GameOverOverlay
            isWinner={game.winnerId === user.id}
            isCancelled={isCancelled}
            stats={{ shots: totalShots, hits: totalHits, accuracy }}
            gameId={gameId}
            rematchInvite={rematchInvite}
            eloDelta={game.eloDelta}
            myBoard={game.myBoard}
            opponentBoard={game.opponentBoard}
          />
        );
      }

      case GAME_STATUS.CANCELLED: {
        return (
          <GameOverOverlay
            isWinner={false}
            isCancelled={true}
            stats={null}
            gameId={gameId}
            rematchInvite={null}
            eloDelta={null}
            myBoard={game.myBoard}
            opponentBoard={game.opponentBoard}
          />
        );
      }

      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-background flex flex-col">
      <header className="flex justify-between items-center px-panel-padding h-12 bg-surface-container-low border-b border-outline-variant z-40">
        <div className="flex items-center gap-3">
          <span className="font-mono-data text-mono-data text-on-surface-variant">
            MISSÃO #{gameId?.slice(0, 8).toUpperCase()}
          </span>
        </div>
        <div className="flex items-center gap-2">
          {user?.role === 'ADMIN' && game && (game.status === GAME_STATUS.IN_PROGRESS || game.status === GAME_STATUS.PLACING) && (
            <button
              onClick={handleAdminReveal}
              disabled={adminRevealLoading}
              className={`flex items-center gap-1 transition-colors disabled:opacity-50 ${
                adminBoardsRevealed
                  ? 'text-primary'
                  : 'text-on-surface-variant hover:text-primary'
              }`}
              title={adminBoardsRevealed ? 'Ocultar boards' : 'Admin: Revelar boards'}
            >
              <span className="material-symbols-outlined text-sm">
                {adminBoardsRevealed ? 'visibility' : 'admin_panel_settings'}
              </span>
            </button>
          )}
          {isStormMode && (
            <button
              onClick={() => setShowTutorial(true)}
              className="flex items-center gap-1 text-on-surface-variant hover:text-primary transition-colors"
              title="Tutorial Modo Tempestade"
            >
              <span className="material-symbols-outlined text-sm">help</span>
            </button>
          )}
          <button
            onClick={toggleMute}
            className="flex items-center gap-1 text-on-surface-variant hover:text-primary transition-colors"
            title={muted ? 'Ativar sons' : 'Desativar sons'}
          >
            <span className="material-symbols-outlined text-sm">
              {muted ? 'volume_off' : 'volume_up'}
            </span>
          </button>
        </div>
      </header>

      {opponentDisconnected && (
        <OpponentDisconnectedBanner
          gracePeriodSeconds={opponentDisconnected}
          onExpired={() => setOpponentDisconnected(null)}
        />
      )}

      <div className="flex-1 flex items-center justify-center p-margin-safe">
        {renderContent()}
      </div>

      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}

      {isStormMode && (
        <StormTutorial
          open={showTutorial}
          onClose={() => setShowTutorial(false)}
        />
      )}
    </div>
  );
};

export default GamePage;
