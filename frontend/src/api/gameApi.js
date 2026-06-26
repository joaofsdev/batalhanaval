import axiosClient from './axiosClient';

export const createOrJoinGame = () => axiosClient.post('/api/games');

export const getGame = (id) => axiosClient.get(`/api/games/${id}`);

export const placeShips = (id, ships) =>
  axiosClient.post(`/api/games/${id}/ships`, { ships });
