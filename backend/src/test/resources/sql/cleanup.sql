-- Cleanup script executed after each integration test to reset DB state.
-- Order matters: child tables first to avoid FK constraint violations.

DELETE FROM audit_log; -- NOSONAR intentional full-table delete for test cleanup
DELETE FROM abonos; -- NOSONAR
DELETE FROM lineas_factura; -- NOSONAR
DELETE FROM lineas_cotizacion; -- NOSONAR
DELETE FROM facturas; -- NOSONAR
DELETE FROM cotizaciones; -- NOSONAR
DELETE FROM contadores; -- NOSONAR
