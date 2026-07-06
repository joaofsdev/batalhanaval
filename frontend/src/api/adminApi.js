import axiosClient from './axiosClient';

export const getUsers = (page = 0, size = 20, status) => {
  let url = `/api/admin/users?page=${page}&size=${size}`;
  if (status) url += `&status=${status}`;
  return axiosClient.get(url);
};

export const banUser = (id) =>
  axiosClient.patch(`/api/admin/users/${id}/ban`);

export const suspendUser = (id, suspendedUntil) =>
  axiosClient.patch(`/api/admin/users/${id}/suspend`, { suspendedUntil });

export const reactivateUser = (id) =>
  axiosClient.patch(`/api/admin/users/${id}/reactivate`);

export const getActiveGames = (page = 0, size = 20) =>
  axiosClient.get(`/api/admin/games/active?page=${page}&size=${size}`);

export const forceEndGame = (id) =>
  axiosClient.patch(`/api/admin/games/${id}/force-end`);

export const getAuditLog = (page = 0, size = 20) =>
  axiosClient.get(`/api/admin/audit-log?page=${page}&size=${size}`);
