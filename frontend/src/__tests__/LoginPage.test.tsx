import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import axios from 'axios';
import LoginPage from '../pages/LoginPage.tsx';

vi.mock('axios');
const mockedAxios = vi.mocked(axios, true);

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('renders the login form with Serviplus branding', () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    expect(screen.getByText('Serviplus SA')).toBeInTheDocument();
    expect(screen.getByText('Sistema de Contabilidad')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('nombre.usuario')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('••••••••')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ingresar' })).toBeInTheDocument();
  });

  it('stores token and navigates to /cotizaciones on successful login', async () => {
    mockedAxios.post = vi.fn().mockResolvedValueOnce({ data: { token: 'jwt-token-123' } });

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByPlaceholderText('nombre.usuario'), {
      target: { value: 'admin' },
    });
    fireEvent.change(screen.getByPlaceholderText('••••••••'), {
      target: { value: 'password123' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Ingresar' }));

    await waitFor(() => {
      expect(localStorage.getItem('access_token')).toBe('jwt-token-123');
      expect(mockNavigate).toHaveBeenCalledWith('/cotizaciones', { replace: true });
    });
  });

  it('shows error message on failed login', async () => {
    mockedAxios.post = vi.fn().mockRejectedValueOnce(new Error('Unauthorized'));

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByPlaceholderText('nombre.usuario'), {
      target: { value: 'wrong' },
    });
    fireEvent.change(screen.getByPlaceholderText('••••••••'), {
      target: { value: 'wrongpass' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Ingresar' }));

    await waitFor(() => {
      expect(
        screen.getByText('Credenciales incorrectas. Verifica tu usuario y contraseña.')
      ).toBeInTheDocument();
    });
    expect(localStorage.getItem('access_token')).toBeNull();
  });
});
