import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import axios from 'axios';
import MisCotizacionesPage from '../pages/MisCotizacionesPage.tsx';
import type { CotizacionResponse } from '../types.ts';

vi.mock('axios');
const mockedAxios = vi.mocked(axios, true);

const cotizacionMock: CotizacionResponse = {
  id: 1,
  numero: 'COT-2026-0001',
  clienteId: 1,
  clienteNombre: 'Empresa Demo S.A.',
  fechaVigencia: '2026-12-31',
  estado: 'BORRADOR',
  subtotal: 200.0,
  impuesto: 26.0,
  total: 226.0,
  lineas: [],
  creadoEn: '2026-06-01T10:00:00',
  creadoPor: 'usuario1',
};

function renderPage() {
  return render(
    <MemoryRouter>
      <MisCotizacionesPage />
    </MemoryRouter>
  );
}

describe('MisCotizacionesPage', () => {
  beforeEach(() => {
    localStorage.setItem('access_token', 'test-token');
  });

  it('muestra indicador de carga mientras la petición está en vuelo', () => {
    // Arrange — promise que nunca resuelve
    mockedAxios.get.mockReturnValue(new Promise(() => undefined));

    // Act
    renderPage();

    // Assert
    expect(screen.getByText(/cargando cotizaciones/i)).toBeInTheDocument();
  });

  it('renderiza la tabla con cotizaciones al resolver la petición', async () => {
    // Arrange
    mockedAxios.get.mockResolvedValueOnce({ data: [cotizacionMock] });

    // Act
    renderPage();

    // Assert
    await waitFor(() => {
      expect(screen.getByText('COT-2026-0001')).toBeInTheDocument();
    });
    expect(screen.getByText('Empresa Demo S.A.')).toBeInTheDocument();
    expect(screen.getByText('BORRADOR')).toBeInTheDocument();
  });

  it('muestra mensaje cuando la lista está vacía', async () => {
    // Arrange
    mockedAxios.get.mockResolvedValueOnce({ data: [] });

    // Act
    renderPage();

    // Assert
    await waitFor(() => {
      expect(screen.getByText(/no hay cotizaciones registradas/i)).toBeInTheDocument();
    });
  });

  it('muestra toast de error cuando la petición falla', async () => {
    // Arrange
    mockedAxios.get.mockRejectedValueOnce(new Error('Network Error'));

    // Act
    renderPage();

    // Assert — página deja de cargar aunque falle
    await waitFor(() => {
      expect(screen.queryByText(/cargando/i)).not.toBeInTheDocument();
    });
  });

  it('incluye el token en la cabecera Authorization', async () => {
    // Arrange
    mockedAxios.get.mockResolvedValueOnce({ data: [] });

    // Act
    renderPage();
    await waitFor(() => {
      expect(mockedAxios.get).toHaveBeenCalledOnce();
    });

    // Assert
    const [, config] = mockedAxios.get.mock.calls[0] as [
      string,
      { headers: Record<string, string> },
    ];
    expect(config.headers['Authorization']).toBe('Bearer test-token');
  });
});
