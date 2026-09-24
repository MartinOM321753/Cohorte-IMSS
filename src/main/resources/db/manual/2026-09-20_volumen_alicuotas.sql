-- ============================================================================
--  Contabilidad de volumen entre muestras padre y alícuotas
-- ============================================================================
--
--  Se ejecuta UNA VEZ, DESPUÉS de arrancar la versión nueva de la aplicación.
--
--  Por qué después y no antes: las columnas nuevas las crea Hibernate al
--  arrancar (`spring.jpa.hibernate.ddl-auto=update`), y este script necesita
--  que ya existan para poder rellenarlas. El orden es:
--
--      respaldo  →  desplegar  →  arrancar  →  ESTE SCRIPT  →  habilitar
--
--  Es re-ejecutable: comprueba antes de tocar, así que correrlo dos veces no
--  rompe nada ni da error.
--
--  ANTES DE EMPEZAR:  mysqldump de la base.
-- ============================================================================

START TRANSACTION;

-- ─────────────────────────────────────────────────────────────────────────────
--  1. Los tipos de evento nuevos tienen que caber en la columna
-- ─────────────────────────────────────────────────────────────────────────────
--  `historial_cambio_muestra.tipo_evento` es un ENUM de MySQL, no un VARCHAR:
--  Hibernate mapea así los @Enumerated(STRING) contra MySQL. Si no contiene los
--  tres valores nuevos, insertarlos falla con «Data truncated for column» y el
--  historial de la contabilidad se pierde en silencio, que es la peor forma de
--  perderlo.
--
--  Verificado en desarrollo: al arrancar, `ddl-auto=update` emite por su cuenta
--  el ALTER que añade los valores. Este bloque es la red de seguridad para el
--  caso de que en producción ese ALTER no llegue a aplicarse —que es justo lo
--  que ocurrió con `institucion_modulo.modulo` y obligó a la migración del
--  módulo de reportes—.
--
--  Se mantiene el tipo ENUM y el orden alfabético que genera Hibernate: dejarlo
--  como VARCHAR haría que en cada arranque volviera a intentar convertirlo.

SET @enum_actual = (
  SELECT COLUMN_TYPE FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME   = 'historial_cambio_muestra'
     AND COLUMN_NAME  = 'tipo_evento'
);

SET @faltan = (
  @enum_actual LIKE 'enum%'
  AND (@enum_actual NOT LIKE '%ALICUOTAS_COMPROMETIDAS%'
    OR @enum_actual NOT LIKE '%ALICUOTA_MATERIALIZADA%'
    OR @enum_actual NOT LIKE '%MUESTRA_AGOTADA%')
);

SET @sql = IF(@faltan,
  'ALTER TABLE `historial_cambio_muestra` MODIFY COLUMN `tipo_evento` ENUM(
     ''ACTUALIZACION_CAMPO'',''ALICUOTAS_COMPROMETIDAS'',''ALICUOTA_MATERIALIZADA'',
     ''ESTUDIO_REALIZADO'',''MUESTRA_AGOTADA'',''MUESTRA_DADA_BAJA'',
     ''POSICION_ASIGNADA'',''POSICION_LIBERADA'',''PRESTAMO_CANCELADO'',
     ''PRESTAMO_DEVUELTO'',''PRESTAMO_ENVIADO'',''PRESTAMO_RECIBIDO'',''REGISTRO''
   ) NOT NULL',
  'SELECT ''tipo_evento ya admite los eventos nuevos: no hace falta tocarla'' AS nota');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ─────────────────────────────────────────────────────────────────────────────
--  2. Las muestras padre arrancan con cero volumen comprometido
-- ─────────────────────────────────────────────────────────────────────────────

UPDATE `Muestra`
   SET `valor_comprometido` = 0
 WHERE `valor_comprometido` IS NULL;

-- ─────────────────────────────────────────────────────────────────────────────
--  3. Las alícuotas que ya existían se dan por materializadas, SIN descontar
-- ─────────────────────────────────────────────────────────────────────────────
--  Decisión deliberada, y conviene comunicarla a los usuarios:
--
--  Hasta ahora el sistema creaba siempre todas las alícuotas del tubo con su
--  volumen nominal y nunca descontaba nada de la padre. Eso significa que de un
--  tubo de 5 × 50 mL sobre una extracción de 200 mL hay registrados 250 mL en
--  alícuotas MÁS los 200 mL de la padre: 450 mL a partir de una extracción de
--  200.
--
--  Descontarlas ahora retroactivamente dejaría padres en negativo y reescribiría
--  una historia física que hoy ya nadie puede verificar. Así que se marcan como
--  materializadas con descuento CERO: no vuelven a tocar a su padre nunca, las
--  padres conservan el valor que tienen hoy, y la contabilidad correcta empieza
--  limpia desde el despliegue.
--
--  `fecha_registro` como fecha de materialización es lo más cercano a la verdad
--  que hay en la base: es cuando la alícuota se creó.

