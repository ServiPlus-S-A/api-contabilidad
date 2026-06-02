import { test, expect } from '@playwright/test'
import type { FacturaResponse } from '../src/types.ts'

const mockFactura: FacturaResponse = {
  id: 1,
  numero: 'FAC-2026-0001',
  clienteId: 1,
  clienteNombre: 'Cliente Ejemplo',
  fechaVencimiento: '2026-09-30',
  estado: 'PENDIENTE',
  subtotal: 500.00,
  impuesto: 65.00,
  total: 565.00,
  saldo: 565.00,
  lineas: [
    { id: 1, descripcion: 'Desarrollo de software', cantidad: 1, precioUnitario: 500, subtotal: 500 },
  ],
  creadoEn: '2026-06-01T10:00:00',
  creadoPor: 'usuario1',
}

test.describe('Facturas', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('access_token', 'eyJhbGciOiJIUzI1NiJ9.test.token')
    })
  })

  test('renderiza el formulario de nueva factura con sus campos', async ({ page }) => {
    // Act
    await page.goto('/facturas/nueva')

    // Assert
    await expect(page.getByText('Nueva Factura')).toBeVisible()
    await expect(page.getByText('Datos del cliente')).toBeVisible()
    await expect(page.getByRole('button', { name: /crear factura/i })).toBeVisible()
  })

  test('muestra el detalle de una factura con número y estado', async ({ page }) => {
    // Arrange
    await page.route('**/api/v1/facturas/1', async (route) => {
      await route.fulfill({ json: mockFactura })
    })

    // Act
    await page.goto('/facturas/1')

    // Assert
    await expect(page.getByText('FAC-2026-0001')).toBeVisible()
    await expect(page.getByText('PENDIENTE')).toBeVisible()
    await expect(page.getByText('Cliente Ejemplo')).toBeVisible()
    await expect(page.getByText(/₡565.00/)).toBeVisible()
  })

  test('muestra enlace al PDF cuando pdfUrl está presente', async ({ page }) => {
    // Arrange
    const facturaConPdf: FacturaResponse = { ...mockFactura, pdfUrl: 'http://minio/facturas/FAC-2026-0001.pdf' }
    await page.route('**/api/v1/facturas/1', async (route) => {
      await route.fulfill({ json: facturaConPdf })
    })

    // Act
    await page.goto('/facturas/1')

    // Assert
    const pdfLink = page.getByRole('link', { name: /descargar pdf/i })
    await expect(pdfLink).toBeVisible()
    await expect(pdfLink).toHaveAttribute('href', 'http://minio/facturas/FAC-2026-0001.pdf')
  })

  test('muestra "no encontrada" cuando la API devuelve error', async ({ page }) => {
    // Arrange
    await page.route('**/api/v1/facturas/999', async (route) => {
      await route.fulfill({ status: 404, json: { message: 'Factura no encontrada' } })
    })

    // Act
    await page.goto('/facturas/999')

    // Assert
    await expect(page.getByText(/factura no encontrada/i)).toBeVisible()
  })
})
