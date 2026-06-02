// Domain enums — must stay in sync with backend EstadoCotizacion / EstadoFactura
export type EstadoCotizacion = 'BORRADOR' | 'ENVIADA' | 'ACEPTADA' | 'RECHAZADA' | 'ANULADA'
export type EstadoFactura    = 'PENDIENTE' | 'PAGADA' | 'ANULADA'

// ── Response types (match backend Java records exactly) ────────────────────

export interface LineaCotizacionResponse {
  id: number
  descripcion: string
  cantidad: number
  precioUnitario: number
  subtotal: number
}

export interface CotizacionResponse {
  id: number
  numero: string
  clienteId: number
  clienteNombre: string
  fechaVigencia: string          // ISO date yyyy-MM-dd
  notas?: string
  estado: EstadoCotizacion
  subtotal: number
  impuesto: number
  total: number
  lineas: LineaCotizacionResponse[]
  creadoEn: string
  creadoPor: string
}

export interface LineaFacturaResponse {
  id: number
  descripcion: string
  cantidad: number
  precioUnitario: number
  subtotal: number
}

export interface FacturaResponse {
  id: number
  numero: string
  clienteId: number
  clienteNombre: string
  fechaVencimiento: string       // ISO date yyyy-MM-dd
  notas?: string
  estado: EstadoFactura
  subtotal: number
  impuesto: number
  total: number
  saldo: number
  pdfUrl?: string
  lineas: LineaFacturaResponse[]
  creadoEn: string
  creadoPor: string
}

export interface AbonoResponse {
  id: number
  facturaId: number
  monto: number
  fecha: string
  referencia?: string
  creadoPor: string
}

// ── API error shape (matches backend ApiError record) ─────────────────────

export interface ApiError {
  status: number
  message: string
  timestamp: string
}

// ── Form value types (react-hook-form) ────────────────────────────────────

export interface LineaFormValues {
  descripcion: string
  cantidad: number
  precioUnitario: number
}

export interface CotizacionFormValues {
  clienteId: number
  clienteNombre: string
  fechaVigencia: string
  notas: string
  lineas: LineaFormValues[]
}

export interface FacturaFormValues {
  clienteId: number
  clienteNombre: string
  fechaVencimiento: string
  notas: string
  lineas: LineaFormValues[]
}
