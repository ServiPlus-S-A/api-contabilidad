package com.serviplus.apicontabilidad.async.event;

import com.serviplus.apicontabilidad.domain.Factura;
import org.springframework.context.ApplicationEvent;

public class FacturaCreadaEvent extends ApplicationEvent {

    private final transient Factura factura;

    public FacturaCreadaEvent(Object source, Factura factura) {
        super(source);
        this.factura = factura;
    }

    public Factura getFactura() {
        return factura;
    }
}
