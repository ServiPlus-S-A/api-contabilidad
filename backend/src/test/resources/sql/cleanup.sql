-- Cleanup script executed after each integration test to reset DB state.
-- Order matters: child tables first to avoid FK constraint violations.

DELETE FROM audit_log;
DELETE FROM abonos;
DELETE FROM lineas_factura;
DELETE FROM lineas_cotizacion;
DELETE FROM facturas;
DELETE FROM cotizaciones;
DELETE FROM contadores;
