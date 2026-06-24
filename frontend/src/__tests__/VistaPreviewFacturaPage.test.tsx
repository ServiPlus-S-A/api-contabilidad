import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import axios from 'axios';
import VistaPreviewFacturaPage from '../pages/VistaPreviewFacturaPage.tsx';
import type { FacturaResponse } from '../types.ts';

vi.mock('axios');
vi.mock('react-toastify', () => ({ toast: { error: vi.fn(), success: vi.fn() } }));

const mockedAxios = vi.mocked(axios, true);

const facturaMock: FacturaResponse = {
  id: 1,
  numero: 'FAC-2026-0001',
  clienteId: 10,
  clienteNombre: 'Cliente Demo SA',
  fechaVencimiento: '2026-12-31',
  estado: 'PENDIENTE',
  subtotal: 1000,
  impuesto: 130,
  total: 1130,
  saldo: 1130,
  pdfUrl: 'http://minio/facturas/1.pdf',
  lineas: [
    { id: 1, descripcion: 'Servicio B', cantidad: 1, precioUnitario: 1000, subtotal: 1000 },
  ],
  creadoEn: '2026-06-01T10:00:00',
  creadoPor: 'admin',
};

const renderWithRoute = (id: string) =>
  render(
    <MemoryRouter initialEntries={[`/facturas/${id}`]}>
      <Routes>
        <Route path="/facturas/:id" element={<VistaPreviewFacturaPage />} />
      </Routes>
    </MemoryRouter>,
  );

describe('VistaPreviewFacturaPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.setItem('access_token', 'test-token');
  });

  it('muestra indicador de carga inicialmente', () => {
    mockedAxios.get = vi.fn().mockReturnValue(new Promise(() => {}));
    renderWithRoute('1');
    expect(screen.getByText('Cargando factura...')).toBeInTheDocument();
  });

  it('muestra los detalles de la factura cuando carga correctamente', async () => {
    mockedAxios.get = vi.fn().mockResolvedValueOnce({ data: facturaMock });
    renderWithRoute('1');

    await waitFor(() => {
      expect(screen.getByText('Factura FAC-2026-0001')).toBeInTheDocument();
    });

    expect(screen.getByText('Cliente Demo SA')).toBeInTheDocument();
    expect(screen.getByText('PENDIENTE')).toBeInTheDocument();
    expect(screen.getByText('Servicio B')).toBeInTheDocument();
    expect(screen.getByText('📄 Descargar PDF')).toBeInTheDocument();
  });

  it('muestra mensaje de no encontrada cuando la API falla', async () => {
    mockedAxios.get = vi.fn().mockRejectedValueOnce(new Error('Not found'));
    renderWithRoute('99');

    await waitFor(() => {
      expect(screen.getByText('Factura no encontrada')).toBeInTheDocument();
    });
  });

  it('no muestra enlace PDF cuando pdfUrl no esta presente', async () => {
    const sinPdf = { ...facturaMock, pdfUrl: undefined };
    mockedAxios.get = vi.fn().mockResolvedValueOnce({ data: sinPdf });
    renderWithRoute('1');

    await waitFor(() => {
      expect(screen.getByText('Factura FAC-2026-0001')).toBeInTheDocument();
    });

    expect(screen.queryByText('📄 Descargar PDF')).not.toBeInTheDocument();
  });
});
