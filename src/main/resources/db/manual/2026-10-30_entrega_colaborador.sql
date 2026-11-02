-- ============================================================================
--  Entrega/préstamo de muestras a colaboradores (personas físicas)
-- ============================================================================
--
--  Se ejecuta UNA VEZ, DESPUÉS de arrancar la versión nueva de la aplicación.
--
--  La mayor parte de este cambio NO necesita SQL manual:
--    • Las tablas nuevas `Colaborador` y `Entrega_Muestra` las crea Hibernate al
--      arrancar (`spring.jpa.hibernate.ddl-auto=update`).
--    • Los estados nuevos de muestra (ENTREGADA, CONSUMIDA) viven en
--      `Muestra.estado_muestra`, que es VARCHAR(20): caben sin tocar la columna.
--    • Los permisos nuevos (COLABORADORES_*, ENTREGAS_*) y su asignación a los
--      roles los siembra `PermisoInitializer` en el arranque, de forma
--      idempotente; no hace falta insertarlos aquí.
--
--  El único punto frágil es el ENUM `historial_cambio_muestra.tipo_evento`: si
--  el ALTER de Hibernate no llegara a aplicarse en producción (como ya pasó con
--  `institucion_modulo.modulo`), insertar un evento de entrega fallaría con
--  «Data truncated for column» y se perdería el historial en silencio. Este
--  script es esa red de seguridad.
--
--  Es re-ejecutable: comprueba antes de tocar.
--
--  ANTES DE EMPEZAR:  mysqldump de la base.
-- ============================================================================

START TRANSACTION;

-- ─────────────────────────────────────────────────────────────────────────────
--  Los cuatro eventos de entrega tienen que caber en el ENUM del historial
-- ─────────────────────────────────────────────────────────────────────────────
--  Se mantiene el tipo ENUM (dejarlo como VARCHAR haría que Hibernate intentara
--  revertirlo en cada arranque) y se incluye la lista COMPLETA de valores en
--  orden alfabético, que es como la genera Hibernate.

SET @enum_actual = (
  SELECT COLUMN_TYPE FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME   = 'historial_cambio_muestra'
     AND COLUMN_NAME  = 'tipo_evento'
);

SET @faltan = (
  @enum_actual LIKE 'enum%'
  AND (@enum_actual NOT LIKE '%ENTREGA_REALIZADA%'
    OR @enum_actual NOT LIKE '%ENTREGA_DEVUELTA%'
    OR @enum_actual NOT LIKE '%ENTREGA_CONSUMIDA%'
    OR @enum_actual NOT LIKE '%ENTREGA_CANCELADA%')
);

SET @sql = IF(@faltan,
  'ALTER TABLE `historial_cambio_muestra` MODIFY COLUMN `tipo_evento` ENUM(
     ''ACTUALIZACION_CAMPO'',''ALICUOTAS_COMPROMETIDAS'',''ALICUOTA_MATERIALIZADA'',
     ''ENTREGA_CANCELADA'',''ENTREGA_CONSUMIDA'',''ENTREGA_DEVUELTA'',''ENTREGA_REALIZADA'',
     ''ESTUDIO_REALIZADO'',''MUESTRA_AGOTADA'',''MUESTRA_DADA_BAJA'',
     ''POSICION_ASIGNADA'',''POSICION_LIBERADA'',''PRESTAMO_CANCELADO'',
     ''PRESTAMO_DEVUELTO'',''PRESTAMO_ENVIADO'',''PRESTAMO_RECIBIDO'',''REGISTRO''
   ) NOT NULL',
  'SELECT ''tipo_evento ya admite los eventos de entrega: no hace falta tocarla'' AS nota');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ─────────────────────────────────────────────────────────────────────────────
--  El destinatario de una entrega puede ser un colaborador O un usuario
-- ─────────────────────────────────────────────────────────────────────────────
--  Añadido 2026-10-31. `Entrega_Muestra` ahora tiene dos FKs de destinatario,
--  `id_colaborador` e `id_usuario_destino`, ambas nullable (exactamente una se
--  llena). Si la tabla se creó en un arranque anterior cuando `id_colaborador`
--  era NOT NULL, `ddl-auto=update` NO relaja la restricción, y entregar a un
--  usuario falla con «Column 'id_colaborador' cannot be null». Esto la afloja.
--  Idempotente: si la tabla no existe aún, o la columna ya es nullable, no hace
--  nada. (`id_usuario_destino` la añade Hibernate al arrancar; aquí solo se
--  afloja la vieja.)

SET @col_not_null = (
  SELECT IS_NULLABLE FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME   = 'entrega_muestra'
     AND COLUMN_NAME  = 'id_colaborador'
);

SET @sql = IF(@col_not_null = 'NO',
  'ALTER TABLE `entrega_muestra` MODIFY COLUMN `id_colaborador` BIGINT NULL',
  'SELECT ''id_colaborador ya admite null (o la tabla aún no existe)'' AS nota');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

COMMIT;

-- ============================================================================
--  COMPROBACIONES POSTERIORES — ejecutar y leer, no son automáticas
-- ============================================================================

-- 1  Las tablas nuevas existen (las crea Hibernate al arrancar).
SELECT TABLE_NAME
  FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME IN ('Colaborador', 'Entrega_Muestra');

-- 2  Los permisos nuevos quedaron sembrados (los inserta PermisoInitializer).
SELECT `codigo`, `modulo`
  FROM `permiso`
 WHERE `modulo` IN ('COLABORADORES', 'ENTREGAS')
 ORDER BY `modulo`, `codigo`;
