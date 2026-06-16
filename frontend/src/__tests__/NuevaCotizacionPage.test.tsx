import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import axios from 'axios';
import NuevaCotizacionPage from '../pages/NuevaCotizacionPage.tsx';

vi.mock('axios');
const mockedAxios = vi.mocked(axios, true);

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/cotizaciones/nueva']}>
      <Routes>
        <Route path="/cotizaciones/nueva" element={<NuevaCotizacionPage />} />
        <Route path="/cotizaciones/:id" element={<div>Detalle</div>} />
      </Routes>
    </MemoryRouter>
  );
}

describe('NuevaCotizacionPage', () => {
  beforeEach(() => {
    localStorage.setItem('access_token', 'test-token');
  });

  it('renderiza todos los campos del formulario', () => {
    // Act
    renderPage();

    // Assert
    expect(screen.getByText('Nueva Cotización')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Descripción')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /crear cotización/i })).toBeInTheDocument();
  });

  it('permite agregar una línea adicional al hacer clic en el botón', async () => {
    // Arrange
    const user = userEvent.setup();
    renderPage();
    const descripcionInputs = screen.getAllByPlaceholderText('Descripción');
    expect(descripcionInputs).toHaveLength(1);

    // Act
    await user.click(screen.getByRole('button', { name: /agregar línea/i }));

    // Assert
    expect(screen.getAllByPlaceholderText('Descripción')).toHaveLength(2);
  });

  it('llama a la API y redirige al detalle tras un envío exitoso', async () => {
    // Arrange
    const user = userEvent.setup();
    mockedAxios.post.mockResolvedValueOnce({ data: { id: 42, numero: 'COT-2026-0042' } });
    renderPage();

    // Act — completar campos requeridos
    await user.type(screen.getAllByRole('spinbutton')[0], '1');
    await user.type(screen.getByRole('textbox', { name: /nombre/i }), 'Cliente Test');
    const dateInput = document.querySelector('input[type="date"]') as HTMLInputElement;
    fireEvent.change(dateInput, { target: { value: '2027-01-01' } });
    await user.type(screen.getByPlaceholderText('Descripción'), 'Servicio');
    // precioUnitario defaults to 0 which fails min:0.01 — set it to a valid value
    fireEvent.change(screen.getAllByRole('spinbutton')[2], { target: { value: '100', valueAsNumber: 100 } });
    await user.click(screen.getByRole('button', { name: /crear cotización/i }));

    // Assert
    await waitFor(() => {
      expect(mockedAxios.post).toHaveBeenCalledOnce();
      expect(screen.getByText('Detalle')).toBeInTheDocument();
    });
  });
});
