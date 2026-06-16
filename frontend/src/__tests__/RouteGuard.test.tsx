import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import RouteGuard from '../components/RouteGuard.tsx';

// Provides the full routing context that RouteGuard needs (<Outlet /> + Navigate)
function TestRoutes({ initialPath }: { initialPath: string }) {
  return (
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path="/login" element={<div>Login Page</div>} />
        <Route element={<RouteGuard />}>
          <Route path="/cotizaciones" element={<div>Protected Content</div>} />
        </Route>
      </Routes>
    </MemoryRouter>
  );
}

describe('RouteGuard', () => {
  describe('cuando no hay token en localStorage', () => {
    it('redirige a /login y no renderiza el contenido protegido', () => {
      // Arrange — localStorage vacío (limpiado en setup.ts)

      // Act
      render(<TestRoutes initialPath="/cotizaciones" />);

      // Assert
      expect(screen.getByText('Login Page')).toBeInTheDocument();
      expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
    });
  });

  describe('cuando existe un token en localStorage', () => {
    it('renderiza el contenido protegido sin redirigir', () => {
      // Arrange
      localStorage.setItem('access_token', 'eyJhbGciOiJIUzI1NiJ9.test.token');

      // Act
      render(<TestRoutes initialPath="/cotizaciones" />);

      // Assert
      expect(screen.getByText('Protected Content')).toBeInTheDocument();
      expect(screen.queryByText('Login Page')).not.toBeInTheDocument();
    });
  });
});
