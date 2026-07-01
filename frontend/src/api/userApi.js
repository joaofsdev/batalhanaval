import axiosClient from './axiosClient';

export const getProfile = (userId) =>
  axiosClient.get(`/api/users/${userId}/profile`);
