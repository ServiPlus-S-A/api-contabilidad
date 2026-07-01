import { useForm, useFieldArray } from 'react-hook-form';
import { toast } from 'react-toastify';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import type { CotizacionFormValues, CotizacionResponse } from '../types.ts';

const API = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

export default function NuevaCotizacionPage() {
  const navigate = useNavigate();
  const {
    register,
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CotizacionFormValues>({
    defaultValues: {
      clienteId: undefined,
      clienteNombre: '',
      fechaVigencia: '',
      notas: '',
      lineas: [{ descripcion: '', cantidad: 1, precioUnitario: 0 }],
    },
  });

  const { fields, append, remove } = useFieldArray({ control, name: 'lineas' });

  const onSubmit = async (data: CotizacionFormValues) => {
    const token = localStorage.getItem('access_token');
    const res = await axios.post<CotizacionResponse>(`${API}/cotizaciones`, data, {
      headers: { Authorization: `Bearer ${token ?? ''}` },
    });
    toast.success(`Cotización ${res.data.numero} creada correctamente`);
    navigate(`/cotizaciones/${res.data.id}`);
  };

  const handleError = () => {
    toast.error('Error al crear la cotización');
  };

  return (
    <main style={{ maxWidth: 800, margin: '2rem auto', padding: '0 1rem' }}>
      <h1>Nueva Cotización</h1>
      {/* eslint-disable-next-line @typescript-eslint/no-misused-promises */}
      <form onSubmit={handleSubmit(onSubmit, handleError)} noValidate>
        <label>
          ID Cliente{' '}
          <input
            type="number"
            {...register('clienteId', { required: 'Requerido', min: 1, valueAsNumber: true })}
          />
          {errors.clienteId && <span role="alert">{errors.clienteId.message}</span>}
        </label>

        <label>
          Nombre del cliente{' '}
          <input type="text" {...register('clienteNombre', { required: 'Requerido' })} />
        </label>

        <label>
          Fecha de vigencia{' '}
          <input
            type="date"
            title="Hasta cuándo es válida esta cotización"
            {...register('fechaVigencia', { required: 'Requerido' })}
          />
        </label>

        <h2>Líneas</h2>
        {fields.map((field, index) => (
          <div key={field.id} style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem' }}>
            <input
              placeholder="Descripción"
              {...register(`lineas.${index}.descripcion`, { required: true })}
            />
            <input
              type="number"
              step="0.01"
              placeholder="Cantidad"
              {...register(`lineas.${index}.cantidad`, { min: 0.01, valueAsNumber: true })}
            />
            <input
              type="number"
              step="0.01"
              placeholder="Precio unitario"
              {...register(`lineas.${index}.precioUnitario`, { min: 0.01, valueAsNumber: true })}
            />
            {fields.length > 1 && (
              <button
                type="button"
                onClick={() => {
                  remove(index);
                }}
              >
                ✕
              </button>
            )}
          </div>
        ))}
        <button
          type="button"
          onClick={() => {
            append({ descripcion: '', cantidad: 1, precioUnitario: 0 });
          }}
        >
          + Agregar línea
        </button>

        <label>
          Notas <textarea {...register('notas')} rows={3} />
        </label>

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Guardando...' : 'Crear Cotización'}
        </button>
      </form>
    </main>
  );
}
