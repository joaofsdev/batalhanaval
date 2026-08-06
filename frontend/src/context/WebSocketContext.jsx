import { createContext, useContext, useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import { useAuth } from './AuthContext';

const WebSocketContext = createContext(null);

export const useWs = () => useContext(WebSocketContext);

export const WebSocketProvider = ({ children }) => {
  const { token } = useAuth();
  const clientRef = useRef(null);
  const [connected, setConnected] = useState(false);
  const pendingRef = useRef([]);
  const activeSubsRef = useRef([]);
  const onReconnectRef = useRef(null);

  useEffect(() => {
    if (!token) {
      // Disconnect if token is removed (logout)
      if (clientRef.current) {
        clientRef.current.deactivate();
        clientRef.current = null;
        setConnected(false);
        activeSubsRef.current = [];
        pendingRef.current = [];
      }
      return;
    }

    const baseUrl = import.meta.env.VITE_API_BASE_URL;
    const wsUrl = baseUrl.replace(/^http/, 'ws') + '/ws-native';

    const client = new Client({
      brokerURL: wsUrl,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setConnected(true);
        // Re-subscribe pending subscriptions
        pendingRef.current.forEach(({ dest, cb }) => {
          const sub = client.subscribe(dest, (msg) => cb(JSON.parse(msg.body)));
          activeSubsRef.current.push(sub);
        });
        pendingRef.current = [];
        // Notify reconnection
        if (activeSubsRef.current.length > 0) {
          onReconnectRef.current?.();
        }
      },
      onDisconnect: () => {
        setConnected(false);
        activeSubsRef.current = [];
      },
      onStompError: (frame) => {
        setConnected(false);
        const message = frame?.headers?.message || '';
        if (message.includes('inválido') || message.includes('suspensa') || message.includes('banida')) {
          client.deactivate();
          localStorage.removeItem('bn_token');
          localStorage.removeItem('bn_user');
          window.location.href = '/';
        }
      },
      onWebSocketClose: (event) => {
        if (event?.code === 1008 || event?.code === 1002) {
          client.deactivate();
          localStorage.removeItem('bn_token');
          localStorage.removeItem('bn_user');
          window.location.href = '/';
        }
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      activeSubsRef.current = [];
      pendingRef.current = [];
      client.deactivate();
      clientRef.current = null;
    };
  }, [token]);

  const subscribe = useCallback((destination, callback) => {
    const client = clientRef.current;
    if (client?.connected) {
      const sub = client.subscribe(destination, (msg) => callback(JSON.parse(msg.body)));
      activeSubsRef.current.push(sub);
      return sub;
    } else {
      pendingRef.current.push({ dest: destination, cb: callback });
      return null;
    }
  }, []);

  const unsubscribeAll = useCallback(() => {
    activeSubsRef.current.forEach((sub) => {
      try { sub.unsubscribe(); } catch (e) { /* ignore */ }
    });
    activeSubsRef.current = [];
    pendingRef.current = [];
  }, []);

  const publish = useCallback((destination, body) => {
    clientRef.current?.publish({
      destination,
      body: JSON.stringify(body),
    });
  }, []);

  const setOnReconnect = useCallback((fn) => {
    onReconnectRef.current = fn;
  }, []);

  return (
    <WebSocketContext.Provider value={{ connected, subscribe, unsubscribeAll, publish, setOnReconnect }}>
      {children}
    </WebSocketContext.Provider>
  );
};
