-- MySQL 8. Revisar y aplicar una sola vez con respaldo, antes de desplegar.
-- Los campos nuevos son anulables para conservar solicitudes históricas.
ALTER TABLE solicitud_acceso_datos
  ADD COLUMN telefono VARCHAR(10) NULL,
  ADD COLUMN correo_verificado_en DATETIME(6) NULL,
  ADD COLUMN ubicacion_latitud DOUBLE NULL,
  ADD COLUMN ubicacion_longitud DOUBLE NULL,
  ADD COLUMN ubicacion_precision DOUBLE NULL,
  ADD COLUMN ubicacion_capturada_en DATETIME(6) NULL,
  ADD COLUMN ubicacion_consentimiento_en DATETIME(6) NULL,
  ADD COLUMN conexion_pais VARCHAR(2) NULL,
  ADD COLUMN conexion_proveedor VARCHAR(80) NULL,
  ADD COLUMN conexion_revisada_en DATETIME(6) NULL;

CREATE TABLE solicitud_correo_verificacion (
  id VARCHAR(43) NOT NULL PRIMARY KEY,
  email VARCHAR(180) NOT NULL,
  ip VARCHAR(45) NOT NULL,
  code_hash VARCHAR(64) NOT NULL,
  failed_attempts INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  verified_at DATETIME(6) NULL,
  used_at DATETIME(6) NULL,
  version BIGINT NULL,
  INDEX idx_solicitud_verificacion_ip_fecha (ip, created_at),
  INDEX idx_solicitud_verificacion_correo_fecha (email, created_at),
  INDEX idx_solicitud_verificacion_expira (expires_at)
);
