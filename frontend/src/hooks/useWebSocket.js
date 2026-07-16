import { useEffect, useRef, useCallback, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const useWebSocket = ({ token, onReconnect }) => {
  const clientRef = useRef(null);
  const [connected, setConnected] = useState(false);
  const pendingRef = useRef([]);
  const activeSubsRef = useRef([]);

  useEffect(() => {
    if (!token) return;

    const baseUrl = import.meta.env.VITE_API_BASE_URL;
    const client = new Client({
      webSocketFactory: () => new SockJS(`${baseUrl}/ws`),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        pendingRef.current.forEach(({ dest, cb }) => {
          const sub = client.subscribe(dest, (msg) => cb(JSON.parse(msg.body)));
          activeSubsRef.current.push(sub);
        });
        if (activeSubsRef.current.length > 0) {
          onReconnect?.();
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
      pendingRef.current = [];
      activeSubsRef.current = [];
      client.deactivate();
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

  return { connected, subscribe, unsubscribeAll, publish };
};

export default useWebSocket;
