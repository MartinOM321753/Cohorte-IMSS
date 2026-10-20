-- Rangos de referencia por sexo en parámetros de estudios de muestra.
-- Reemplaza valor_minimo/valor_maximo (rango global) por 4 columnas por sexo,
-- igual que Parametro_Estudio y Examen.

ALTER TABLE Parametro_Estudio_Muestra
    ADD COLUMN valor_min_mujeres DOUBLE NULL,
    ADD COLUMN valor_max_mujeres DOUBLE NULL,
    ADD COLUMN valor_min_hombres DOUBLE NULL,
    ADD COLUMN valor_max_hombres DOUBLE NULL;

-- Migrar datos existentes: el rango global pasa a ambos sexos.
UPDATE Parametro_Estudio_Muestra
SET valor_min_mujeres = valor_minimo,
    valor_max_mujeres = valor_maximo,
    valor_min_hombres = valor_minimo,
    valor_max_hombres = valor_maximo
WHERE valor_minimo IS NOT NULL OR valor_maximo IS NOT NULL;

ALTER TABLE Parametro_Estudio_Muestra
    DROP COLUMN valor_minimo,
    DROP COLUMN valor_maximo;
