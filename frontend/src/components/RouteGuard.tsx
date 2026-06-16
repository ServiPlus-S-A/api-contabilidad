import { Navigate, Outlet } from 'react-router-dom';

/**
 * <<component>> RouteGuard
 * Pattern: Proxy — intercepts protected routes and redirects unauthenticated users.
 * Production note: replace localStorage with httpOnly cookie check.
 */
export default function RouteGuard() {
  const token = localStorage.getItem('access_token');
  return token !== null ? <Outlet /> : <Navigate to="/login" replace />;
}
