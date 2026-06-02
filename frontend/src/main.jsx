import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ToastContainer } from 'react-toastify'
import 'react-toastify/dist/ReactToastify.css'
import App from './App.jsx'

/**
 * React 18 entry point.
 * <<component>> Feature: ReactToasting — ToastContainer renders global toast
 * notifications used across all accounting pages.
 */
createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
    {/* Pattern: Decorator — ToastContainer wraps the app to provide
        notification capability without modifying individual components */}
    <ToastContainer
      position="top-right"
      autoClose={4000}
      hideProgressBar={false}
      closeOnClick
      pauseOnFocusLoss
      draggable
      pauseOnHover
    />
  </StrictMode>,
)
