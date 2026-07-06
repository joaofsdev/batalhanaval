import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/shared/ProtectedRoute';
import AdminRoute from './components/shared/AdminRoute';
import AuthPage from './pages/AuthPage';
import LobbyPage from './pages/LobbyPage';
import GamePage from './pages/GamePage';
import ProfilePage from './pages/ProfilePage';
import RoomPage from './pages/RoomPage';
import AdminLayout from './pages/admin/AdminLayout';
import UsersPage from './pages/admin/UsersPage';
import GamesPage from './pages/admin/GamesPage';
import AuditPage from './pages/admin/AuditPage';

const App = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<AuthPage />} />
          <Route element={<ProtectedRoute />}>
            <Route path="/lobby" element={<LobbyPage />} />
            <Route path="/game/:id" element={<GamePage />} />
            <Route path="/profile/:id" element={<ProfilePage />} />
            <Route path="/room" element={<RoomPage />} />
          </Route>
          <Route element={<AdminRoute />}>
            <Route path="/admin" element={<AdminLayout />}>
              <Route index element={<Navigate to="/admin/users" replace />} />
              <Route path="users" element={<UsersPage />} />
              <Route path="games" element={<GamesPage />} />
              <Route path="audit" element={<AuditPage />} />
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
