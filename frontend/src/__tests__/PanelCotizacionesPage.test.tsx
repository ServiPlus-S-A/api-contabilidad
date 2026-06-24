import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import axios from 'axios';
import PanelCotizacionesPage from '../pages/PanelCotizacionesPage.tsx';
import type { CotizacionResponse } from '../types.ts';

vi.mock('axios');
vi.mock('react-toastify', () => ({ toast: { error: vi.fn(), success: vi.fn() } }));

const mockedAxios = vi.mocked(axios, true);

const cotizacionEnviada: CotizacionResponse = {
  id: 1,
  numero: 'COT-2026-0001',
  clienteId: 10,
  clienteNombre: 'Cliente Enviada SA',
  fechaVigencia: '2026-12-31',
  estado: 'ENVIADA',
  subtotal: 1000,
  impuesto: 130,
  total: 1130,
  lineas: [],
  creadoEn: '2026-06-01T10:00:00',
  creadoPor: 'admin',
};

const cotizacionAceptada: CotizacionResponse = {
  ...cotizacionEnviada,
  id: 2,
  numero: 'COT-2026-0002',
  estado: 'ACEPTADA',
};

const renderPage = () =>
  render(
    <MemoryRouter>
      <PanelCotizacionesPage />
    </MemoryRouter>,
  );

describe('PanelCotizacionesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.setItem('access_token', 'test-token');
  });

  it('renderiza el titulo del panel', async () => {
    mockedAxios.get = vi.fn().mockResolvedValueOnce({ data: [] });
    renderPage();
    expect(screen.getByText('Panel de Cotizaciones')).toBeInTheDocument();
  });

  it('muestra las cotizaciones cargadas desde la API', async () => {
    mockedAxios.get = vi
      .fn()
      .mockResolvedValueOnce({ data: [cotizacionEnviada, cotizacionAceptada] });
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('COT-2026-0001')).toBeInTheDocument();
      expect(screen.getByText('COT-2026-0002')).toBeInTheDocument();
    });
  });

  it('muestra botones de aprobar y rechazar solo para cotizaciones ENVIADA', async () => {
    mockedAxios.get = vi
      .fn()
      .mockResolvedValueOnce({ data: [cotizacionEnviada, cotizacionAceptada] });
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('COT-2026-0001')).toBeInTheDocument();
    });

    expect(screen.getByText('✓ Aprobar')).toBeInTheDocument();
    expect(screen.getByText('✗ Rechazar')).toBeInTheDocument();
  });

  it('llama a la API de aprobar al hacer click en Aprobar', async () => {
    mockedAxios.get = vi.fn().mockResolvedValue({ data: [cotizacionEnviada] });
    mockedAxios.put = vi.fn().mockResolvedValueOnce({});
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('✓ Aprobar')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('✓ Aprobar'));

    await waitFor(() => {
      expect(mockedAxios.put).toHaveBeenCalledWith(
        expect.stringContaining('/cotizaciones/1/aprobar'),
        null,
        expect.any(Object),
      );
    });
  });

  it('llama a la API de rechazar al hacer click en Rechazar', async () => {
    mockedAxios.get = vi.fn().mockResolvedValue({ data: [cotizacionEnviada] });
    mockedAxios.put = vi.fn().mockResolvedValueOnce({});
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('✗ Rechazar')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('✗ Rechazar'));

    await waitFor(() => {
      expect(mockedAxios.put).toHaveBeenCalledWith(
        expect.stringContaining('/cotizaciones/1/rechazar'),
        null,
        expect.any(Object),
      );
    });
  });
});