UPDATE `Muestra`
   SET `fecha_materializacion`     = COALESCE(`fecha_registro`, NOW()),
       `cantidad_descontada_padre` = 0
 WHERE `id_muestra_padre` IS NOT NULL
   AND `fecha_materializacion` IS NULL;

-- ─────────────────────────────────────────────────────────────────────────────
--  4. Los tubos ya configurados conservan el comportamiento de siempre
-- ─────────────────────────────────────────────────────────────────────────────
--  El código lee estas dos columnas con `null` = activado, justamente para que
--  nada cambie aunque este script no llegue a correr. Esto solo deja el dato
--  explícito para que se vea en el configurador.
--
--  Ojo con el motivo de que sean nullable: MySQL rellena con 0 una columna
--  booleana NOT NULL recién añadida sobre filas que ya existen. Declararlas
--  obligatorias habría dejado TODOS los tubos configurados en manual, de golpe
--  y sin un solo error visible.

UPDATE `Tubo_Muestra`
   SET `generacion_automatica` = 1
 WHERE `generacion_automatica` IS NULL;

UPDATE `Tubo_Muestra`
   SET `permite_alicuota_parcial` = 1
 WHERE `permite_alicuota_parcial` IS NULL;

COMMIT;

-- ============================================================================
--  COMPROBACIONES POSTERIORES — ejecutar y leer, no son automáticas
-- ============================================================================

-- 4.1  Tubos que alicuotan pero no dicen de cuánto ni en qué unidad.
--      A partir de ahora la unidad del tubo se le impone a la muestra padre, así
--      que un tubo sin unidad no tiene nada que imponer y no se podrá usar para
--      registrar hasta que el configurador se la ponga. Si esta consulta
--      devuelve filas, hay que avisar a quien administre los tipos de muestra
--      ANTES de habilitar el módulo.

SELECT t.`id_tubo_muestra`,
       t.`nombre`            AS tubo,
       tm.`nombre`           AS tipo_muestra,
       i.`nombre`            AS institucion,
       t.`numero_alicuotas`,
       t.`volumen_alicuota`,
       t.`unidad_volumen`
  FROM `Tubo_Muestra` t
  JOIN `Tipo_Muestra` tm ON tm.`id_tipo_muestra` = t.`id_tipo_muestra`
  JOIN `institucion`   i ON i.`id_institucion`   = tm.`id_institucion`
 WHERE t.`numero_alicuotas` > 0
   AND (t.`unidad_volumen` IS NULL OR t.`unidad_volumen` = ''
        OR t.`volumen_alicuota` IS NULL OR t.`volumen_alicuota` <= 0)
 ORDER BY i.`nombre`, tm.`nombre`, t.`orden`;

-- 4.2  Muestras cuya unidad no coincide con la del tubo con el que se
--      registraron. Son anteriores a la imposición de unidad y se respetan tal
--      cual: NO se convierten ni se reescriben. Solo sirve para saber cuántas
--      son y advertir que al alicuotarlas se pedirá alinear el tubo.

SELECT m.`id_muestra`, m.`etiqueta`, m.`unidad` AS unidad_muestra,
       t.`nombre` AS tubo, t.`unidad_volumen` AS unidad_tubo
  FROM `Muestra` m
  JOIN `Tubo_Muestra` t ON t.`id_tubo_muestra` = m.`id_tubo_muestra`
 WHERE m.`id_muestra_padre` IS NULL
   AND t.`unidad_volumen` IS NOT NULL AND t.`unidad_volumen` <> ''
   AND m.`unidad` IS NOT NULL AND m.`unidad` <> ''
   AND UPPER(TRIM(m.`unidad`)) <> UPPER(TRIM(t.`unidad_volumen`))
 ORDER BY m.`id_muestra`;

-- 4.3  Red de seguridad del invariante: ninguna padre debería tener un
--      `valor_comprometido` distinto de la suma de sus alícuotas sin ubicar.
--      Justo después de este script el resultado tiene que ser vacío, porque
--      todo quedó en cero. Vale la pena volver a correrla de vez en cuando:
--      si algún día devuelve filas, la contabilidad se desvió y esta misma
--      consulta dice en cuánto.

SELECT p.`id_muestra`,
       p.`etiqueta`,
       p.`valor_comprometido`                       AS comprometido_guardado,
       COALESCE(SUM(a.`valor`), 0)                  AS comprometido_real,
       p.`valor_comprometido` - COALESCE(SUM(a.`valor`), 0) AS desviacion
  FROM `Muestra` p
  LEFT JOIN `Muestra` a
         ON a.`id_muestra_padre` = p.`id_muestra`
        AND a.`fecha_materializacion` IS NULL
        AND a.`estado_muestra` <> 'BAJA'
 WHERE p.`id_muestra_padre` IS NULL
 GROUP BY p.`id_muestra`, p.`etiqueta`, p.`valor_comprometido`
HAVING ABS(p.`valor_comprometido` - COALESCE(SUM(a.`valor`), 0)) > 0.0001;
