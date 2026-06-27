import axiosClient from './axiosClient';

export const createOrJoinGame = () => axiosClient.post('/api/games');

export const getGame = (id) => axiosClient.get(`/api/games/${id}`);

export const placeShips = (id, ships) =>
  axiosClient.post(`/api/games/${id}/ships`, { ships });

export const cancelGame = (id) =>
  axiosClient.delete(`/api/games/${id}`);

export const getRanking = () =>
  axiosClient.get('/api/ranking');
