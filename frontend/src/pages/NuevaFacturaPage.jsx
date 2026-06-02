import { useForm, useFieldArray } from 'react-hook-form'
import { toast } from 'react-toastify'
import axios from 'axios'
import { useNavigate } from 'react-router-dom'

const API = import.meta.env.VITE_API_BASE_URL || '/api/v1'

/**
 * <<component>> NuevaFacturaPage
 *
 * Form to create a new Factura (Invoice).
 * Pattern: Observer — React Hook Form observes field changes in real time.
 * Validation: React Hook Form (HTML5 + custom rules) — Protección frente a
 *             errores de usuario (ISO 25010).
 */
export default function NuevaFacturaPage() {
  const navigate = useNavigate()
  const {
    register,
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    defaultValues: {
      clienteId: '',
      clienteNombre: '',
      fechaVencimiento: '',
      notas: '',
      lineas: [{ descripcion: '', cantidad: 1, precioUnitario: 0 }],
    },
  })

  const { fields, append, remove } = useFieldArray({ control, name: 'lineas' })

  const onSubmit = async (data) => {
    try {
      const token = localStorage.getItem('access_token')
      const response = await axios.post(`${API}/facturas`, data, {
        headers: { Authorization: `Bearer ${token}` },
      })
      toast.success(`Factura ${response.data.numero} creada correctamente`)
      navigate(`/facturas/${response.data.id}`)
    } catch (err) {
      const msg = err.response?.data?.message || 'Error al crear la factura'
      toast.error(msg)
    }
  }

  return (
    <main style={{ maxWidth: 800, margin: '2rem auto', padding: '0 1rem' }}>
      <h1>Nueva Factura</h1>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>

        {/* ── Client info ── */}
        <section>
          <h2>Datos del cliente</h2>
          <label>
            ID Cliente
            <input
              type="number"
              {...register('clienteId', { required: 'Requerido', min: 1 })}
              title="Identificador del cliente en el sistema"
            />
            {errors.clienteId && <span role="alert">{errors.clienteId.message}</span>}
          </label>
          <label>
            Nombre del cliente
            <input
              type="text"
              {...register('clienteNombre', { required: 'Requerido', maxLength: 255 })}
            />
            {errors.clienteNombre && <span role="alert">{errors.clienteNombre.message}</span>}
          </label>
          <label>
            Fecha de vencimiento
            <input
              type="date"
              {...register('fechaVencimiento', { required: 'Requerido' })}
            />
            {errors.fechaVencimiento && <span role="alert">{errors.fechaVencimiento.message}</span>}
          </label>
        </section>

        {/* ── Line items ── */}
        <section>
          <h2>Líneas de detalle</h2>
          {fields.map((field, index) => (
            <div key={field.id} style={{ display: 'flex', gap: '1rem', marginBottom: '0.5rem' }}>
              <input
                placeholder="Descripción"
                {...register(`lineas.${index}.descripcion`, { required: true })}
              />
              <input
                type="number"
                step="0.01"
                placeholder="Cantidad"
                title="Cantidad de unidades"
                {...register(`lineas.${index}.cantidad`, { required: true, min: 0.01 })}
              />
              <input
                type="number"
                step="0.01"
                placeholder="Precio unitario"
                title="Precio por unidad en moneda local"
                {...register(`lineas.${index}.precioUnitario`, { required: true, min: 0.01 })}
              />
              {fields.length > 1 && (
                <button type="button" onClick={() => remove(index)}>✕</button>
              )}
            </div>
          ))}
          <button
            type="button"
            onClick={() => append({ descripcion: '', cantidad: 1, precioUnitario: 0 })}
          >
            + Agregar línea
          </button>
        </section>

        <label>
          Notas
          <textarea {...register('notas', { maxLength: 1000 })} rows={3} />
        </label>

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Guardando...' : 'Crear Factura'}
        </button>
      </form>
    </main>
  )
}
