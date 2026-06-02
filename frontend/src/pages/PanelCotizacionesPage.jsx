import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'react-toastify'
import axios from 'axios'

const API = import.meta.env.VITE_API_BASE_URL || '/api/v1'

/**
 * <<component>> PanelCotizacionesPage
 * Admin panel showing all cotizaciones with approval/reject actions.
 * ROLE_ADMIN and ROLE_CONTADOR can access approve/reject.
 */
export default function PanelCotizacionesPage() {
  const [cotizaciones, setCotizaciones] = useState([])

  const fetchAll = () => {
    const token = localStorage.getItem('access_token')
    axios
      .get(`${API}/cotizaciones`, { headers: { Authorization: `Bearer ${token}` } })
      .then((res) => setCotizaciones(res.data))
      .catch(() => toast.error('Error al cargar el panel'))
  }

  useEffect(fetchAll, [])

  const handleAction = async (id, action) => {
    const token = localStorage.getItem('access_token')
    try {
      await axios.put(`${API}/cotizaciones/${id}/${action}`, null, {
        headers: { Authorization: `Bearer ${token}` },
      })
      toast.success(`Cotización ${action === 'aprobar' ? 'aprobada' : 'rechazada'} correctamente`)
      fetchAll()
    } catch (err) {
      toast.error(err.response?.data?.message || `Error al ${action} la cotización`)
    }
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
              <td>₡{c.total?.toFixed(2)}</td>
              <td>
                {c.estado === 'ENVIADA' && (
                  <>
                    <button
                      title="Aprobar cotización — notifica al cliente por email"
                      onClick={() => handleAction(c.id, 'aprobar')}
                    >
                      ✓ Aprobar
                    </button>
                    <button
                      title="Rechazar cotización"
                      onClick={() => handleAction(c.id, 'rechazar')}
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
