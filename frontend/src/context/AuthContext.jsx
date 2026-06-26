import { createContext, useContext, useState } from 'react';
import { useNavigate } from 'react-router-dom';

const AuthContext = createContext(null);

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(() => localStorage.getItem('bn_token'));
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('bn_user');
    return stored ? JSON.parse(stored) : null;
  });

  const login = (userData) => {
    const { token: jwt, ...userInfo } = userData;
    localStorage.setItem('bn_token', jwt);
    localStorage.setItem('bn_user', JSON.stringify(userInfo));
    setToken(jwt);
    setUser(userInfo);
  };

  const logout = () => {
    localStorage.removeItem('bn_token');
    localStorage.removeItem('bn_user');
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, token, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};
