import axiosClient from './axiosClient';

export const createOrJoinGame = (gameMode) =>
  axiosClient.post('/api/games', { gameMode: gameMode || 'CLASSIC' });

export const getGame = (id) => axiosClient.get(`/api/games/${id}`);

export const getActiveGame = () => axiosClient.get('/api/games/active');

export const getFleetConfig = () => axiosClient.get('/api/games/fleet-config');

export const placeShips = (id, ships) =>
  axiosClient.post(`/api/games/${id}/ships`, { ships });

export const cancelGame = (id) =>
  axiosClient.delete(`/api/games/${id}`);

export const surrender = (id) =>
  axiosClient.post(`/api/games/${id}/surrender`);

export const requestRematch = (id) =>
  axiosClient.post(`/api/games/${id}/rematch`);

export const getRanking = (page = 0, size = 20, period = 'all') =>
  axiosClient.get(`/api/ranking?page=${page}&size=${size}&period=${period}`);

export const getGameHistory = (page = 0, size = 10) =>
  axiosClient.get(`/api/games/history?page=${page}&size=${size}`);

export const getAbility = (id) =>
  axiosClient.get(`/api/games/${id}/ability`);

export const useAbility = (id, payload) =>
  axiosClient.post(`/api/games/${id}/ability`, payload);
