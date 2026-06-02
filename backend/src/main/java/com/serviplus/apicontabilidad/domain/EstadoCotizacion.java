package com.serviplus.apicontabilidad.domain;

/**
 * <<State Pattern>> — Defines allowed lifecycle transitions for a Cotizacion.
 *
 * BORRADOR  → ENVIADA  : customer submits the quote for review
 * ENVIADA   → ACEPTADA : admin/contador approves; triggers email + PDF
 * ENVIADA   → RECHAZADA: admin/contador rejects
 * BORRADOR  → ANULADA  : customer cancels before submission
 * ENVIADA   → ANULADA  : admin cancels a pending quote
 * ACEPTADA, RECHAZADA, ANULADA → terminal; no further transitions allowed
 */
public enum EstadoCotizacion {

    BORRADOR,
    ENVIADA,
    ACEPTADA,
    RECHAZADA,
    ANULADA;

    public boolean puedeTransicionarA(EstadoCotizacion destino) {
        return switch (this) {
            case BORRADOR -> destino == ENVIADA  || destino == ANULADA;
            case ENVIADA  -> destino == ACEPTADA || destino == RECHAZADA || destino == ANULADA;
            case ACEPTADA, RECHAZADA, ANULADA -> false;
        };
    }
}
