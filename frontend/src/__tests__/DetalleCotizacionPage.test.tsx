import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import axios from 'axios';
import DetalleCotizacionPage from '../pages/DetalleCotizacionPage.tsx';
import type { CotizacionResponse } from '../types.ts';

vi.mock('axios');
vi.mock('react-toastify', () => ({ toast: { error: vi.fn(), success: vi.fn() } }));

const mockedAxios = vi.mocked(axios, true);

const cotizacionMock: CotizacionResponse = {
  id: 1,
  numero: 'COT-2026-0001',
  clienteId: 10,
  clienteNombre: 'Empresa Demo SA',
  fechaVigencia: '2026-12-31',
  notas: 'Notas de prueba',
  estado: 'ENVIADA',
  subtotal: 1000,
  impuesto: 130,
  total: 1130,
  lineas: [{ id: 1, descripcion: 'Servicio A', cantidad: 2, precioUnitario: 500, subtotal: 1000 }],
  creadoEn: '2026-06-01T10:00:00',
  creadoPor: 'admin',
};

const renderWithRoute = (id: string) =>
  render(
    <MemoryRouter initialEntries={[`/cotizaciones/${id}`]}>
      <Routes>
        <Route path="/cotizaciones/:id" element={<DetalleCotizacionPage />} />
      </Routes>
    </MemoryRouter>
  );

describe('DetalleCotizacionPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.setItem('access_token', 'test-token');
  });

  it('muestra indicador de carga inicialmente', () => {
    mockedAxios.get = vi.fn().mockReturnValue(new Promise(() => {}));
    renderWithRoute('1');
    expect(screen.getByText('Cargando...')).toBeInTheDocument();
  });

  it('muestra los detalles de la cotizacion cuando carga correctamente', async () => {
    mockedAxios.get = vi.fn().mockResolvedValueOnce({ data: cotizacionMock });
    renderWithRoute('1');

    await waitFor(() => {
      expect(screen.getByText('Cotización COT-2026-0001')).toBeInTheDocument();
    });

    expect(screen.getByText('Empresa Demo SA')).toBeInTheDocument();
    expect(screen.getByText('ENVIADA')).toBeInTheDocument();
    expect(screen.getByText('Servicio A')).toBeInTheDocument();
    expect(screen.getByText('Notas de prueba')).toBeInTheDocument();
  });

  it('muestra mensaje de no encontrada cuando la API falla', async () => {
    mockedAxios.get = vi.fn().mockRejectedValueOnce(new Error('Not found'));
    renderWithRoute('99');

    await waitFor(() => {
      expect(screen.getByText('Cotización no encontrada')).toBeInTheDocument();
    });
  });

  it('no muestra la seccion de notas cuando no hay notas', async () => {
    const sinNotas = { ...cotizacionMock, notas: undefined };
    mockedAxios.get = vi.fn().mockResolvedValueOnce({ data: sinNotas });
    renderWithRoute('1');

    await waitFor(() => {
      expect(screen.getByText('Cotización COT-2026-0001')).toBeInTheDocument();
    });

    expect(screen.queryByText('Notas:')).not.toBeInTheDocument();
  });
});
