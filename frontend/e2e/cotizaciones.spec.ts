import { test, expect } from '@playwright/test';
import type { CotizacionResponse } from '../src/types.ts';

const mockCotizacion: CotizacionResponse = {
  id: 1,
  numero: 'COT-2026-0001',
  clienteId: 1,
  clienteNombre: 'Empresa Demo S.A.',
  fechaVigencia: '2026-12-31',
  estado: 'BORRADOR',
  subtotal: 200.0,
  impuesto: 26.0,
  total: 226.0,
  lineas: [{ id: 1, descripcion: 'Consultoría', cantidad: 2, precioUnitario: 100, subtotal: 200 }],
  creadoEn: '2026-06-01T10:00:00',
  creadoPor: 'usuario1',
};

test.describe('Cotizaciones', () => {
  test.beforeEach(async ({ page }) => {
    // Inject auth token before any navigation
    await page.addInitScript(() => {
      localStorage.setItem('access_token', 'eyJhbGciOiJIUzI1NiJ9.test.token');
    });
  });

  test('muestra la lista de cotizaciones con datos de la API', async ({ page }) => {
    // Arrange
    await page.route('**/api/v1/cotizaciones', async (route) => {
      await route.fulfill({ json: [mockCotizacion] });
    });

    // Act
    await page.goto('/cotizaciones');

    // Assert
    await expect(page.getByText('COT-2026-0001')).toBeVisible();
    await expect(page.getByText('Empresa Demo S.A.')).toBeVisible();
    await expect(page.getByText('BORRADOR')).toBeVisible();
    await expect(page.getByRole('link', { name: '+ Nueva cotización' })).toBeVisible();
  });

  test('muestra mensaje cuando no hay cotizaciones', async ({ page }) => {
    // Arrange
    await page.route('**/api/v1/cotizaciones', async (route) => {
      await route.fulfill({ json: [] });
    });

    // Act
    await page.goto('/cotizaciones');

    // Assert
    await expect(page.getByText(/no hay cotizaciones registradas/i)).toBeVisible();
  });

  test('navega a la página de detalle al hacer clic en "Ver detalle"', async ({ page }) => {
    // Arrange
    await page.route('**/api/v1/cotizaciones', async (route) => {
      await route.fulfill({ json: [mockCotizacion] });
    });
    await page.route('**/api/v1/cotizaciones/1', async (route) => {
      await route.fulfill({ json: mockCotizacion });
    });

    // Act
    await page.goto('/cotizaciones');
    await page.getByRole('link', { name: 'Ver detalle' }).click();

    // Assert
    await expect(page).toHaveURL(/\/cotizaciones\/1/);
    await expect(page.getByText('COT-2026-0001')).toBeVisible();
  });

  test('renderiza el formulario de nueva cotización', async ({ page }) => {
    // Act
    await page.goto('/cotizaciones/nueva');

    // Assert
    await expect(page.getByText('Nueva Cotización')).toBeVisible();
    await expect(page.getByPlaceholder('Descripción')).toBeVisible();
    await expect(page.getByRole('button', { name: /agregar línea/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /crear cotización/i })).toBeVisible();
  });

  test('el panel muestra botones aprobar/rechazar solo para cotizaciones ENVIADA', async ({
    page,
  }) => {
    // Arrange
    const enviada: CotizacionResponse = { ...mockCotizacion, estado: 'ENVIADA' };
    await page.route('**/api/v1/cotizaciones', async (route) => {
      await route.fulfill({ json: [enviada, mockCotizacion] });
    });

    // Act
    await page.goto('/cotizaciones/panel');

    // Assert — one row ENVIADA has action buttons, BORRADOR does not
    await expect(page.getByRole('button', { name: /aprobar/i })).toHaveCount(1);
    await expect(page.getByRole('button', { name: /rechazar/i })).toHaveCount(1);
  });
});
