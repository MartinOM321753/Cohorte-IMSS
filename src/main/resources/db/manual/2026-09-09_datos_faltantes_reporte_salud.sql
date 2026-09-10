-- ============================================================================
--  Las dos mediciones del reporte de salud que no existen como parámetro
-- ============================================================================
--
--  Se ejecuta UNA VEZ, después de 2026-09-09_rangos_referencia_laboratorio.sql.
--  No hace falta parar la aplicación.
--
--  POR QUÉ
--  -------
--  El formato oficial del reporte imprime 23 mediciones. 21 ya son capturables.
--  Estas dos no:
--
--    1. La DISTANCIA recorrida en la caminata de 6 minutos. La prueba está dada de
--       alta —el tipo de estudio 4 guarda las presiones, las frecuencias cardiacas
--       y las escalas de Borg en cada momento— pero no el metraje, que es el
--       resultado principal de la prueba.
--
--    2. Los PERCENTILES de dinamometría. El formato dice «P5–P95 según edad y
--       sexo (ver tabla de percentiles)». Esa tabla no está en el sistema, así que
--       hoy la fuerza de prensión se compara contra un rango plano de 0 a 70/90 kg
--       que no distingue a una persona de 30 años de una de 70.
--
--  LO QUE ESTE GUION HACE Y LO QUE NO
--  ----------------------------------
--  Da de alta la distancia, que es un parámetro más y no tiene discusión.
--
--  NO inventa la tabla de percentiles. Hace falta decidir de qué fuente salen y
--  con qué cortes de edad, y eso es del protocolo del estudio, no de este guion.
--  Lo que sí deja es el hueco preparado: la tabla creada y vacía, para que
--  cargarla después sea un INSERT y no un cambio de esquema.
--
--  ES RE-EJECUTABLE.
--
--  ANTES DE EMPEZAR:  mysqldump de la base.
-- ============================================================================

START TRANSACTION;

-- ─────────────────────────────────────────────────────────────────────────────
--  1. Distancia recorrida en la caminata de 6 minutos
-- ─────────────────────────────────────────────────────────────────────────────
--  Va en el tipo de estudio 4, junto al resto de la prueba.
--
--  Sin rango fijo: el propio formato lo define como una ECUACIÓN distinta por
--  sexo —(2.11 × estatura) − (5.78 × edad) − (2.29 × peso) + 667 en mujeres, y
--  (7.57 × estatura) − (5.02 × edad) − (1.76 × peso) − 309 en hombres—, así que
--  el valor esperado se calcula con el motor de fórmulas y no cabe en dos
--  columnas. Dejar min y max en NULL es correcto: significa «sin rango plano»,
--  y RangoReferencia no marca nada cuando no hay con qué comparar.

INSERT INTO parametro_estudio (nombre, tipo, unidad, id_tipo_estudio, activo,
                               valor_min_mujeres, valor_max_mujeres,
                               valor_min_hombres, valor_max_hombres)
SELECT 'Distancia recorrida en 6 minutos', 'NUMERICO', 'm', 4, TRUE,
       NULL, NULL, NULL, NULL
 WHERE NOT EXISTS (
       SELECT 1 FROM (SELECT * FROM parametro_estudio) AS p
        WHERE p.id_tipo_estudio = 4
          AND p.nombre = 'Distancia recorrida en 6 minutos');

-- ─────────────────────────────────────────────────────────────────────────────
--  2. El hueco para los percentiles de dinamometría
-- ─────────────────────────────────────────────────────────────────────────────
--  Una fila por sexo y tramo de edad. Se guardan los dos extremos que el formato
--  nombra, P5 y P95, y de paso la mediana, que es lo que se suele querer enseñar
--  al participante junto a su resultado.
--
--  `edad_hasta` es INCLUSIVO. Con tramos abiertos por un lado —«65 y más»— se
--  pone un tope alto en lugar de NULL: un NULL obligaría a que cada consulta se
--  acuerde de tratarlo, y la que se olvide dejará sin percentil justo a los más
--  mayores, que son los que más lo necesitan.

CREATE TABLE IF NOT EXISTS percentil_dinamometria (
    id_percentil  BIGINT       NOT NULL AUTO_INCREMENT,
    sexo          ENUM('M','F') NOT NULL,
    edad_desde    INT          NOT NULL,
    edad_hasta    INT          NOT NULL,
    p5            DOUBLE       NOT NULL,
    p50           DOUBLE           NULL,
    p95           DOUBLE       NOT NULL,
    fuente        VARCHAR(200)     NULL COMMENT 'De dónde salen estos valores',
    PRIMARY KEY (id_percentil),
    UNIQUE KEY uk_percentil_sexo_edad (sexo, edad_desde, edad_hasta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='P5–P95 de fuerza de prensión por sexo y edad. Se llena desde el protocolo.';

COMMIT;

-- ─────────────────────────────────────────────────────────────────────────────
--  3. Comprobación
-- ─────────────────────────────────────────────────────────────────────────────
--  La primera debe devolver 1. La segunda, 0 filas: la tabla existe y está vacía
--  a la espera de los valores del protocolo.

SELECT COUNT(*) AS distancia_dada_de_alta
  FROM parametro_estudio
 WHERE id_tipo_estudio = 4
   AND nombre = 'Distancia recorrida en 6 minutos';

SELECT COUNT(*) AS percentiles_cargados FROM percentil_dinamometria;
