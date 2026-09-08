-- ============================================================================
--  Preparación de la base de PRODUCCIÓN antes de desplegar el módulo de reportes
-- ============================================================================
--
--  Se ejecuta UNA VEZ, ANTES de arrancar la versión nueva de la aplicación.
--
--  Por qué hace falta: el sistema arranca con `spring.jpa.hibernate.ddl-auto=update`,
--  que sabe AGREGAR tablas y columnas nuevas pero NO sabe modificar una columna que
--  ya existe. Las dos cosas de aquí abajo son justo eso: cambios sobre columnas
--  existentes que Hibernate va a pasar por alto en silencio.
--
--  Es re-ejecutable: comprueba antes de tocar, así que correrlo dos veces no rompe
--  nada ni da error.
--
--  ANTES DE EMPEZAR:  mysqldump de la base. Hay 7 826 participantes detrás.
-- ============================================================================

START TRANSACTION;

-- ─────────────────────────────────────────────────────────────────────────────
--  1. El módulo REPORTES no cabe en la columna
-- ─────────────────────────────────────────────────────────────────────────────
--  `institucion_modulo.modulo` es un ENUM con diez valores y REPORTES no es uno de
--  ellos. El código ya tiene once. Sin este cambio, activar el módulo de reportes
--  para una institución falla con «Data truncated for column 'modulo'», y como todo
--  el módulo está detrás de @RequireModulo(REPORTES), queda inaccesible.
--
--  REPORTES se añade AL FINAL de la lista a propósito. MySQL guarda internamente la
--  posición de cada valor: insertarlo en orden alfabético renumeraría los diez que ya
--  están y obligaría a reescribir la tabla. Al final es una operación instantánea y
--  las filas existentes no se tocan. El orden no importa para nada más, porque la
--  aplicación guarda y lee el texto, no la posición.

ALTER TABLE `institucion_modulo`
  MODIFY COLUMN `modulo` ENUM(
    'BIOBANCO','BITACORA_ACCESOS','BITACORA_ACCIONES','CITAS','COBERTURA',
    'DOCUMENTOS','ESTUDIOS_MEDICOS','EXAMENES','PARTICIPANTES','SOMATOMETRIA',
    'REPORTES'
  ) NOT NULL;

-- ─────────────────────────────────────────────────────────────────────────────
--  2. `parametro_estudio.activo` sobre 211 parámetros que ya existen
-- ─────────────────────────────────────────────────────────────────────────────
--  La columna no está en producción y la entidad la declara NOT NULL. Hibernate la
--  agregaría solo, y con el `columnDefinition` que trae debería dejar los 211
--  parámetros en TRUE. Se hace aquí igualmente para no depender de eso: si por
--  cualquier motivo la columna se creara sin el valor por omisión, los 211 parámetros
--  quedarían en 0 —retirados de uso— y desaparecerían de los formularios de captura
--  sin que nada avisara. Ese error se nota tarde y cuesta rastrearlo.
--
--  Se comprueba primero porque MySQL 8 no admite ADD COLUMN IF NOT EXISTS.

SET @existe := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME   = 'parametro_estudio'
    AND COLUMN_NAME  = 'activo'
);

SET @sql := IF(@existe = 0,
  'ALTER TABLE `parametro_estudio` ADD COLUMN `activo` BOOLEAN NOT NULL DEFAULT TRUE',
  'DO 0');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

COMMIT;

-- ============================================================================
--  Comprobación — las tres consultas deben salir como se indica
-- ============================================================================

--  (a) El ENUM ya admite REPORTES  →  debe devolver 1
SELECT COUNT(*) AS enum_admite_reportes
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'institucion_modulo'
  AND COLUMN_NAME  = 'modulo'
  AND COLUMN_TYPE LIKE '%REPORTES%';

--  (b) Ningún parámetro quedó retirado por accidente  →  debe devolver 0
SELECT COUNT(*) AS parametros_desactivados_por_error
FROM `parametro_estudio`
WHERE `activo` = 0;

--  (c) Los diez módulos que ya estaban siguen ahí  →  debe devolver 10
SELECT COUNT(*) AS modulos_conservados FROM `institucion_modulo`;
