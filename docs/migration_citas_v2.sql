-- Migración del módulo de Citas
-- MySQL script

ALTER TABLE cita 
ADD COLUMN uuid VARCHAR(36),
ADD COLUMN start_at_utc DATETIME(3),
ADD COLUMN end_at_utc DATETIME(3),
ADD COLUMN timezone VARCHAR(50),
ADD COLUMN color_hex VARCHAR(7),
ADD COLUMN created_at_utc DATETIME(3),
ADD COLUMN updated_at_utc DATETIME(3),
ADD COLUMN created_by VARCHAR(50),
ADD COLUMN updated_by VARCHAR(50),
ADD COLUMN version BIGINT DEFAULT 0;

-- Índices solicitados
CREATE INDEX idx_usuario_start ON cita (id_usuario_agenda, start_at_utc);
CREATE INDEX idx_start_utc ON cita (start_at_utc);
CREATE UNIQUE INDEX idx_cita_uuid ON cita (uuid);

-- Backfill: Migración de datos existentes
-- Se asume 'America/Mexico_City' como timezone default si no existe
UPDATE cita 
SET 
    uuid = (SELECT UUID()),
    start_at_utc = CONVERT_TZ(fecha_cita, 'America/Mexico_City', 'UTC'),
    end_at_utc = DATE_ADD(CONVERT_TZ(fecha_cita, 'America/Mexico_City', 'UTC'), INTERVAL duracion_minutos MINUTE),
    timezone = 'America/Mexico_City',
    created_at_utc = COALESCE(fecha_registro, NOW()),
    updated_at_utc = COALESCE(fecha_actualizacion, NOW());

-- Una vez migrados los datos, se pueden aplicar las restricciones NOT NULL a las nuevas columnas
ALTER TABLE cita 
MODIFY COLUMN uuid VARCHAR(36) NOT NULL,
MODIFY COLUMN start_at_utc DATETIME(3) NOT NULL,
MODIFY COLUMN end_at_utc DATETIME(3) NOT NULL,
MODIFY COLUMN timezone VARCHAR(50) NOT NULL,
MODIFY COLUMN created_at_utc DATETIME(3) NOT NULL,
MODIFY COLUMN updated_at_utc DATETIME(3) NOT NULL;

-- Una vez migrados los datos, se pueden eliminar las columnas antiguas o hacerlas nullable
-- para que no interfieran con nuevos registros
ALTER TABLE cita 
MODIFY COLUMN fecha_cita DATETIME(3) NULL,
MODIFY COLUMN fecha_registro DATETIME(3) NULL,
MODIFY COLUMN fecha_actualizacion DATETIME(3) NULL;

-- ALTER TABLE cita DROP COLUMN fecha_cita, DROP COLUMN fecha_registro, DROP COLUMN fecha_actualizacion;
