package com.serviplus.apicontabilidad.domain;

/**
 * <<State Pattern>> — Lifecycle transitions for a Factura.
 *
 * PENDIENTE → PAGADA  : saldo reaches zero after one or more abonos
 * PENDIENTE → ANULADA : admin voids the invoice before full payment
 * PAGADA, ANULADA → terminal; no further transitions allowed
 */
public enum EstadoFactura {

    PENDIENTE,
    PAGADA,
    ANULADA;

    public boolean puedeTransicionarA(EstadoFactura destino) {
        return switch (this) {
            case PENDIENTE -> destino == PAGADA || destino == ANULADA;
            case PAGADA, ANULADA -> false;
        };
    }
}
