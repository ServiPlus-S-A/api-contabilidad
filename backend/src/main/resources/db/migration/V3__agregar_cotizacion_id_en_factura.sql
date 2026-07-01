-- V2__agregar_cotizacion_id_en_factura.sql
-- HU-05: vincula una factura a la cotización ACEPTADA de origen
ALTER TABLE facturas
    ADD COLUMN cotizacion_id BIGINT NULL,
    ADD CONSTRAINT fk_factura_cotizacion
        FOREIGN KEY (cotizacion_id) REFERENCES cotizaciones(id);

CREATE INDEX idx_facturas_cotizacion_id ON facturas(cotizacion_id);
