import { useEffect, useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'react-toastify'
import axios from 'axios'
import type { CotizacionResponse } from '../types.ts'

const API = import.meta.env['VITE_API_BASE_URL'] as string | undefined ?? '/api/v1'

export default function PanelCotizacionesPage() {
  const [cotizaciones, setCotizaciones] = useState<CotizacionResponse[]>([])

  const fetchAll = useCallback(() => {
    const token = localStorage.getItem('access_token')
    void axios
      .get<CotizacionResponse[]>(`${API}/cotizaciones`, {
        headers: { Authorization: `Bearer ${token ?? ''}` },
      })
      .then((res) => { setCotizaciones(res.data) })
      .catch(() => { toast.error('Error al cargar el panel') })
  }, [])

  useEffect(() => { fetchAll() }, [fetchAll])

  const handleAction = async (id: number, action: 'aprobar' | 'rechazar') => {
    const token = localStorage.getItem('access_token')
    await axios.put(`${API}/cotizaciones/${id}/${action}`, null, {
      headers: { Authorization: `Bearer ${token ?? ''}` },
    })
    toast.success(`Cotización ${action === 'aprobar' ? 'aprobada' : 'rechazada'} correctamente`)
    fetchAll()
  }

  const handleActionSafe = (id: number, action: 'aprobar' | 'rechazar') => {
    void handleAction(id, action).catch(() => {
      toast.error(`Error al ${action} la cotización`)
    })
  }

  return (
    <main style={{ maxWidth: 1000, margin: '2rem auto', padding: '0 1rem' }}>
      <h1>Panel de Cotizaciones</h1>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            <th>Número</th><th>Cliente</th><th>Estado</th><th>Total</th><th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          {cotizaciones.map((c) => (
            <tr key={c.id}>
              <td><Link to={`/cotizaciones/${c.id}`}>{c.numero}</Link></td>
              <td>{c.clienteNombre}</td>
              <td>{c.estado}</td>
              <td>₡{c.total.toFixed(2)}</td>
              <td>
                {c.estado === 'ENVIADA' && (
                  <>
                    <button
                      title="Aprobar cotización — notifica al cliente por email"
                      onClick={() => { handleActionSafe(c.id, 'aprobar') }}
                    >
                      ✓ Aprobar
                    </button>
                    <button
                      title="Rechazar cotización"
                      onClick={() => { handleActionSafe(c.id, 'rechazar') }}
                    >
                      ✗ Rechazar
                    </button>
                  </>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </main>
  )
}
