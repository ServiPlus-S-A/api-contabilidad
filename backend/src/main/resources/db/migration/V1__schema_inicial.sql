-- V1__schema_inicial.sql
-- Flyway migration: complete initial schema for api-contabilidad
-- Engine: InnoDB (ACID), Charset: utf8mb4

-- ==============================================================================
-- Secuencias de numeración (ADR: Pessimistic-locked counter to avoid gaps)
-- ==============================================================================
CREATE TABLE contadores (
    id      BIGINT       NOT NULL AUTO_INCREMENT,
    tipo    VARCHAR(20)  NOT NULL,
    anio    INT          NOT NULL,
    siguiente BIGINT     NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT uq_contador_tipo_anio UNIQUE (tipo, anio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- Cotizaciones (Quotes)
-- ==============================================================================
CREATE TABLE cotizaciones (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    numero          VARCHAR(20)     NOT NULL,
    cliente_id      BIGINT          NOT NULL,
    cliente_nombre  VARCHAR(255)    NOT NULL,
    fecha_vigencia  DATE            NOT NULL,
    notas           VARCHAR(1000),
    estado          VARCHAR(20)     NOT NULL,
    subtotal        DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    impuesto        DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    total           DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    creado_en       DATETIME        NOT NULL,
    actualizado_en  DATETIME,
    creado_por      VARCHAR(100)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_cotizacion_numero UNIQUE (numero)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_cotizaciones_cliente_id ON cotizaciones(cliente_id);
CREATE INDEX idx_cotizaciones_estado     ON cotizaciones(estado);
CREATE INDEX idx_cotizaciones_creado_por ON cotizaciones(creado_por);

-- ==============================================================================
-- Líneas de cotización
-- ==============================================================================
CREATE TABLE lineas_cotizacion (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    cotizacion_id   BIGINT          NOT NULL,
    descripcion     VARCHAR(500)    NOT NULL,
    cantidad        DECIMAL(15,4)   NOT NULL,
    precio_unitario DECIMAL(15,2)   NOT NULL,
    subtotal        DECIMAL(15,2)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_lc_cotizacion FOREIGN KEY (cotizacion_id)
        REFERENCES cotizaciones(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_lineas_cotizacion_id ON lineas_cotizacion(cotizacion_id);

-- ==============================================================================
-- Facturas (Invoices)
-- ==============================================================================
CREATE TABLE facturas (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    numero              VARCHAR(20)     NOT NULL,
    cliente_id          BIGINT          NOT NULL,
    cliente_nombre      VARCHAR(255)    NOT NULL,
    fecha_vencimiento   DATE            NOT NULL,
    notas               VARCHAR(1000),
    estado              VARCHAR(20)     NOT NULL,
    subtotal            DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    impuesto            DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    total               DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    saldo               DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    pdf_url             VARCHAR(500),
    creado_en           DATETIME        NOT NULL,
    actualizado_en      DATETIME,
    creado_por          VARCHAR(100)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_factura_numero UNIQUE (numero)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_facturas_cliente_id ON facturas(cliente_id);
CREATE INDEX idx_facturas_estado     ON facturas(estado);
CREATE INDEX idx_facturas_creado_por ON facturas(creado_por);

-- ==============================================================================
-- Líneas de factura
-- ==============================================================================
CREATE TABLE lineas_factura (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    factura_id      BIGINT          NOT NULL,
    descripcion     VARCHAR(500)    NOT NULL,
    cantidad        DECIMAL(15,4)   NOT NULL,
    precio_unitario DECIMAL(15,2)   NOT NULL,
    subtotal        DECIMAL(15,2)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_lf_factura FOREIGN KEY (factura_id)
        REFERENCES facturas(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_lineas_factura_id ON lineas_factura(factura_id);

-- ==============================================================================
-- Abonos (Partial payments applied to a factura)
-- ==============================================================================
CREATE TABLE abonos (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    factura_id  BIGINT          NOT NULL,
    monto       DECIMAL(15,2)   NOT NULL,
    fecha       DATETIME        NOT NULL,
    referencia  VARCHAR(255),
    creado_por  VARCHAR(100)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_abono_factura FOREIGN KEY (factura_id)
        REFERENCES facturas(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_abonos_factura_id ON abonos(factura_id);

-- ==============================================================================
-- Audit Log (ADR-S3: non-repudiation)
-- ==============================================================================
CREATE TABLE audit_log (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    entidad     VARCHAR(50)     NOT NULL,
    entidad_id  BIGINT          NOT NULL,
    accion      VARCHAR(50)     NOT NULL,
    usuario     VARCHAR(100)    NOT NULL,
    detalle     VARCHAR(2000),
    timestamp   DATETIME        NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_log_entidad   ON audit_log(entidad, entidad_id);
CREATE INDEX idx_audit_log_usuario   ON audit_log(usuario);
CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp);
