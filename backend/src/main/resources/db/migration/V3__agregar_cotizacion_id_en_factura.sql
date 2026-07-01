ALTER TABLE facturas
    ADD COLUMN cotizacion_id BIGINT NULL,
    ADD CONSTRAINT fk_factura_cotizacion
        FOREIGN KEY (cotizacion_id) REFERENCES cotizaciones(id),
    ADD CONSTRAINT uq_factura_cotizacion
        UNIQUE (cotizacion_id);
