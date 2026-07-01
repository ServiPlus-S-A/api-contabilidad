package com.serviplus.apicontabilidad.async.event;

import com.serviplus.apicontabilidad.domain.Cotizacion;
import org.springframework.context.ApplicationEvent;

public class CotizacionAprobadaEvent extends ApplicationEvent {

    private final transient Cotizacion cotizacion;

    public CotizacionAprobadaEvent(Object source, Cotizacion cotizacion) {
        super(source);
        this.cotizacion = cotizacion;
    }

    public Cotizacion getCotizacion() {
        return cotizacion;
    }
}
