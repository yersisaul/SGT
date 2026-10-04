-- Objetos que Hibernate (ddl-auto=update) no crea por sí solo.
-- Se ejecuta en cada arranque después de Hibernate
-- (spring.jpa.defer-datasource-initialization=true): todo debe ser idempotente.

-- Secuencias de numeración legible ST-n / RQ-n / OT-n (NumeracionServiceImpl).
CREATE SEQUENCE IF NOT EXISTS seq_solicitud;
CREATE SEQUENCE IF NOT EXISTS seq_requerimiento;
CREATE SEQUENCE IF NOT EXISTS seq_orden;

-- Si ya existen registros numerados (BD con datos previos a las secuencias),
-- se adelanta cada secuencia al máximo número usado para no repetirlo.
SELECT setval('seq_solicitud', m) FROM (SELECT MAX(CAST(SUBSTRING(numero_solicitud FROM 4) AS BIGINT)) AS m FROM solicitud WHERE numero_solicitud ~ '^ST-[0-9]+$') t WHERE m IS NOT NULL AND m >= (SELECT last_value FROM seq_solicitud);
SELECT setval('seq_requerimiento', m) FROM (SELECT MAX(CAST(SUBSTRING(numero_requerimiento FROM 4) AS BIGINT)) AS m FROM requerimiento WHERE numero_requerimiento ~ '^RQ-[0-9]+$') t WHERE m IS NOT NULL AND m >= (SELECT last_value FROM seq_requerimiento);
SELECT setval('seq_orden', m) FROM (SELECT MAX(CAST(SUBSTRING(numero_orden FROM 4) AS BIGINT)) AS m FROM orden WHERE numero_orden ~ '^OT-[0-9]+$') t WHERE m IS NOT NULL AND m >= (SELECT last_value FROM seq_orden);

-- La OT puede estar en cola sin ejecutor (PRD D13). ddl-auto=update no quita
-- un NOT NULL existente; DROP NOT NULL es idempotente.
ALTER TABLE orden ALTER COLUMN id_usuario DROP NOT NULL;

-- Cola por especialidad: OT sin ejecutor y sin cerrar (predicado estable, sin funciones volátiles).
CREATE INDEX IF NOT EXISTS idx_orden_cola ON orden (id_especialidad) WHERE id_usuario IS NULL AND fecha_cierre IS NULL;
CREATE INDEX IF NOT EXISTS idx_asignacion_orden_orden_fecha ON asignacion_orden (id_orden, fecha);
CREATE INDEX IF NOT EXISTS idx_asignacion_orden_tipo_fecha ON asignacion_orden (tipo, fecha);
