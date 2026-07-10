import { useState, useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import useWebSocket from "../hooks/useWebSocket";
import * as gameApi from "../api/gameApi";

const RoomPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = localStorage.getItem("bn_token");

  // Mode from query param (default CLASSIC)
  const mode = searchParams.get("mode") || "CLASSIC";
  const joinToken = searchParams.get("token");

  const [view, setView] = useState("choosing"); // choosing | creating | joining | room
  const [room, setRoom] = useState(null);
  const [inputToken, setInputToken] = useState(joinToken || "");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [copied, setCopied] = useState(false);

  const handleRoomUpdate = useCallback(
    (data) => {
      if (data.status === "CANCELLED") {
        setError("A sala foi cancelada pelo criador.");
        setView("choosing");
        setRoom(null);
        return;
      }
      setRoom(data);

      // If both ready, navigate to game
      if (data.status === "STARTING") {
        setTimeout(() => navigate(`/game/${data.gameId}`), 1000);
      }
    },
    [navigate],
  );

  const { connected, subscribe } = useWebSocket({ token, onReconnect: null });

  // Subscribe to room updates when we have a room
  useEffect(() => {
    if (room?.gameId && connected) {
      subscribe(`/topic/room/${room.gameId}`, handleRoomUpdate);
    }
  }, [room?.gameId, connected, subscribe, handleRoomUpdate]);

  const handleCreateRoom = async () => {
    setError("");
    setLoading(true);
    try {
      const res = await gameApi.createRoom(mode);
      setRoom(res.data);
      setView("room");
    } catch (err) {
      const code = err.response?.data?.code;
      if (code === "PLAYER_ALREADY_IN_GAME") {
        setError("Você já está em uma partida ativa.");
      } else {
        setError(err.response?.data?.message || "Erro ao criar sala.");
      }
    } finally {
      setLoading(false);
    }
  };

  const handleJoinRoom = async (tokenValue) => {
    setError("");
    setLoading(true);
    try {
      const res = await gameApi.joinRoom(tokenValue || inputToken);
      setRoom(res.data);
      setView("room");
    } catch (err) {
      setError(err.response?.data?.message || "Erro ao entrar na sala.");
      setView("choosing");
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmReady = async () => {
    setError("");
    setLoading(true);
    try {
      const res = await gameApi.confirmReady(room.gameId);
      setRoom(res.data);
    } catch (err) {
      setError(err.response?.data?.message || "Erro ao confirmar.");
    } finally {
      setLoading(false);
    }
  };

  const handleCancelRoom = async () => {
    try {
      await gameApi.cancelRoom(room.gameId);
      setRoom(null);
      setView("choosing");
    } catch (err) {
      setError(err.response?.data?.message || "Erro ao cancelar sala.");
    }
  };

  const handleCopyToken = () => {
    navigator.clipboard.writeText(room.token);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const isHost = room?.hostUsername === user?.username;
  const isReady = isHost ? room?.hostReady : room?.guestReady;

  return (
    <div className="h-screen flex flex-col bg-background">
      {/* Header */}
      <header className="flex justify-between items-center w-full px-panel-padding h-16 z-40 bg-surface-container-low border-b border-outline-variant">
        <button
          onClick={() => navigate("/lobby")}
          className="flex items-center gap-2 text-on-surface-variant hover:text-primary transition-colors"
        >
          <span className="material-symbols-outlined">arrow_back</span>
          <span className="font-label-caps text-label-caps">VOLTAR</span>
        </button>
        <h1 className="font-headline-md text-headline-md text-primary tracking-widest">
          SALA PRIVADA
        </h1>
        <div className="w-20" />
      </header>

      {/* Content */}
      <main className="flex-1 flex items-center justify-center p-margin-safe">
        <div className="w-full max-w-md bg-surface-container border border-outline-variant p-panel-padding flex flex-col gap-6">
          {/* Choosing view: Create or Join */}
          {view === "choosing" && (
            <>
              <header className="border-b border-outline-variant pb-3 flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-sm">
                  meeting_room
                </span>
                <h2 className="font-headline-md text-headline-md text-primary tracking-widest">
                  SALA PRIVADA
                </h2>
              </header>

              <p className="font-body-md text-body-md text-on-surface-variant">
                Crie uma sala para jogar com um amigo ou entre usando um código
                de acesso.
              </p>

              <div className="flex flex-col gap-3">
                <button
                  onClick={handleCreateRoom}
                  disabled={loading}
                  className="w-full py-3 bg-surface-container-high border border-primary text-primary font-label-caps text-label-caps hover:bg-primary-container hover:text-on-primary-fixed transition-all radar-glow disabled:opacity-40"
                >
                  {loading ? "CRIANDO..." : `CRIAR SALA (${mode})`}
                </button>

                <div className="flex items-center gap-3">
                  <div className="flex-1 h-px bg-outline-variant" />
                  <span className="font-label-caps text-label-caps text-on-surface-variant">
                    OU
                  </span>
                  <div className="flex-1 h-px bg-outline-variant" />
                </div>

                <div className="flex gap-2">
                  <input
                    type="text"
                    value={inputToken}
                    onChange={(e) =>
                      setInputToken(e.target.value.toUpperCase())
                    }
                    placeholder="CÓDIGO DA SALA"
                    maxLength={6}
                    className="flex-1 px-4 py-3 bg-surface-container-high border border-outline-variant text-on-surface font-mono-data text-mono-data placeholder:text-on-surface-variant/50 focus:border-primary focus:outline-none tracking-widest text-center uppercase"
                  />
                  <button
                    onClick={() => handleJoinRoom()}
                    disabled={loading || inputToken.length < 6}
                    className="px-6 py-3 border border-primary text-primary font-label-caps text-label-caps hover:bg-primary-container hover:text-on-primary-fixed transition-all disabled:opacity-40"
                  >
                    ENTRAR
                  </button>
                </div>
              </div>
            </>
          )}

          {/* Room view: waiting or confirming */}
          {view === "room" && room && (
            <>
              <header className="border-b border-outline-variant pb-3 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="material-symbols-outlined text-primary text-sm">
                    groups
                  </span>
                  <h2 className="font-headline-md text-headline-md text-primary tracking-widest">
                    {room.status === "WAITING_OPPONENT"
                      ? "AGUARDANDO"
                      : "SALA PRONTA"}
                  </h2>
                </div>
                <span className="font-mono-data text-mono-data text-on-surface-variant">
                  {room.gameMode}
                </span>
              </header>

              {/* Token display */}
              <div className="flex flex-col items-center gap-2 py-4">
                <span className="font-label-caps text-label-caps text-on-surface-variant">
                  CÓDIGO DA SALA
                </span>
                <div className="flex items-center gap-3">
                  <span className="font-display-tactical text-display-tactical text-primary tracking-[0.5em] text-3xl">
                    {room.token}
                  </span>
                  <button
                    onClick={handleCopyToken}
                    className="p-2 text-on-surface-variant hover:text-primary transition-colors"
                    title="Copiar código"
                  >
                    <span className="material-symbols-outlined">
                      {copied ? "check" : "content_copy"}
                    </span>
                  </button>
                </div>
                {copied && (
                  <span className="font-mono-data text-mono-data text-primary text-xs">
                    CÓDIGO COPIADO!
                  </span>
                )}
                <span className="font-mono-data text-mono-data text-on-surface-variant text-xs">
                  Compartilhe o código com seu oponente
                </span>
              </div>

              {/* Players status */}
              <div className="flex flex-col gap-3 border border-outline-variant p-4">
                {/* Host */}
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-full border border-primary bg-secondary-container flex items-center justify-center font-label-caps text-label-caps text-primary">
                      {room.hostUsername?.[0]?.toUpperCase()}
                    </div>
                    <span className="font-mono-data text-mono-data text-on-surface">
                      {room.hostUsername?.toUpperCase()}
                    </span>
                    <span className="font-label-caps text-[10px] text-on-surface-variant border border-outline-variant px-1">
                      HOST
                    </span>
                  </div>
                  <span
                    className={`font-label-caps text-label-caps ${room.hostReady ? "text-primary" : "text-on-surface-variant"}`}
                  >
                    {room.hostReady ? "✓ PRONTO" : "AGUARDANDO"}
                  </span>
                </div>

                {/* Guest */}
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    {room.guestUsername ? (
                      <>
                        <div className="w-8 h-8 rounded-full border border-primary bg-secondary-container flex items-center justify-center font-label-caps text-label-caps text-primary">
                          {room.guestUsername[0]?.toUpperCase()}
                        </div>
                        <span className="font-mono-data text-mono-data text-on-surface">
                          {room.guestUsername.toUpperCase()}
                        </span>
                      </>
                    ) : (
                      <>
                        <div className="w-8 h-8 rounded-full border border-outline-variant bg-surface-container-high flex items-center justify-center">
                          <span className="material-symbols-outlined text-on-surface-variant text-sm">
                            person_add
                          </span>
                        </div>
                        <span className="font-mono-data text-mono-data text-on-surface-variant animate-pulse">
                          AGUARDANDO OPONENTE...
                        </span>
                      </>
                    )}
                  </div>
                  {room.guestUsername && (
                    <span
                      className={`font-label-caps text-label-caps ${room.guestReady ? "text-primary" : "text-on-surface-variant"}`}
                    >
                      {room.guestReady ? "✓ PRONTO" : "AGUARDANDO"}
                    </span>
                  )}
                </div>
              </div>

              {/* Starting animation */}
              {room.status === "STARTING" && (
                <div className="flex flex-col items-center gap-2 py-4">
                  <span className="material-symbols-outlined text-primary text-3xl animate-pulse">
                    rocket_launch
                  </span>
                  <span className="font-label-caps text-label-caps text-primary animate-pulse">
                    INICIANDO PARTIDA...
                  </span>
                </div>
              )}

              {/* Action buttons */}
              {room.status !== "STARTING" && (
                <div className="flex flex-col gap-3">
                  {room.guestUsername && !isReady && (
                    <button
                      onClick={handleConfirmReady}
                      disabled={loading}
                      className="w-full py-3 bg-surface-container-high border border-primary text-primary font-label-caps text-label-caps hover:bg-primary-container hover:text-on-primary-fixed transition-all radar-glow disabled:opacity-40"
                    >
                      {loading ? "CONFIRMANDO..." : "CONFIRMAR PRONTO"}
                    </button>
                  )}
                  {isReady && room.status !== "STARTING" && (
                    <div className="flex items-center justify-center gap-2 py-3 border border-primary/30 bg-primary-container/10">
                      <span className="material-symbols-outlined text-primary text-sm">
                        check_circle
                      </span>
                      <span className="font-label-caps text-label-caps text-primary">
                        VOCÊ ESTÁ PRONTO — AGUARDANDO OPONENTE
                      </span>
                    </div>
                  )}
                  {isHost && (
                    <button
                      onClick={handleCancelRoom}
                      className="w-full py-2 text-on-surface-variant font-label-caps text-label-caps hover:text-error transition-colors"
                    >
                      CANCELAR SALA
                    </button>
                  )}
                </div>
              )}
            </>
          )}

          {/* Error display */}
          {error && (
            <p className="font-mono-data text-mono-data text-error border-l-2 border-error pl-3 text-sm">
              {error}
            </p>
          )}
        </div>
      </main>
    </div>
  );
};

export default RoomPage;
