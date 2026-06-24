import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import NuevaFacturaPage from '../pages/NuevaFacturaPage.tsx';

vi.mock('react-toastify', () => ({ toast: { error: vi.fn(), success: vi.fn() } }));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

const renderPage = () =>
  render(
    <MemoryRouter>
      <NuevaFacturaPage />
    </MemoryRouter>
  );

describe('NuevaFacturaPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.setItem('access_token', 'test-token');
  });

  it('renderiza el formulario con todos los campos', () => {
    renderPage();

    expect(screen.getByText('Nueva Factura')).toBeInTheDocument();
    expect(screen.getByTitle('Identificador del cliente en el sistema')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Descripción')).toBeInTheDocument();
    expect(screen.getByText('Crear Factura')).toBeInTheDocument();
  });

  it('agrega una nueva linea al hacer click en Agregar linea', () => {
    renderPage();

    const descripcionInputs = screen.getAllByPlaceholderText('Descripción');
    expect(descripcionInputs).toHaveLength(1);

    fireEvent.click(screen.getByText('+ Agregar línea'));

    expect(screen.getAllByPlaceholderText('Descripción')).toHaveLength(2);
  });

  it('elimina una linea al hacer click en el boton de eliminar', () => {
    renderPage();

    fireEvent.click(screen.getByText('+ Agregar línea'));
    expect(screen.getAllByPlaceholderText('Descripción')).toHaveLength(2);

    fireEvent.click(screen.getAllByText('✕')[0]);
    expect(screen.getAllByPlaceholderText('Descripción')).toHaveLength(1);
  });
});
