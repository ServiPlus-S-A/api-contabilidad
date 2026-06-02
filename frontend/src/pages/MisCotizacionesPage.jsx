import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'react-toastify'
import axios from 'axios'

const API = import.meta.env.VITE_API_BASE_URL || '/api/v1'

/**
 * <<component>> MisCotizacionesPage
 * Lists all quotations for the current user.
 * Quality Attribute: Operabilidad, Reconocibilidad (ISO 25010)
 */
export default function MisCotizacionesPage() {
  const [cotizaciones, setCotizaciones] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem('access_token')
    axios
      .get(`${API}/cotizaciones`, { headers: { Authorization: `Bearer ${token}` } })
      .then((res) => setCotizaciones(res.data))
      .catch(() => toast.error('Error al cargar cotizaciones'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p>Cargando cotizaciones...</p>

  return (
    <main style={{ maxWidth: 900, margin: '2rem auto', padding: '0 1rem' }}>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>Mis Cotizaciones</h1>
        <Link to="/cotizaciones/nueva">
          <button title="Crear una nueva cotización">+ Nueva cotización</button>
        </Link>
      </header>

      {cotizaciones.length === 0 ? (
        <p>No hay cotizaciones registradas.</p>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr>
              <th>Número</th><th>Cliente</th><th>Estado</th>
              <th>Total</th><th>Vigencia</th><th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {cotizaciones.map((c) => (
              <tr key={c.id}>
                <td>{c.numero}</td>
                <td>{c.clienteNombre}</td>
                <td>{c.estado}</td>
                <td>₡{c.total?.toFixed(2)}</td>
                <td>{c.fechaVigencia}</td>
                <td>
                  <Link to={`/cotizaciones/${c.id}`}>Ver detalle</Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </main>
  )
}
