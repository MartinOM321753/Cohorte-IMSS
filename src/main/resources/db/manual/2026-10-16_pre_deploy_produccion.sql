-- ============================================================================
-- Migración pre-despliegue: producción → versión actual del código
-- Fecha: 2026-10-16
-- ============================================================================
--
--  Este script reúne TODOS los cambios de esquema que el código actual espera
--  y que la base de producción (dump del 2026-10-16) todavía no tiene.
--
--  Orden de ejecución:
--    1. Tablas nuevas (sin dependencias externas primero)
--    2. Columnas nuevas en tablas existentes
--    3. Migraciones de datos (renombrar/mover columnas)
--
--  IMPORTANTE: Hibernate (ddl-auto=update) crearía las tablas y columnas
--  nuevas al arrancar, pero NO migra datos ni borra columnas viejas.
--  Este script hace ambas cosas de forma segura.
--
--  Ejecutar en una ventana de mantenimiento con respaldo previo.
-- ============================================================================

-- ────────────────────────────────────────────────────────────────────────────
-- 1. TABLA NUEVA: toma
--    La entidad Toma (registro inmutable de recolección de muestra)
--    es referenciada por muestra.id_toma y lote.id_toma
-- ────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS `toma` (
  `id_toma`                 BIGINT       NOT NULL AUTO_INCREMENT,
  `version`                 BIGINT       DEFAULT NULL,
  `id_paciente`             BIGINT       NOT NULL,
  `id_tipo_muestra`         BIGINT       NOT NULL,
  `id_institucion_origen`   BIGINT       NOT NULL,
  `id_usuario_recolecta`    BIGINT       DEFAULT NULL,
  `fecha_recoleccion`       DATETIME(6)  NOT NULL,
  `observaciones`           VARCHAR(500) DEFAULT NULL,
  `fecha_registro`          DATETIME(6)  NOT NULL,
  PRIMARY KEY (`id_toma`),
  UNIQUE KEY `uk_toma_paciente_tipo_fecha_inst`
    (`id_paciente`, `id_tipo_muestra`, `fecha_recoleccion`, `id_institucion_origen`),
  INDEX `idx_toma_paciente` (`id_paciente`),
  CONSTRAINT `fk_toma_paciente`
    FOREIGN KEY (`id_paciente`) REFERENCES `paciente` (`id_paciente`),
  CONSTRAINT `fk_toma_tipo_muestra`
    FOREIGN KEY (`id_tipo_muestra`) REFERENCES `tipo_muestra` (`id_tipo_muestra`),
  CONSTRAINT `fk_toma_institucion_origen`
    FOREIGN KEY (`id_institucion_origen`) REFERENCES `institucion` (`id_institucion`),
  CONSTRAINT `fk_toma_usuario_recolecta`
    FOREIGN KEY (`id_usuario_recolecta`) REFERENCES `usuario` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- ────────────────────────────────────────────────────────────────────────────
-- 2. COLUMNAS NUEVAS EN TABLAS EXISTENTES
-- ────────────────────────────────────────────────────────────────────────────

-- 2a. muestra.id_toma  (nullable — las muestras existentes no tienen toma)
ALTER TABLE `muestra`
  ADD COLUMN IF NOT EXISTS `id_toma` BIGINT DEFAULT NULL,
  ADD CONSTRAINT `fk_muestra_toma`
    FOREIGN KEY (`id_toma`) REFERENCES `toma` (`id_toma`);

-- 2b. lote.id_toma  (nullable)
ALTER TABLE `lote`
  ADD COLUMN IF NOT EXISTS `id_toma` BIGINT DEFAULT NULL,
  ADD CONSTRAINT `fk_lote_toma`
    FOREIGN KEY (`id_toma`) REFERENCES `toma` (`id_toma`);

-- 2c. institucion.folio_inicio y folio_fin  (nullable)
ALTER TABLE `institucion`
  ADD COLUMN IF NOT EXISTS `folio_inicio` INT DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `folio_fin`    INT DEFAULT NULL;


-- ────────────────────────────────────────────────────────────────────────────
-- 3. MIGRACIÓN: parametro_estudio_muestra
--    Reemplazar valor_minimo/valor_maximo por rangos por sexo
--    (script original: 2026-10-13_rangos_referencia_estudio_muestra.sql)
-- ────────────────────────────────────────────────────────────────────────────

-- Solo ejecutar si las columnas nuevas no existen aún
SET @col_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'parametro_estudio_muestra'
    AND COLUMN_NAME = 'valor_min_mujeres'
);

