-- ============================================================================
--  El dato del reporte de salud que no existe: los percentiles de dinamometría
-- ============================================================================
--
--  Se ejecuta UNA VEZ. No hace falta parar la aplicación.
--
--  POR QUÉ
--  -------
--  El formato oficial del reporte compara la fuerza de prensión contra «P5–P95
--  según edad y sexo (ver tabla de percentiles)». Esa tabla no está en el sistema,
--  así que hoy la fuerza se compara contra un rango plano (0–70 kg en mujeres,
--  0–90 en hombres) que no distingue a una persona de 30 años de una de 70.
--
--  La distancia de la caminata de 6 minutos NO falta: existe como «Distancia
--  recorrida» en la prueba de caminata (id_parametro 14, tipo de estudio 3, en
--  metros), tanto en producción como en local. Una versión anterior de este guion
--  la daba de alta otra vez en el tipo 4; se quitó porque habría creado un
--  parámetro duplicado en un estudio que no es el suyo.
--
--  LO QUE ESTE GUION HACE Y LO QUE NO
--  ----------------------------------
--  NO inventa los percentiles. De qué fuente salen y con qué cortes de edad es del
--  protocolo del estudio. Deja la tabla creada y vacía para que cargarlos después
--  sea un INSERT y no un cambio de esquema.
--
--  ES RE-EJECUTABLE.
--
--  ANTES DE EMPEZAR:  mysqldump de la base.
-- ============================================================================

--  Una fila por sexo y tramo de edad. Se guardan los dos extremos que el formato
--  nombra, P5 y P95, y de paso la mediana, que es lo que se suele querer enseñar
--  al participante junto a su resultado.
--
--  `edad_hasta` es INCLUSIVO. Con tramos abiertos por un lado —«65 y más»— se
--  pone un tope alto en lugar de NULL: un NULL obligaría a que cada consulta se
--  acuerde de tratarlo, y la que se olvide dejará sin percentil justo a los más
--  mayores, que son los que más lo necesitan.

CREATE TABLE IF NOT EXISTS percentil_dinamometria (
    id_percentil  BIGINT        NOT NULL AUTO_INCREMENT,
    sexo          ENUM('M','F') NOT NULL,
    edad_desde    INT           NOT NULL,
    edad_hasta    INT           NOT NULL,
    p5            DOUBLE        NOT NULL,
    p50           DOUBLE            NULL,
    p95           DOUBLE        NOT NULL,
    fuente        VARCHAR(200)      NULL COMMENT 'De dónde salen estos valores',
    PRIMARY KEY (id_percentil),
    UNIQUE KEY uk_percentil_sexo_edad (sexo, edad_desde, edad_hasta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='P5–P95 de fuerza de prensión por sexo y edad. Se llena desde el protocolo.';

-- ─────────────────────────────────────────────────────────────────────────────
--  Comprobación: debe devolver 0 — la tabla existe y está vacía a la espera de
--  los valores del protocolo.
-- ─────────────────────────────────────────────────────────────────────────────
SELECT COUNT(*) AS percentiles_cargados FROM percentil_dinamometria;
