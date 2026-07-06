import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const AdminRoute = () => {
  const { token, user } = useAuth();

  if (!token) return <Navigate to="/" replace />;
  if (user?.role !== 'ADMIN') return <Navigate to="/lobby" replace />;

  return <Outlet />;
};

export default AdminRoute;