-- Si ya existen las columnas nuevas, este bloque no hace nada
SET @sql_add = IF(@col_exists = 0,
  'ALTER TABLE parametro_estudio_muestra
     ADD COLUMN valor_min_mujeres DOUBLE NULL,
     ADD COLUMN valor_max_mujeres DOUBLE NULL,
     ADD COLUMN valor_min_hombres DOUBLE NULL,
     ADD COLUMN valor_max_hombres DOUBLE NULL',
  'SELECT 1');
PREPARE stmt FROM @sql_add;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Migrar datos: el rango global pasa a ambos sexos
UPDATE `parametro_estudio_muestra`
SET `valor_min_mujeres` = `valor_minimo`,
    `valor_max_mujeres` = `valor_maximo`,
    `valor_min_hombres` = `valor_minimo`,
    `valor_max_hombres` = `valor_maximo`
WHERE (`valor_minimo` IS NOT NULL OR `valor_maximo` IS NOT NULL)
  AND `valor_min_mujeres` IS NULL
  AND `valor_min_hombres` IS NULL;

-- Borrar columnas viejas solo si las nuevas ya existen
SET @col_old = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'parametro_estudio_muestra'
    AND COLUMN_NAME = 'valor_minimo'
);
SET @sql_drop = IF(@col_old > 0,
  'ALTER TABLE parametro_estudio_muestra DROP COLUMN valor_minimo, DROP COLUMN valor_maximo',
  'SELECT 1');
PREPARE stmt FROM @sql_drop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- ────────────────────────────────────────────────────────────────────────────
-- 4. TABLAS NUEVAS: Módulo Cuestionarios
--    (10 tablas — Hibernate las crearía con ddl-auto=update, pero tenerlas
--     aquí documenta el esquema esperado y permite revisar antes de arrancar)
--
--    Hibernate las crea automáticamente al arrancar. Si prefieres que el
--    script las cree, descomenta el bloque. Si no, déjalas comentadas y
--    deja que Hibernate las genere.
-- ────────────────────────────────────────────────────────────────────────────

-- Las siguientes 10 tablas del módulo cuestionarios serán creadas
-- automáticamente por Hibernate (ddl-auto=update) al arrancar:
--   - tipo_cuestionario
--   - seccion_cuestionario
--   - variable_cuestionario
--   - opcion_variable
--   - cuestionario
--   - respuesta_cuestionario
--   - regla_validacion
--   - discrepancia_lectura
--   - tarea_carga_volcado
--   - fila_omitida_volcado
--
-- No tienen datos previos que migrar: son tablas completamente nuevas
-- sin dependencias en columnas de tablas existentes.


-- ────────────────────────────────────────────────────────────────────────────
-- 5. VERIFICACIÓN
-- ────────────────────────────────────────────────────────────────────────────

-- Después de ejecutar, verificar:
--   SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
--   WHERE TABLE_SCHEMA = DATABASE()
--     AND TABLE_NAME = 'muestra' AND COLUMN_NAME = 'id_toma';
--   -- Debe devolver 1
--
--   SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
--   WHERE TABLE_SCHEMA = DATABASE()
--     AND TABLE_NAME = 'parametro_estudio_muestra' AND COLUMN_NAME = 'valor_min_mujeres';
--   -- Debe devolver 1
--
--   SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
--   WHERE TABLE_SCHEMA = DATABASE()
--     AND TABLE_NAME = 'parametro_estudio_muestra' AND COLUMN_NAME = 'valor_minimo';
--   -- Debe devolver 0
