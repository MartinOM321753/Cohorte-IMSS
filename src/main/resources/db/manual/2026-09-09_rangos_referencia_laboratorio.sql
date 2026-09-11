-- ============================================================================
--  Rangos de referencia de laboratorio: alinearlos con el protocolo del estudio
-- ============================================================================
--
--  Se ejecuta UNA VEZ. No hace falta parar la aplicación: sólo cambia datos.
--
--  POR QUÉ
--  -------
--  Los rangos guardados en `examen` no son los del formato oficial del reporte de
--  salud HWC. De las 26 combinaciones analito × sexo que el reporte imprime, 22 no
--  coinciden. Mientras no se corrijan, cualquier reporte emitido lleva semáforos
--  calculados con umbrales que no son los del estudio.
--
--  Los dos errores que más importan, porque marcan de más:
--
--    · HDL tenía TECHO (55 en hombres, 65 en mujeres). El HDL alto es protector y
--      por eso el protocolo sólo pone piso. Con el techo, un HDL de 70 —excelente—
--      se imprimía como «fuera de rango» en un documento que se entrega en mano.
--
--    · Colesterol total y triglicéridos tenían PISO (140 y 80) que el protocolo no
--      pone: el protocolo dice «<200» y «<150» y nada más. Un valor bajo salía
--      marcado sin serlo.
--
--  Y el que marca de menos y ya se puede demostrar: ácido úrico estaba en 3.4–7.0
--  para ambos sexos. El protocolo da 2.4–6.0 en mujeres. Una participante con 6.4
--  salía «en rango» cuando el protocolo la pone por arriba.
--
--  NULL significa «sin límite por ese lado», y así lo entiende RangoReferencia:
--  comprueba `min != null` antes de comparar. Poner 0 en lugar de NULL no es lo
--  mismo — con 0 el límite existe y se evalúa.
--
--  ES RE-EJECUTABLE: los UPDATE son idempotentes y el WHERE va por nombre exacto.
--
--  ANTES DE EMPEZAR:  mysqldump de la base.
--
--  Rangos tomados del formato oficial «Reporte de salud HWC», sección LABORATORIOS.
-- ============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
--  0. Cómo están ahora (para poder comparar después)
-- ─────────────────────────────────────────────────────────────────────────────
SELECT id_examen, nombre_examen, unidad,
       valor_min_mujeres AS minM, valor_max_mujeres AS maxM,
       valor_min_hombres AS minH, valor_max_hombres AS maxH
  FROM examen
 ORDER BY id_examen;

START TRANSACTION;

-- ─────────────────────────────────────────────────────────────────────────────
--  1. Los trece analitos que imprime el reporte
-- ─────────────────────────────────────────────────────────────────────────────

--  Glucosa en ayuno · 70–99 mg/dL en ambos sexos
UPDATE examen SET valor_min_mujeres = 70,   valor_max_mujeres = 99,
                  valor_min_hombres = 70,   valor_max_hombres = 99
 WHERE nombre_examen = 'Glucosa';

--  Creatinina · el protocolo sí la diferencia por sexo
UPDATE examen SET valor_min_mujeres = 0.59, valor_max_mujeres = 1.04,
                  valor_min_hombres = 0.74, valor_max_hombres = 1.35
 WHERE nombre_examen = 'Creatinina';

--  Ácido úrico · estaba sin diferenciar; en mujeres el rango es más bajo
UPDATE examen SET valor_min_mujeres = 2.4,  valor_max_mujeres = 6.0,
                  valor_min_hombres = 3.4,  valor_max_hombres = 7.0
 WHERE nombre_examen = 'Acido urico';

--  Colesterol total · «<200». Se quita el piso de 140, que no está en el protocolo.
UPDATE examen SET valor_min_mujeres = NULL, valor_max_mujeres = 200,
                  valor_min_hombres = NULL, valor_max_hombres = 200
 WHERE nombre_examen = 'Colesterol total';

--  Triglicéridos · «<150». Se quita el piso de 80.
UPDATE examen SET valor_min_mujeres = NULL, valor_max_mujeres = 150,
                  valor_min_hombres = NULL, valor_max_hombres = 150
 WHERE nombre_examen = 'Triglicéridos';

--  Albúmina · 3.5–5.0 g/dL. El 8.5 de hombres era casi seguro un dedazo por 5.5,
--  y ni 5.5 ni 8.5 son el valor del protocolo.
UPDATE examen SET valor_min_mujeres = 3.5,  valor_max_mujeres = 5.0,
                  valor_min_hombres = 3.5,  valor_max_hombres = 5.0
 WHERE nombre_examen = 'Albúmina';

--  AST / TGO
UPDATE examen SET valor_min_mujeres = 10,   valor_max_mujeres = 35,
                  valor_min_hombres = 10,   valor_max_hombres = 40
 WHERE nombre_examen = 'AST-TGO';

