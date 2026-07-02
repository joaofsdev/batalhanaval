import { useReducer, useCallback } from 'react';
import * as gameApi from '../api/gameApi';

const initialState = { game: null, loading: true, error: null };

const reducer = (state, action) => {
  switch (action.type) {
    case 'SET_GAME':
      return { game: action.payload, loading: false, error: null };
    case 'SET_ERROR':
      return { ...state, loading: false, error: action.payload };
    case 'UPDATE_STATUS':
      return {
        ...state,
        game: {
          ...state.game,
          status: action.payload.status,
          currentTurnPlayerId: action.payload.currentTurnPlayerId,
          winnerId: action.payload.winnerId ?? state.game.winnerId,
        },
      };
    case 'ADD_SHOT_RESULT': {
      const shot = action.payload;
      return {
        ...state,
        game: {
          ...state.game,
          opponentBoard: {
            ...state.game.opponentBoard,
            shotsReceived: [...state.game.opponentBoard.shotsReceived, shot],
          },
        },
      };
    }
    case 'ADD_OPPONENT_SHOT': {
      const { row, col, result } = action.payload;
      // Shield-blocked shots (MISS on a ship cell) should not mark the cell as hit
      // The server does not set cell.hit=true for shielded shots — fetchGame will confirm
      const shouldMarkHit = result !== 'MISS' || !state.game.myBoard.cells.find(
        (c) => c.row === row && c.col === col && c.hasShip
      );
      return {
        ...state,
        game: {
          ...state.game,
          myBoard: {
            ...state.game.myBoard,
            cells: state.game.myBoard.cells.map((c) =>
              c.row === row && c.col === col ? { ...c, hit: shouldMarkHit } : c
            ),
          },
        },
      };
    }
    default:
      return state;
  }
};

const useGame = (gameId) => {
  const [state, dispatch] = useReducer(reducer, initialState);

  const fetchGame = useCallback(async () => {
    try {
      const res = await gameApi.getGame(gameId);
      dispatch({ type: 'SET_GAME', payload: res.data });
    } catch (err) {
      dispatch({
        type: 'SET_ERROR',
        payload: err.response?.data?.message || 'Erro ao carregar partida',
      });
    }
  }, [gameId]);

  const handleShotResult = useCallback((payload) => {
    dispatch({ type: 'ADD_SHOT_RESULT', payload });
  }, []);

  const handleOpponentShot = useCallback((payload) => {
    dispatch({ type: 'ADD_OPPONENT_SHOT', payload });
  }, []);

  const handleStateUpdate = useCallback((payload) => {
    dispatch({ type: 'UPDATE_STATUS', payload });
  }, []);

  return {
    ...state,
    fetchGame,
    handleShotResult,
    handleOpponentShot,
    handleStateUpdate,
  };
};

export default useGame;
