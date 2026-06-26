import { useEffect, useState, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import useGame from '../hooks/useGame';
import useWebSocket from '../hooks/useWebSocket';
import WaitingScreen from '../components/GameStatus/WaitingScreen';
import TurnIndicator from '../components/GameStatus/TurnIndicator';
import PlacementBoard from '../components/Board/PlacementBoard';
import MyBoard from '../components/Board/MyBoard';
import OpponentBoard from '../components/Board/OpponentBoard';
import Toast from '../components/shared/Toast';
import { GAME_STATUS } from '../constants/ships';

const GamePage = () => {
  const { id: gameId } = useParams();
  const { user, token } = useAuth();
  const navigate = useNavigate();
  const [toast, setToast] = useState(null);
  const [boardConfirmed, setBoardConfirmed] = useState(false);
  const subscribedRef = useRef(false);

  const { game, loading, error, fetchGame, handleShotResult, handleOpponentShot, handleStateUpdate } = useGame(gameId);

  const { subscribe, publish, connected } = useWebSocket({
    token,
    onReconnect: fetchGame,
  });

  useEffect(() => { fetchGame(); }, [fetchGame]);

  // Poll while WAITING (backend doesn't send WS event when player2 joins)
  // Also poll while PLACING with board confirmed (waiting for opponent to place)
  useEffect(() => {
    if (!game) return;
    const shouldPoll =
      game.status === GAME_STATUS.WAITING ||
      (game.status === GAME_STATUS.PLACING && (boardConfirmed || game.myBoard?.ready));
    if (!shouldPoll) return;
    const interval = setInterval(fetchGame, 3000);
    return () => clearInterval(interval);
  }, [game?.status, boardConfirmed, game?.myBoard?.ready, fetchGame]);

  useEffect(() => {
    if (!connected || !gameId || subscribedRef.current) return;
    subscribedRef.current = true;

    subscribe(`/topic/game/${gameId}/player-joined`, () => fetchGame());
    subscribe(`/topic/game/${gameId}/state`, (payload) => {
      handleStateUpdate(payload);
      fetchGame();
    });
    subscribe('/user/queue/game/started', () => fetchGame());
    subscribe('/user/queue/game/shot-result', (payload) => {
      handleShotResult(payload);
      if (payload.sunkShipType) setToast({ message: `Você afundou o ${payload.sunkShipType} do oponente! 💥`, type: 'success' });
    });
    subscribe('/user/queue/game/opponent-shot', (payload) => {
      handleOpponentShot(payload);
      if (payload.sunkShipType) setToast({ message: `Seu ${payload.sunkShipType} foi afundado! 🚢`, type: 'error' });
    });
    subscribe('/user/queue/errors', (payload) => {
      setToast({ message: payload.message, type: 'error' });
    });
  }, [connected, gameId]);

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-900 flex items-center justify-center">
        <div className="w-10 h-10 border-4 border-blue-400 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-slate-900 flex flex-col items-center justify-center gap-4">
        <p className="text-red-400">{error}</p>
        <button onClick={() => navigate('/lobby')} className="text-blue-400 hover:underline">Voltar ao Lobby</button>
      </div>
    );
  }

  const renderContent = () => {
    switch (game.status) {
      case GAME_STATUS.WAITING:
        return <WaitingScreen />;

      case GAME_STATUS.PLACING:
        if (boardConfirmed || game.myBoard?.ready) {
          return (
            <div className="flex flex-col items-center gap-4 py-16">
              <div className="w-10 h-10 border-4 border-green-400 border-t-transparent rounded-full animate-spin" />
              <p className="text-slate-300">Frota confirmada! Aguardando oponente posicionar...</p>
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
        const opponent = game.player1?.id === user.id ? game.player2 : game.player1;
        return (
          <div>
            <TurnIndicator isMyTurn={isMyTurn} opponentName={opponent?.username} />
            <div className="flex flex-col md:flex-row gap-6 justify-center items-start">
              <MyBoard cells={game.myBoard?.cells} ships={game.myBoard?.ships} />
              <OpponentBoard
                shotsReceived={game.opponentBoard?.shotsReceived}
                isMyTurn={isMyTurn}
                onFire={(row, col) => publish(`/app/game/${gameId}/fire`, { row, col })}
              />
            </div>
          </div>
        );
      }

      case GAME_STATUS.FINISHED:
        // Implemented in TASK 7
        return (
          <div className="text-center py-16">
            <p className="text-2xl text-white mb-4">
              {game.winnerId === user.id ? 'Você venceu! 🏆' : 'Você perdeu. 😔'}
            </p>
            <button onClick={() => navigate('/lobby')} className="text-blue-400 hover:underline">Jogar Novamente</button>
          </div>
        );

      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 p-6">
      <div className="max-w-4xl mx-auto">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-blue-400 font-bold">⚓ Batalha Naval</h1>
          <span className="text-slate-500 text-xs">ID: {gameId?.slice(0, 8)}</span>
        </div>
        {renderContent()}
      </div>
      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
    </div>
  );
};

export default GamePage;
