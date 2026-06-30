package com.serviplus.apicontabilidad.domain;

/**
 * <<State Pattern>> — Defines allowed lifecycle transitions for a Cotizacion.
 *
 * BORRADOR  → ENVIADA  : coordinator (ADMIN/CONTADOR) submits the quote for internal review
 * ENVIADA   → ACEPTADA : coordinator approves; triggers client email + PDF via async event
 * ENVIADA   → RECHAZADA: coordinator rejects
 * BORRADOR  → ANULADA  : coordinator cancels the quote before sending it for review
 * ENVIADA   → ANULADA  : coordinator cancels a quote already under review
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
