import { Navigate, Outlet } from 'react-router-dom'

/**
 * <<component>> RouteGuard (SAD diagram)
 *
 * Pattern: Proxy — intercepts navigation to protected routes and redirects
 * unauthenticated users to the login page before the route renders.
 *
 * JWT storage: localStorage (simple approach for demo).
 * [Archetype Convention Addition]: In production, use httpOnly cookies via
 * the auth service to prevent XSS-based token theft.
 *
 * Quality Attributes: Confidencialidad, Autenticidad (ISO 25010)
 */
export default function RouteGuard() {
  const token = localStorage.getItem('access_token')

  if (!token) {
    // Redirect to login — the auth service is external (Kong Gateway handles auth)
    return <Navigate to="/login" replace />
  }

  // Token present — allow access. Kong has already validated it upstream.
  // The backend re-validates for defense in depth.
  return <Outlet />
}
