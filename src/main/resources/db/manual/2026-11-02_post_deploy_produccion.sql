-- ============================================================================
--  Post-deploy de producción — cambios de almacén (temperatura / tipo de caja)
-- ============================================================================
--
--  Se ejecuta UNA VEZ, DESPUÉS de arrancar la versión nueva de la aplicación
--  (Hibernate, con ddl-auto=update, ya habrá creado las columnas
--  `refrigerador.temperatura`, `tipo_muestra.temperatura` y
--  `caja_criogenica.id_tipo_muestra_recomendado`).
--
--  Es re-ejecutable y NO borra nada: solo rellena columnas que están en NULL.
--
--  ANTES DE EMPEZAR:  mysqldump de la base.
-- ============================================================================

START TRANSACTION;

-- ─────────────────────────────────────────────────────────────────────────────
--  1. Caja criogénica: pasar el texto libre `tipo_caja` al tipo de muestra
-- ─────────────────────────────────────────────────────────────────────────────
--  La columna `tipo_caja` ya no la lee la aplicación (se queda en la tabla, con
--  sus datos, porque Hibernate nunca elimina columnas). Para no perder la
--  información en pantalla, se vincula cada caja con el tipo de muestra de su
--  misma institución cuyo nombre coincide con el texto (sin distinguir
--  mayúsculas). Las que no coincidan quedan en NULL = «Sin preferencia».

UPDATE `caja_criogenica` c
  JOIN `tipo_muestra` t
    ON t.`id_institucion` = c.`id_institucion`
   AND UPPER(TRIM(t.`nombre`)) = UPPER(TRIM(c.`tipo_caja`))
   SET c.`id_tipo_muestra_recomendado` = t.`id_tipo_muestra`
 WHERE c.`id_tipo_muestra_recomendado` IS NULL
   AND c.`tipo_caja` IS NOT NULL
   AND TRIM(c.`tipo_caja`) <> '';

COMMIT;

-- ============================================================================
--  COMPROBACIONES POSTERIORES — ejecutar y leer, no son automáticas
-- ============================================================================

-- A  Cajas que quedaron sin tipo recomendado aunque tenían texto (revisar a mano).
SELECT `id_caja`, `codigo_caja`, `tipo_caja`
  FROM `caja_criogenica`
 WHERE `id_tipo_muestra_recomendado` IS NULL
   AND `tipo_caja` IS NOT NULL AND TRIM(`tipo_caja`) <> '';

-- B  Refrigeradores sin temperatura. El formulario la exige al editar y, si un
--    tipo de muestra tiene temperatura y el refrigerador no, sus cajas no
--    aparecen al ubicar muestras de ese tipo. Capturarla desde la pantalla de
--    Refrigeradores (o con UPDATE `refrigerador` SET `temperatura` = <°C>).
SELECT `id_refrigerador`, `nombre`, `temperatura`
  FROM `refrigerador`
 WHERE `temperatura` IS NULL;

-- C  Instituciones raíz: debe haber UNA. Si aparece
--    «IMSS Cuernavaca - Sede Central» además de la raíz real, la creó
--    InitialConfig porque no encontró una institución con ese nombre (ver
--    informe). Está sin usuarios ni datos y se puede eliminar a mano.
SELECT i.`id_institucion`, i.`nombre`, i.`fecha_registro`,
       (SELECT COUNT(*) FROM `usuario` u WHERE u.`id_institucion` = i.`id_institucion`) AS usuarios
  FROM `institucion` i
 WHERE i.`id_institucion_padre` IS NULL;
