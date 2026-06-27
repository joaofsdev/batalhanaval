import { useEffect, useState, useCallback, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import useGame from "../hooks/useGame";
import useWebSocket from "../hooks/useWebSocket";
import WaitingScreen from "../components/GameStatus/WaitingScreen";
import TurnIndicator from "../components/GameStatus/TurnIndicator";
import PlacementBoard from "../components/Board/PlacementBoard";
import MyBoard from "../components/Board/MyBoard";
import OpponentBoard from "../components/Board/OpponentBoard";
import Toast from "../components/shared/Toast";
import GameOverOverlay from "../components/GameStatus/GameOverOverlay";
import { GAME_STATUS } from "../constants/ships";

const GamePage = () => {
  const { id: gameId } = useParams();
  const { user, token } = useAuth();
  const navigate = useNavigate();
  const [toast, setToast] = useState(null);
  const [boardConfirmed, setBoardConfirmed] = useState(false);
  const [cancelDisabled, setCancelDisabled] = useState(false);
  const subscribedRef = useRef(false);

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

  useEffect(() => {
    fetchGame();
  }, [fetchGame]);

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

    subscribe(`/topic/game/${gameId}/player-joined`, () => { setCancelDisabled(true); fetchGame(); });
    subscribe(`/topic/game/${gameId}/state`, (payload) => {
      handleStateUpdate(payload);
      fetchGame();
    });
    subscribe("/user/queue/game/started", () => fetchGame());
    subscribe("/user/queue/game/shot-result", (payload) => {
      handleShotResult(payload);
      if (payload.sunkShipType)
        setToast({
          message: `Você afundou o ${payload.sunkShipType} do oponente! 💥`,
          type: "success",
        });
    });
    subscribe("/user/queue/game/opponent-shot", (payload) => {
      handleOpponentShot(payload);
      if (payload.sunkShipType)
        setToast({
          message: `Seu ${payload.sunkShipType} foi afundado! 🚢`,
          type: "error",
        });
    });
    subscribe("/user/queue/errors", (payload) => {
      setToast({ message: payload.message, type: "error" });
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
      await new Promise(resolve => setTimeout(resolve, 400));
    } catch (e) {
      // silenciar — o backend trata os dois cenários
      await new Promise(resolve => setTimeout(resolve, 400));
    } finally {
      navigate('/lobby');
    }
  };

  const renderContent = () => {
    switch (game.status) {
      case GAME_STATUS.WAITING:
        return <WaitingScreen gameId={gameId} myUsername={user?.username} onCancel={handleCancelSearch} canCancel={!cancelDisabled} />;

      case GAME_STATUS.PLACING:
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
          <PlacementBoard
            gameId={gameId}
            onConfirmed={() => setBoardConfirmed(true)}
          />
        );

      case GAME_STATUS.IN_PROGRESS: {
        const isMyTurn = game.currentTurnPlayerId === user.id;
        const opponent =
          game.player1?.id === user.id ? game.player2 : game.player1;
        return (
          <div className="flex flex-col gap-4">
            <TurnIndicator
              isMyTurn={isMyTurn}
              opponentName={opponent?.username}
            />
            <div className="flex flex-col md:flex-row gap-6 justify-center items-start">
              <MyBoard
                cells={game.myBoard?.cells}
                ships={game.myBoard?.ships}
              />
              <OpponentBoard
                shotsReceived={game.opponentBoard?.shotsReceived}
                isMyTurn={isMyTurn}
                onFire={(row, col) =>
                  publish(`/app/game/${gameId}/fire`, { row, col })
                }
              />
            </div>
          </div>
        );
      }

      case GAME_STATUS.FINISHED:
        return (
          <GameOverOverlay
            isWinner={game.winnerId === user.id}
            onNewGame={() => navigate("/lobby")}
          />
        );

      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-background flex flex-col">
      {/* Header */}
      <header className="flex justify-between items-center px-panel-padding h-12 bg-surface-container-low border-b border-outline-variant z-40">
        <h1 className="font-headline-md text-headline-md text-primary tracking-widest glow-text">
          Aegis Command
        </h1>
        <div className="flex items-center gap-3">
          <span className="font-mono-data text-mono-data text-on-surface-variant">
            MISSÃO #{gameId?.slice(0, 8).toUpperCase()}
          </span>
        </div>
      </header>

      {/* Content */}
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
    </div>
  );
};

export default GamePage;
