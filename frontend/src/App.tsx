import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import RouteGuard from './components/RouteGuard.tsx'
import NuevaFacturaPage from './pages/NuevaFacturaPage.tsx'
import VistaPreviewFacturaPage from './pages/VistaPreviewFacturaPage.tsx'
import NuevaCotizacionPage from './pages/NuevaCotizacionPage.tsx'
import DetalleCotizacionPage from './pages/DetalleCotizacionPage.tsx'
import MisCotizacionesPage from './pages/MisCotizacionesPage.tsx'
import PanelCotizacionesPage from './pages/PanelCotizacionesPage.tsx'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/cotizaciones" replace />} />

        <Route element={<RouteGuard />}>
          <Route path="/facturas/nueva"    element={<NuevaFacturaPage />} />
          <Route path="/facturas/:id"      element={<VistaPreviewFacturaPage />} />
          <Route path="/cotizaciones"      element={<MisCotizacionesPage />} />
          <Route path="/cotizaciones/panel" element={<PanelCotizacionesPage />} />
          <Route path="/cotizaciones/nueva" element={<NuevaCotizacionPage />} />
          <Route path="/cotizaciones/:id"  element={<DetalleCotizacionPage />} />
        </Route>

        <Route path="*" element={<Navigate to="/cotizaciones" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
