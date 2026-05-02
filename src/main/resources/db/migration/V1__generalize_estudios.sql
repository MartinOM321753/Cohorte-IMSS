ALTER TABLE Resultado_Estudio
    ADD COLUMN IF NOT EXISTS valor_booleano BIT NULL,
    ADD COLUMN IF NOT EXISTS grupo_codigo VARCHAR(50) NOT NULL DEFAULT 'ROOT',
    ADD COLUMN IF NOT EXISTS grupo_etiqueta VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS orden_resultado INT NOT NULL DEFAULT 0;

UPDATE Resultado_Estudio
SET grupo_codigo = 'ROOT',
    orden_resultado = 0
WHERE grupo_codigo IS NULL
   OR orden_resultado IS NULL;

SET @resultado_index := (
    SELECT idx.index_name
    FROM (
        SELECT index_name,
               GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columnas
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'Resultado_Estudio'
          AND non_unique = 0
        GROUP BY index_name
    ) idx
    WHERE idx.columnas = 'id_estudio,id_parametro'
    LIMIT 1
);
SET @drop_resultado_index := IF(
    @resultado_index IS NOT NULL,
    CONCAT('ALTER TABLE Resultado_Estudio DROP INDEX ', @resultado_index),
    'SELECT 1'
);
PREPARE stmt FROM @drop_resultado_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE Resultado_Estudio
    ADD CONSTRAINT uk_estudio_parametro_grupo_orden
        UNIQUE (id_estudio, id_parametro, grupo_codigo, orden_resultado);

SET @parametro_index := (
    SELECT idx.index_name
    FROM (
        SELECT index_name,
               GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columnas
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'Parametro_Estudio'
          AND non_unique = 0
        GROUP BY index_name
    ) idx
    WHERE idx.columnas = 'nombre'
    LIMIT 1
);
SET @drop_parametro_index := IF(
    @parametro_index IS NOT NULL,
    CONCAT('ALTER TABLE Parametro_Estudio DROP INDEX ', @parametro_index),
    'SELECT 1'
);
PREPARE stmt FROM @drop_parametro_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE Parametro_Estudio
    ADD CONSTRAINT uk_parametro_tipo_nombre
        UNIQUE (id_tipo_estudio, nombre);

CREATE TABLE IF NOT EXISTS Estudio_Adjunto (
    id_adjunto BIGINT NOT NULL AUTO_INCREMENT,
    id_estudio BIGINT NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    nombre_original VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    ruta_url VARCHAR(500) NOT NULL,
    descripcion VARCHAR(255) NULL,
    orden_adjunto INT NOT NULL,
    PRIMARY KEY (id_adjunto),
    CONSTRAINT fk_adjunto_estudio
        FOREIGN KEY (id_estudio) REFERENCES Estudio_Medico (id_estudio)
            ON DELETE CASCADE,
    CONSTRAINT uk_estudio_adjunto_orden
        UNIQUE (id_estudio, orden_adjunto)
);
