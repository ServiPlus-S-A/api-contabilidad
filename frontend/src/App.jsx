import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import RouteGuard from './components/RouteGuard.jsx'
import NuevaFacturaPage from './pages/NuevaFacturaPage.jsx'
import VistaPreviewFacturaPage from './pages/VistaPreviewFacturaPage.jsx'
import NuevaCotizacionPage from './pages/NuevaCotizacionPage.jsx'
import DetalleCotizacionPage from './pages/DetalleCotizacionPage.jsx'
import MisCotizacionesPage from './pages/MisCotizacionesPage.jsx'
import PanelCotizacionesPage from './pages/PanelCotizacionesPage.jsx'

/**
 * Root component — React Router configuration.
 * <<component>> React_Router (SAD diagram)
 *
 * All routes except / are wrapped in RouteGuard (JWT check).
 * Pattern: Proxy — RouteGuard acts as proxy for protected routes.
 */
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public */}
        <Route path="/" element={<Navigate to="/cotizaciones" replace />} />

        {/* Protected routes — RouteGuard validates JWT before rendering */}
        <Route element={<RouteGuard />}>
          <Route path="/facturas/nueva"          element={<NuevaFacturaPage />} />
          <Route path="/facturas/:id"             element={<VistaPreviewFacturaPage />} />
          <Route path="/cotizaciones"             element={<MisCotizacionesPage />} />
          <Route path="/cotizaciones/panel"       element={<PanelCotizacionesPage />} />
          <Route path="/cotizaciones/nueva"       element={<NuevaCotizacionPage />} />
          <Route path="/cotizaciones/:id"         element={<DetalleCotizacionPage />} />
        </Route>

        {/* Catch-all */}
        <Route path="*" element={<Navigate to="/cotizaciones" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