--  ALT / TGP · el rango 5–59 para ambos era mucho más ancho que el del protocolo
UPDATE examen SET valor_min_mujeres = 7,    valor_max_mujeres = 35,
                  valor_min_hombres = 10,   valor_max_hombres = 49
 WHERE nombre_examen = 'ALT-TGP';

--  Velocidad de sedimentación
UPDATE examen SET valor_min_mujeres = 0,    valor_max_mujeres = 20,
                  valor_min_hombres = 0,    valor_max_hombres = 15
 WHERE nombre_examen = 'VSG';

--  Hematocrito
UPDATE examen SET valor_min_mujeres = 36,   valor_max_mujeres = 46,
                  valor_min_hombres = 41,   valor_max_hombres = 53
 WHERE nombre_examen = 'Hematocrito';

--  HDL · SÓLO PISO. Es la corrección más importante del guion: el techo hacía que
--  un HDL alto, que es lo deseable, saliera marcado como hallazgo.
UPDATE examen SET valor_min_mujeres = 50,   valor_max_mujeres = NULL,
                  valor_min_hombres = 40,   valor_max_hombres = NULL
 WHERE nombre_examen = 'HDL';

--  LDL · «<100». El piso en 0 no es un límite real: ningún LDL es negativo, pero
--  mientras exista se evalúa, y un día una captura en 0 saldría «en rango» por la
--  puerta equivocada.
UPDATE examen SET valor_min_mujeres = NULL, valor_max_mujeres = 100,
                  valor_min_hombres = NULL, valor_max_hombres = 100
 WHERE nombre_examen = 'LDL';

--  Plaquetas · ya estaban bien; se dejan escritas para que el guion documente las 13
UPDATE examen SET valor_min_mujeres = 150,  valor_max_mujeres = 450,
                  valor_min_hombres = 150,  valor_max_hombres = 450
 WHERE nombre_examen = 'Plaquetas';

-- ─────────────────────────────────────────────────────────────────────────────
--  2. PCR y HbA1c no van en el reporte de salud
-- ─────────────────────────────────────────────────────────────────────────────
--  Se dejan como están a propósito. Están dados de alta y se capturan, pero el
--  formato oficial no los imprime, así que no hay rango del protocolo contra el
--  que compararlos y cambiarlos sería inventar.

COMMIT;

-- ─────────────────────────────────────────────────────────────────────────────
--  3. Comprobación
-- ─────────────────────────────────────────────────────────────────────────────
--  Debe devolver 13 filas y ninguna con la columna `coincide` en 'NO'.
SELECT nombre_examen,
       CONCAT(IFNULL(valor_min_mujeres,'—'), ' – ', IFNULL(valor_max_mujeres,'—')) AS mujeres,
       CONCAT(IFNULL(valor_min_hombres,'—'), ' – ', IFNULL(valor_max_hombres,'—')) AS hombres,
       CASE WHEN (nombre_examen, valor_min_mujeres, valor_max_mujeres,
                  valor_min_hombres, valor_max_hombres) IN (
              ROW('Glucosa',           70,   99,   70,   99),
              ROW('Creatinina',        0.59, 1.04, 0.74, 1.35),
              ROW('Acido urico',       2.4,  6.0,  3.4,  7.0),
              ROW('Albúmina',          3.5,  5.0,  3.5,  5.0),
              ROW('AST-TGO',           10,   35,   10,   40),
              ROW('ALT-TGP',           7,    35,   10,   49),
              ROW('VSG',               0,    20,   0,    15),
              ROW('Hematocrito',       36,   46,   41,   53),
              ROW('Plaquetas',         150,  450,  150,  450))
            THEN 'si'
            WHEN nombre_examen = 'Colesterol total'
                 AND valor_min_mujeres IS NULL AND valor_max_mujeres = 200
                 AND valor_min_hombres IS NULL AND valor_max_hombres = 200 THEN 'si'
            WHEN nombre_examen = 'Triglicéridos'
                 AND valor_min_mujeres IS NULL AND valor_max_mujeres = 150
                 AND valor_min_hombres IS NULL AND valor_max_hombres = 150 THEN 'si'
            WHEN nombre_examen = 'LDL'
                 AND valor_min_mujeres IS NULL AND valor_max_mujeres = 100
                 AND valor_min_hombres IS NULL AND valor_max_hombres = 100 THEN 'si'
            WHEN nombre_examen = 'HDL'
                 AND valor_min_mujeres = 50 AND valor_max_mujeres IS NULL
                 AND valor_min_hombres = 40 AND valor_max_hombres IS NULL THEN 'si'
            ELSE 'NO'
       END AS coincide
  FROM examen
 WHERE nombre_examen IN ('Glucosa','Creatinina','Acido urico','Colesterol total',
                         'Triglicéridos','Albúmina','AST-TGO','ALT-TGP','VSG',
                         'Hematocrito','HDL','LDL','Plaquetas')
 ORDER BY nombre_examen;
