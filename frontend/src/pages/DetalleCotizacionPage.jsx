import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import axios from 'axios'

const API = import.meta.env.VITE_API_BASE_URL || '/api/v1'

/**
 * <<component>> DetalleCotizacionPage
 * Shows full cotizacion details with state and line items.
 */
export default function DetalleCotizacionPage() {
  const { id } = useParams()
  const [cotizacion, setCotizacion] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem('access_token')
    axios
      .get(`${API}/cotizaciones/${id}`, { headers: { Authorization: `Bearer ${token}` } })
      .then((res) => setCotizacion(res.data))
      .catch(() => toast.error('Error al cargar la cotización'))
      .finally(() => setLoading(false))
  }, [id])

  if (loading)    return <p>Cargando...</p>
  if (!cotizacion) return <p>Cotización no encontrada</p>

  return (
    <main style={{ maxWidth: 800, margin: '2rem auto', padding: '0 1rem' }}>
      <h1>Cotización {cotizacion.numero}</h1>
      <p><strong>Estado:</strong> {cotizacion.estado}</p>
      <p><strong>Cliente:</strong> {cotizacion.clienteNombre}</p>
      <p><strong>Vigencia hasta:</strong> {cotizacion.fechaVigencia}</p>
      <p><strong>Total:</strong> ₡{cotizacion.total?.toFixed(2)}</p>

      {cotizacion.notas && <p><strong>Notas:</strong> {cotizacion.notas}</p>}

      <h2>Líneas</h2>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            <th>Descripción</th><th>Cantidad</th><th>Precio unit.</th><th>Subtotal</th>
          </tr>
        </thead>
        <tbody>
          {cotizacion.lineas?.map((l) => (
            <tr key={l.id}>
              <td>{l.descripcion}</td>
              <td>{l.cantidad}</td>
              <td>₡{l.precioUnitario}</td>
              <td>₡{l.subtotal}</td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr>
            <td colSpan={3}><strong>Subtotal</strong></td>
            <td>₡{cotizacion.subtotal?.toFixed(2)}</td>
          </tr>
          <tr>
            <td colSpan={3}><strong>Impuesto (13%)</strong></td>
            <td>₡{cotizacion.impuesto?.toFixed(2)}</td>
          </tr>
          <tr>
            <td colSpan={3}><strong>TOTAL</strong></td>
            <td>₡{cotizacion.total?.toFixed(2)}</td>
          </tr>
        </tfoot>
      </table>
    </main>
  )
}
