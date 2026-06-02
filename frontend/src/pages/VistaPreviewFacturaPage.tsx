import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import axios from 'axios'
import type { FacturaResponse } from '../types.ts'

const API = import.meta.env['VITE_API_BASE_URL'] as string | undefined ?? '/api/v1'

export default function VistaPreviewFacturaPage() {
  const { id } = useParams<{ id: string }>()
  const [factura, setFactura] = useState<FacturaResponse | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!id) return
    const token = localStorage.getItem('access_token')
    void axios
      .get<FacturaResponse>(`${API}/facturas/${id}`, {
        headers: { Authorization: `Bearer ${token ?? ''}` },
      })
      .then((res) => { setFactura(res.data) })
      .catch(() => { toast.error('Error al cargar la factura') })
      .finally(() => { setLoading(false) })
  }, [id])

  if (loading) return <p>Cargando factura...</p>
  if (!factura) return <p>Factura no encontrada</p>

  return (
    <main style={{ maxWidth: 800, margin: '2rem auto', padding: '0 1rem' }}>
      <h1>Factura {factura.numero}</h1>
      <p><strong>Estado:</strong> <span>{factura.estado}</span></p>
      <p><strong>Cliente:</strong> {factura.clienteNombre}</p>
      <p><strong>Fecha vencimiento:</strong> {factura.fechaVencimiento}</p>
      <p><strong>Total:</strong> ₡{factura.total.toFixed(2)}</p>
      <p><strong>Saldo pendiente:</strong> ₡{factura.saldo.toFixed(2)}</p>

      {factura.pdfUrl && (
        <p>
          <a href={factura.pdfUrl} target="_blank" rel="noreferrer">
            📄 Descargar PDF
          </a>
        </p>
      )}

      <table>
        <thead>
          <tr>
            <th>Descripción</th><th>Cantidad</th><th>Precio</th><th>Subtotal</th>
          </tr>
        </thead>
        <tbody>
          {factura.lineas.map((l) => (
            <tr key={l.id}>
              <td>{l.descripcion}</td>
              <td>{l.cantidad}</td>
              <td>₡{l.precioUnitario}</td>
              <td>₡{l.subtotal}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </main>
  )
}
