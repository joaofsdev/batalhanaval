import axiosClient from './axiosClient';

export const getProfile = (userId) =>
  axiosClient.get(`/api/users/${userId}/profile`);

export const getMyProfile = () =>
  axiosClient.get('/api/users/me/profile');
